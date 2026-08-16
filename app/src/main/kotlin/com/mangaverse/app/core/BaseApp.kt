package com.mangaverse.app.core

import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.InvalidationTracker
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import dagger.hilt.EntryPoint
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttp
import org.acra.ACRA
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.config.httpSender
import org.acra.data.StringFormat
import kotlinx.coroutines.withContext
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import org.conscrypt.Conscrypt
import com.mangaverse.app.BuildConfig
import com.mangaverse.app.R
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.os.AppValidator
import com.mangaverse.app.core.os.RomCompat
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.util.ext.processLifecycleScope
import com.mangaverse.app.local.data.LocalStorageChanges
import com.mangaverse.app.local.data.index.LocalContentIndex
import com.mangaverse.app.local.domain.model.LocalContent
import com.mangaverse.app.mihon.MihonExtensionManager
import com.mangaverse.app.parsers.util.suspendlazy.getOrNull
import com.mangaverse.app.settings.work.WorkScheduleManager
import java.security.Security
import javax.inject.Provider

open class BaseApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {

	private val entryPoint: BaseAppEntryPoint by lazy(LazyThreadSafetyMode.NONE) {
		EntryPointAccessors.fromApplication(this, BaseAppEntryPoint::class.java)
	}

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(entryPoint.workerFactory())
			.build()

	override fun newImageLoader(context: Context): ImageLoader {
		return entryPoint.imageLoader()
	}

	override fun onCreate() {
		super.onCreate()
		try {
			OkHttp.initialize(this)
		} catch (e: Throwable) {
			// Ignore initialization errors
		}
		if (ACRA.isACRASenderServiceProcess()) {
			return
		}
		entryPoint.settings().reconcileAfterAppUpgrade(BuildConfig.VERSION_CODE)
		AppCompatDelegate.setDefaultNightMode(entryPoint.settings().theme)
		// TLS 1.3 support for Android < 10
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			try {
				Security.insertProviderAt(Conscrypt.newProvider(), 1)
			} catch (e: Throwable) {
				// Ignore
			}
		}
		setupActivityLifecycleCallbacks()
		// 百益/TB 广告 SDK 初始化（主线程，不阻塞启动流程）
		try {
			android.os.Handler(android.os.Looper.getMainLooper()).post {
				entryPoint.rewardVideoManager().init(null)
			}
		} catch (e: Throwable) {
			// 广告 SDK 初始化失败不影响主流程
		}
		processLifecycleScope.launch {
			runCatching {
				ACRA.errorReporter.putCustomData("isOriginalApp", entryPoint.appValidator().isOriginalApp.getOrNull().toString())
				ACRA.errorReporter.putCustomData("isMiui", RomCompat.isMiui.getOrNull().toString())
			}
		}
		if (!entryPoint.settings().isEntityGraphMigrated ||
			!entryPoint.settings().isLegacyFavouriteProjectionMigrationCompleted
		) {
			val request = OneTimeWorkRequestBuilder<com.mangaverse.app.entitygraph.work.EntityGraphMigrationWorker>().build()
			WorkManager.getInstance(this).enqueue(request)
			entryPoint.settings().isEntityGraphMigrated = true
		}
		if (!entryPoint.settings().isLegacyEntityNameCollisionRepairCompleted) {
			val request = OneTimeWorkRequestBuilder<
				com.mangaverse.app.entitygraph.work.EntityNameCollisionRepairWorker,
			>().build()
			WorkManager.getInstance(this).enqueueUniqueWork(
				com.mangaverse.app.entitygraph.work.EntityNameCollisionRepairWorker.UNIQUE_WORK_NAME,
				ExistingWorkPolicy.KEEP,
				request,
			)
		}
		processLifecycleScope.launch(Dispatchers.Default) {
			runCatching {
				if (entryPoint.settings().requiresWorkMigrationNormalization) {
					entryPoint.favouritesRepository().normalizeWorkFavouritesIfNeeded()
					entryPoint.historyRepository().normalizeWorkHistoryIfNeeded()
					entryPoint.settings().requiresWorkMigrationNormalization = false
				}
				setupDatabaseObservers()
				entryPoint.localStorageChanges().collect(entryPoint.localContentIndexProvider().get())
			}
		}
		try {
			entryPoint.workScheduleManager().init()
		} catch (e: Throwable) {
			e.printStackTrace()
		}
		try {
			entryPoint.mihonExtensionManager().initialize()
		} catch (e: Throwable) {
			e.printStackTrace()
		}
		processLifecycleScope.launch(Dispatchers.IO) {
			try {
				com.mangaverse.app.core.extensions.GlobalExtensionManager.initialize(this@BaseApp)
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}
	}

