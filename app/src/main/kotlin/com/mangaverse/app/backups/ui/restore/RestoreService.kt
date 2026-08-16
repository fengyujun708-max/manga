package com.mangaverse.app.backups.ui.restore

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.widget.Toast
import androidx.annotation.CheckResult
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mangaverse.app.R
import com.mangaverse.app.backups.data.BackupRepository
import com.mangaverse.app.backups.domain.BackupPayloadGuard
import com.mangaverse.app.backups.domain.BackupRestoreFormat
import com.mangaverse.app.backups.domain.BackupSection
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.backups.ui.BaseBackupRestoreService
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.util.ext.checkNotificationPermission
import com.mangaverse.app.core.util.ext.getSerializableExtraCompat
import com.mangaverse.app.core.util.ext.powerManager
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.core.util.ext.toUriOrNull
import com.mangaverse.app.core.util.ext.withPartialWakeLock
import com.mangaverse.app.core.util.progress.Progress
import java.io.File
import java.io.FileNotFoundException
import java.io.FileInputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import androidx.appcompat.R as appcompatR

@AndroidEntryPoint
@SuppressLint("InlinedApi")
class RestoreService : BaseBackupRestoreService() {

	override val notificationTag = TAG
	override val isRestoreService = true

	@Inject
	lateinit var repository: BackupRepository

	@Inject
	lateinit var settings: AppSettings

	override suspend fun IntentJobContext.processIntent(intent: Intent) {
		val notification = buildNotification(Progress.INDETERMINATE)
		setForeground(
			FOREGROUND_NOTIFICATION_ID,
			notification,
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
		)
		val source = intent.getStringExtra(AppRouter.KEY_DATA)?.toUriOrNull() ?: throw FileNotFoundException()
		val requestedSections =
			requireNotNull(intent.getSerializableExtraCompat<Array<BackupSection>>(AppRouter.KEY_ENTRIES)?.toSet())
		val restoreFormat = intent.getStringExtra(EXTRA_RESTORE_FORMAT)
			?.let(BackupRestoreFormat::valueOf)
			?: throw IllegalArgumentException("Missing restore format")
		val sections = restoreFormat.sanitize(requestedSections)
		powerManager.withPartialWakeLock(TAG) {
			val progress = MutableStateFlow(Progress.INDETERMINATE)
			val progressUpdateJob = if (checkNotificationPermission(CHANNEL_ID)) {
				launch {
					progress.collect {
						notificationManager.notify(FOREGROUND_NOTIFICATION_ID, buildNotification(it))
					}
				}
			} else {
				null
			}
			val tempFile = File.createTempFile("manual_backup_restore", ".bk.zip", cacheDir)
			val restoreResult = try {
				contentResolver.openInputStream(source)?.use { input ->
					tempFile.outputStream().use { output -> input.copyTo(output) }
				} ?: throw FileNotFoundException()
				BackupPayloadGuard.requireRestorableWorkSnapshot(
					file = tempFile,
					operation = "manual backup restore",
				)
				BackupPayloadGuard.requireRestoreFormat(tempFile, restoreFormat)
				ZipInputStream(FileInputStream(tempFile)).use { input ->
					repository.restoreBackup(
						input = input,
						sections = sections,
						progress = progress,
						restoreMode = when (restoreFormat) {
							BackupRestoreFormat.KOTOTORO_CURRENT -> BackupRepository.RestoreMode.SNAPSHOT_REPLACE
							BackupRestoreFormat.KOTATSU_OR_LEGACY_KOTOTORO -> BackupRepository.RestoreMode.MERGE
						},
					)
				}
			} finally {
				if (tempFile.exists()) tempFile.delete()
			}
			val restoreContext = repository.resolveRestoreSemanticContext(restoreResult.backupIndex)
			progressUpdateJob?.cancelAndJoin()
			showResultNotification(
				source,
				restoreResult.result,
				showLegacyJarReposImportedHint = restoreResult.legacyJarReposImported,
				showWorkMigrationNormalizationHint = restoreContext.isLegacySemanticSchema,
			)
			if (sections.contains(BackupSection.AUTH)) {
				withContext(Dispatchers.Main) {
					Toast.makeText(
						this@RestoreService,
						R.string.restore_auth_restart_hint,
						Toast.LENGTH_LONG,
					).show()
				}
			}
		}
	}

	private fun IntentJobContext.buildNotification(progress: Progress): Notification {
		return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setContentTitle(getString(R.string.restoring_backup))
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setDefaults(0)
			.setSilent(true)
			.setOngoing(true)
			.setProgress(
				progress.total.coerceAtLeast(0),
				progress.progress.coerceAtLeast(0),
				progress.isIndeterminate,
			)
			.setContentText(
				if (progress.isIndeterminate) {
					getString(R.string.processing_)
				} else {
					getString(R.string.fraction_pattern, progress.progress, progress.total)
				},
			)
			.setSmallIcon(android.R.drawable.stat_sys_upload)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
			.addAction(
				appcompatR.drawable.abc_ic_clear_material,
				applicationContext.getString(android.R.string.cancel),
				getCancelIntent(),
			).build()
	}

	companion object {

		private const val TAG = "RESTORE"
		private const val FOREGROUND_NOTIFICATION_ID = 39
		private const val EXTRA_RESTORE_FORMAT = "restore_format"

		@CheckResult
		fun start(
			context: Context,
			uri: Uri,
			sections: Set<BackupSection>,
			restoreFormat: BackupRestoreFormat,
		): Boolean = try {
			val intent = Intent(context, RestoreService::class.java)
			intent.putExtra(AppRouter.KEY_DATA, uri.toString())
			intent.putExtra(AppRouter.KEY_ENTRIES, sections.toTypedArray())
			intent.putExtra(EXTRA_RESTORE_FORMAT, restoreFormat.name)
			intent.setData(uri)
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			ContextCompat.startForegroundService(context, intent)
			true
		} catch (e: Exception) {
			e.printStackTraceDebug()
			false
		}
	}
}
