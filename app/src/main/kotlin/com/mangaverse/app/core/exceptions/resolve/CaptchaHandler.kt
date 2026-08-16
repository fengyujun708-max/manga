package com.mangaverse.app.core.exceptions.resolve

import android.Manifest
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.CheckResult
import androidx.annotation.RequiresPermission
import androidx.collection.MutableScatterMap
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import androidx.lifecycle.coroutineScope
import coil3.EventListener
import coil3.Extras
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.allowConversionToBitmap
import coil3.request.allowHardware
import coil3.request.lifecycle
import coil3.size.Scale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.mangaverse.app.R
import com.mangaverse.app.core.LocalizedAppContext
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.exceptions.CloudFlareException
import com.mangaverse.app.core.exceptions.CloudFlareProtectedException
import com.mangaverse.app.core.model.ContentSource
import com.mangaverse.app.core.model.UnknownContentSource
import com.mangaverse.app.core.model.getTitle
import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.parser.favicon.faviconUri
import com.mangaverse.app.core.prefs.SourceSettings
import com.mangaverse.app.core.util.ext.checkNotificationPermission
import com.mangaverse.app.core.util.ext.getNotificationIconSize
import com.mangaverse.app.core.util.ext.goAsync
import com.mangaverse.app.core.util.ext.mangaSourceExtra
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.core.util.ext.processLifecycleScope
import com.mangaverse.app.core.util.ext.toBitmapOrNull
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.network.CloudFlareHelper
import com.mangaverse.app.parsers.util.mapToArray
import com.mangaverse.app.parsers.util.runCatchingCancellable
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class CaptchaHandler @Inject constructor(
	@LocalizedAppContext private val context: Context,
	private val databaseProvider: Provider<MangaDatabase>,
	private val coilProvider: Provider<ImageLoader>,
	private val captchaAutoResolveCoordinator: CaptchaAutoResolveCoordinator,
) : EventListener() {

	private val exceptionMap = MutableScatterMap<ContentSource, CloudFlareProtectedException>()
	private val mutex = Mutex()

	@CheckResult
	suspend fun handle(exception: CloudFlareException, tryAutoResolve: Boolean = true): Boolean =
		handleException(
			source = exception.source,
			exception = exception,
			notify = true,
			tryAutoResolve = tryAutoResolve,
		)

	suspend fun discard(source: ContentSource) {
		handleException(source, null, notify = true, tryAutoResolve = false)
	}

	override fun onError(request: ImageRequest, result: ErrorResult) {
		super.onError(request, result)
		val e = result.throwable
		if (e is CloudFlareException) {
			val scope = request.lifecycle?.coroutineScope ?: processLifecycleScope
			scope.launch {
				if (
					handleException(
						source = e.source,
						exception = e,
						notify = request.extras[suppressCaptchaKey] != true,
						tryAutoResolve = false,
					)
				) {
					coilProvider.get().enqueue(request) // TODO check if ok
				}
			}
		}
	}

	private suspend fun handleException(
		source: ContentSource,
		exception: CloudFlareException?,
		notify: Boolean,
		tryAutoResolve: Boolean = true,
	): Boolean = withContext(Dispatchers.Default) {
		if (source == UnknownContentSource) {
			return@withContext false
		}
		if (
			tryAutoResolve &&
			exception is CloudFlareProtectedException &&
			!SourceSettings(context, source).isCaptchaAutoResolveDisabled &&
			captchaAutoResolveCoordinator.resolveInBackground(source, exception)
		) {
			return@withContext true
		}
		mutex.withLock {
			var removedException: CloudFlareProtectedException? = null
			if (exception is CloudFlareProtectedException) {
				exceptionMap[source] = exception
			} else {
				removedException = exceptionMap.remove(source)
			}
			val dao = databaseProvider.get().getSourcesDao()
			dao.setCfState(source.name, exception?.state ?: CloudFlareHelper.PROTECTION_NOT_DETECTED)

			if (notify && context.checkNotificationPermission(CHANNEL_ID)) {
				val exceptions = dao.findAllCaptchaRequired().mapNotNull {
					it.source.toContentSourceOrNull()
				}.filterNot {
					SourceSettings(context, it).isCaptchaNotificationsDisabled
				}.mapNotNull {
					exceptionMap[it]
				}
				if (removedException != null) {
					NotificationManagerCompat.from(context).cancel(TAG, removedException.source.hashCode())
				}
				notify(exceptions)
			}
		}
		false
	}

	@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
	private suspend fun notify(exceptions: List<CloudFlareProtectedException>) {
		val manager = NotificationManagerCompat.from(context)
		val channel = NotificationChannelCompat.Builder(
			CHANNEL_ID,
			NotificationManagerCompat.IMPORTANCE_LOW,
		)
			.setName(context.getString(R.string.captcha_required))
			.setShowBadge(true)
			.setVibrationEnabled(false)
			.setSound(null, null)
			.setLightsEnabled(false)
			.build()
		manager.createNotificationChannel(channel)

		coroutineScope {
			exceptions.map {
				async { it to buildNotification(it) }
			}.awaitAll()
		}.forEach { (exception, notification) ->
			manager.notify(TAG, exception.source.hashCode(), notification)
		}
		if (exceptions.size > 1) {
			val groupNotification = NotificationCompat.Builder(context, CHANNEL_ID)
				.setGroupSummary(true)
				.setContentTitle(context.getString(R.string.captcha_required))
				.setPriority(NotificationCompat.PRIORITY_LOW)
				.setDefaults(0)
				.setOnlyAlertOnce(true)
				.setSmallIcon(R.drawable.ic_bot)
				.setGroup(GROUP_CAPTCHA)
				.setContentIntent(
					PendingIntentCompat.getActivities(
						context, GROUP_NOTIFICATION_ID,
						exceptions.mapToArray { e ->
							AppRouter.cloudFlareResolveIntent(context, e)
						},
						0, false,
					),
				)
				.setContentText(
					context.getString(
						R.string.captcha_required_summary, context.getString(R.string.app_name),
					),
				)
				.setVisibility(
					if (exceptions.any { it.source.isNsfw() }) {
						NotificationCompat.VISIBILITY_SECRET
					} else {
						NotificationCompat.VISIBILITY_PUBLIC
					},
				)
			manager.notify(TAG, GROUP_NOTIFICATION_ID, groupNotification.build())
		} else {
			manager.cancel(TAG, GROUP_NOTIFICATION_ID)
		}
	}

	private suspend fun buildNotification(exception: CloudFlareProtectedException): Notification {
		val intent = AppRouter.cloudFlareResolveIntent(context, exception)
		val discardIntent = Intent(ACTION_DISCARD)
			.putExtra(AppRouter.KEY_SOURCE, exception.source.name)
			.setData("source://${exception.source.name}".toUri())
		val notification = NotificationCompat.Builder(context, CHANNEL_ID)
			.setContentTitle(context.getString(R.string.captcha_required))
			.setPriority(NotificationCompat.PRIORITY_LOW)
			.setDefaults(0)
			.setSmallIcon(R.drawable.ic_bot)
			.setGroup(GROUP_CAPTCHA)
			.setOnlyAlertOnce(true)
			.setAutoCancel(true)
			.setDeleteIntent(PendingIntentCompat.getBroadcast(context, 0, discardIntent, 0, false))
			.setLargeIcon(getFavicon(exception.source))
			.setVisibility(
				if (exception.source.isNsfw()) {
					NotificationCompat.VISIBILITY_SECRET
				} else {
					NotificationCompat.VISIBILITY_PUBLIC
				},
			)
			.setContentText(
				context.getString(
					R.string.captcha_required_summary,
					exception.source.getTitle(context),
				),
			)
			.setContentIntent(PendingIntentCompat.getActivity(context, 0, intent, 0, false))
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val actionIntent = PendingIntentCompat.getActivity(
				context, SETTINGS_ACTION_CODE,
				Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
					.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
					.putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID),
				0, false,
			)
			notification.addAction(
				R.drawable.ic_settings,
				context.getString(R.string.notifications_settings),
				actionIntent,
			)
		}
		return notification.build()
	}

	private fun String.toContentSourceOrNull() = ContentSource(this).takeUnless { it == UnknownContentSource }

	private suspend fun getFavicon(source: ContentSource) = runCatchingCancellable {
		coilProvider.get().execute(
			ImageRequest.Builder(context)
				.data(source.faviconUri())
				.allowHardware(false)
				.allowConversionToBitmap(true)
				.suppressCaptchaErrors()
				.mangaSourceExtra(source)
				.size(context.resources.getNotificationIconSize())
				.scale(Scale.FILL)
				.build(),
		).toBitmapOrNull()
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrNull()

	@AndroidEntryPoint
	class DiscardReceiver : BroadcastReceiver() {

		@Inject
		lateinit var captchaHandler: CaptchaHandler

		override fun onReceive(context: Context?, intent: Intent?) {
			val sourceName = intent?.getStringExtra(AppRouter.KEY_SOURCE) ?: return
			goAsync {
				captchaHandler.handleException(ContentSource(sourceName), exception = null, notify = false)
			}
		}
	}

	companion object {

		fun ImageRequest.Builder.suppressCaptchaErrors() = apply {
			extras[suppressCaptchaKey] = true
		}

		private val suppressCaptchaKey = Extras.Key(false)

		private const val CHANNEL_ID = "captcha"
		private const val TAG = CHANNEL_ID
		private const val GROUP_CAPTCHA = "com.mangaverse.app.CAPTCHA"
		private const val GROUP_NOTIFICATION_ID = 34
		private const val SETTINGS_ACTION_CODE = 3
		private const val ACTION_DISCARD = "com.mangaverse.app.CAPTCHA_DISCARD"
	}
}