	override fun attachBaseContext(base: Context) {
		super.attachBaseContext(base)
		if (ACRA.isACRASenderServiceProcess()) {
			return
		}
		initAcra {
			buildConfigClass = BuildConfig::class.java
			reportFormat = StringFormat.JSON
			httpSender {
				uri = getString(R.string.url_error_report)
				basicAuthLogin = getString(R.string.acra_login)
				basicAuthPassword = getString(R.string.acra_password)
				httpMethod = HttpSender.Method.POST
			}
			reportContent = listOf(
				ReportField.PACKAGE_NAME,
				ReportField.INSTALLATION_ID,
				ReportField.APP_VERSION_CODE,
				ReportField.APP_VERSION_NAME,
				ReportField.ANDROID_VERSION,
				ReportField.PHONE_MODEL,
				ReportField.STACK_TRACE,
				ReportField.CRASH_CONFIGURATION,
				ReportField.CUSTOM_DATA,
			)

			dialog {
				text = getString(R.string.crash_text)
				title = getString(R.string.error_occurred)
				positiveButtonText = getString(R.string.send)
				resIcon = R.drawable.ic_alert_outline
				resTheme = android.R.style.Theme_Material_Light_Dialog_Alert
			}
		}
		com.mangaverse.app.core.logs.CrashLogWriter.install(this)
	}

	@WorkerThread
	private fun setupDatabaseObservers() {
		val tracker = entryPoint.database().get().invalidationTracker
		entryPoint.databaseObserversProvider().get().forEach {
			tracker.addObserver(it)
		}
	}

	private fun setupActivityLifecycleCallbacks() {
		entryPoint.activityLifecycleCallbacks().forEach {
			registerActivityLifecycleCallbacks(it)
		}
	}

	@EntryPoint
	@InstallIn(SingletonComponent::class)
	interface BaseAppEntryPoint {
		fun databaseObserversProvider(): Provider<Set<@JvmSuppressWildcards InvalidationTracker.Observer>>
		fun activityLifecycleCallbacks(): Set<@JvmSuppressWildcards ActivityLifecycleCallbacks>
		fun database(): Provider<MangaDatabase>
		fun settings(): AppSettings
		fun workerFactory(): HiltWorkerFactory
		fun appValidator(): AppValidator
		fun workScheduleManager(): WorkScheduleManager
		fun localContentIndexProvider(): Provider<LocalContentIndex>
		@LocalStorageChanges
		fun localStorageChanges(): MutableSharedFlow<LocalContent?>
		fun mihonExtensionManager(): MihonExtensionManager
		fun captchaAutoResolveCoordinator(): com.mangaverse.app.core.exceptions.resolve.CaptchaAutoResolveCoordinator
		fun jsonSourceManager(): com.mangaverse.app.core.jsonsource.JsonSourceManager
		fun externalExtensionRepoRepository(): com.mangaverse.app.extensions.repo.ExternalExtensionRepoRepository
		fun extensionInstallService(): com.mangaverse.app.extensions.install.ExtensionInstallService
		fun contentSourcesRepository(): com.mangaverse.app.explore.data.ContentSourcesRepository
		fun favouritesRepository(): com.mangaverse.app.favourites.domain.FavouritesRepository
		fun historyRepository(): com.mangaverse.app.history.data.HistoryRepository
		fun workResolver(): com.mangaverse.app.work.domain.WorkResolver
		fun imageLoader(): ImageLoader
		fun rewardVideoManager(): com.mangaverse.app.commercial.data.RewardVideoManager
	}
}
