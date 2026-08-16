package com.mangaverse.app.backups.ui.periodical

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import com.mangaverse.app.R
import com.mangaverse.app.backups.data.BackupRepository
import com.mangaverse.app.backups.domain.BackupPayloadGuard
import com.mangaverse.app.backups.domain.BackupWebDavRestoreCoordinator
import com.mangaverse.app.backups.domain.BackupWebDavUploadCoordinator
import com.mangaverse.app.backups.domain.BackupUtils
import com.mangaverse.app.backups.domain.ExternalBackupStorage
import com.mangaverse.app.backups.data.model.BackupIndex
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.ui.util.ReversibleAction
import com.mangaverse.app.core.util.ext.MutableEventFlow
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.core.util.ext.resolveFile
import java.util.Date
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import com.mangaverse.app.backups.domain.BackupSection

data class WebDavRemoteBackupState(
	val file: BackupFileInfo,
	val restoreStatus: WebDavRemoteBackupRestoreStatus = WebDavRemoteBackupRestoreStatus.UNKNOWN,
)

enum class WebDavRemoteBackupRestoreStatus {
	UNKNOWN,
	RESTORABLE,
	UNRESTORABLE,
}

enum class ManualWebDavRestoreMode {
	SNAPSHOT_REPLACE,
	MERGE,
}

@HiltViewModel
class PeriodicalBackupSettingsViewModel @Inject constructor(
	private val settings: AppSettings,
	private val telegramUploader: TelegramBackupUploader,
	private val webDavUploader: WebDavBackupUploader,
	private val backupWebDavUploadCoordinator: BackupWebDavUploadCoordinator,
	private val backupWebDavRestoreCoordinator: BackupWebDavRestoreCoordinator,
	private val backupStorage: ExternalBackupStorage,
	private val repository: com.mangaverse.app.backups.data.BackupRepository,
	@ApplicationContext private val appContext: Context,
) : BaseViewModel() {

	companion object {
		private const val TAG = "PeriodicalBackupVM"
	}

	val isTelegramAvailable
		get() = telegramUploader.isAvailable

	val lastBackupDate = MutableStateFlow<Date?>(null)
	val backupsDirectory = MutableStateFlow<String?>("")
	val isTelegramCheckLoading = MutableStateFlow(false)
	val isWebDavCheckLoading = MutableStateFlow(false)
	val webDavUploadBusyMessageRes = MutableStateFlow<Int?>(null)
	val webDavRestoreBusyMessageRes = MutableStateFlow<Int?>(null)
	val webDavRemoteBackups = MutableStateFlow<List<WebDavRemoteBackupState>>(emptyList())
	val isWebDavRemoteBackupBusy = MutableStateFlow(false)
	val onActionDone = MutableEventFlow<ReversibleAction>()

	// 最近一次 WebDAV 操作（类型文案资源ID，发生时间毫秒）
	val webDavLastAction = MutableStateFlow<Pair<Int, Long>?>(null)

	init {
		updateSummaryData()
	}

	fun checkTelegram() {
		launchJob(Dispatchers.Default) {
			try {
				isTelegramCheckLoading.value = true
				telegramUploader.sendTestMessage()
				onActionDone.call(ReversibleAction(R.string.connection_ok, null))
			} finally {
				isTelegramCheckLoading.value = false
			}
		}
	}

	fun checkWebDav() {
		launchJob(Dispatchers.Default) {
			try {
				isWebDavCheckLoading.value = true
				webDavUploadBusyMessageRes.value = R.string.webdav_connection_check_in_progress
				webDavUploader.sendTestConnection()
				onActionDone.call(ReversibleAction(R.string.connection_ok, null))
			} finally {
				isWebDavCheckLoading.value = false
				webDavUploadBusyMessageRes.value = null
			}
		}
	}

	fun setWebDavEnabled(value: Boolean) {
		settings.isBackupWebDavUploadEnabled = value
	}

	fun uploadWebDavNow() {
		launchJob(Dispatchers.Default) {
			val output = com.mangaverse.app.backups.domain.BackupUtils.createTempFile(appContext)
			try {
				webDavUploadBusyMessageRes.value = R.string.webdav_upload_in_progress
				java.util.zip.ZipOutputStream(output.outputStream()).use {
					repository.createBackup(it, null)
				}
				// 根据设置决定是否保留本地副本
				var localCopyFailed = false
				if (settings.isBackupWebDavKeepLocalCopyEnabled) {
					// 本地副本失败不应阻断 WebDAV 上传
					runCatching {
						backupStorage.put(output)
						backupStorage.trim(settings.periodicalBackupMaxCount)
					}.onFailure { localCopyFailed = true }
				}
				backupWebDavUploadCoordinator.uploadAndCommit(
					file = output,
					uploadKind = "manual",
				)
				onActionDone.call(
					ReversibleAction(
						if (localCopyFailed) R.string.webdav_upload_success_local_copy_failed
						else R.string.webdav_upload_success,
						null,
					),
				)
				// 仅在保留本地副本时，更新上次本地备份时间展示
				if (settings.isBackupWebDavKeepLocalCopyEnabled) {
					updateLastBackupDate()
				}
				updateWebDavLastAction()
			} catch (e: Exception) {
				errorEvent.call(e)
			} finally {
				webDavUploadBusyMessageRes.value = null
				output.delete()
			}
		}
	}

	fun restoreWebDavNow(restoreMode: ManualWebDavRestoreMode = ManualWebDavRestoreMode.SNAPSHOT_REPLACE) {
		launchJob(Dispatchers.Default) {
			try {
				webDavRestoreBusyMessageRes.value = R.string.webdav_restore_in_progress
				Log.d(TAG, "restoreWebDavNow: listing current work backups once...")
				val latest = webDavUploader.getLatestBackup(RemoteNamespace.V3)
				if (latest == null) {
					Log.w(TAG, "restoreWebDavNow: no current work backups found")
					throw IllegalStateException("No current WebDAV work backups found")
				}
				Log.d(TAG, "restoreWebDavNow: found ${latest.name} (ns=${latest.namespace}, ${latest.size}b)")
				restoreWebDavBackup(latest, restoreMode)
			} catch (e: Exception) {
				Log.e(TAG, "restoreWebDavNow: failed", e)
				errorEvent.call(e)
			} finally {
				webDavRestoreBusyMessageRes.value = null
			}
		}
	}

	fun refreshWebDavRemoteBackups(inspectPayloads: Boolean = false) {
		launchJob(Dispatchers.Default) {
			try {
				isWebDavRemoteBackupBusy.value = true
				val files = webDavUploader.listAllBackupFiles()
					.sortedWith(
						compareByDescending<BackupFileInfo> { it.lastModified.time }
							.thenByDescending { it.dataVersion ?: Int.MIN_VALUE },
					)
				webDavRemoteBackups.value = files.map { WebDavRemoteBackupState(it) }
				if (inspectPayloads) {
					inspectWebDavRemoteBackups(files)
				}
			} catch (e: Exception) {
				errorEvent.call(e)
			} finally {
				isWebDavRemoteBackupBusy.value = false
			}
		}
	}

	fun restoreWebDavRemoteBackup(
		file: BackupFileInfo,
		restoreMode: ManualWebDavRestoreMode = ManualWebDavRestoreMode.SNAPSHOT_REPLACE,
	) {
		launchJob(Dispatchers.Default) {
			try {
				webDavRestoreBusyMessageRes.value = R.string.webdav_restore_in_progress
				restoreWebDavBackup(file, restoreMode)
			} catch (e: Exception) {
				Log.e(TAG, "restoreWebDavRemoteBackup: failed name=${file.name}", e)
				errorEvent.call(e)
			} finally {
				webDavRestoreBusyMessageRes.value = null
			}
		}
	}

	fun deleteWebDavRemoteBackup(file: BackupFileInfo) {
		launchJob(Dispatchers.Default) {
			try {
				isWebDavRemoteBackupBusy.value = true
				webDavUploader.deleteRemote(file.name, file.namespace)
				webDavRemoteBackups.value = webDavRemoteBackups.value.filterNot { it.file.name == file.name }
				onActionDone.call(ReversibleAction(R.string.webdav_remote_backup_deleted, null))
			} catch (e: Exception) {
				errorEvent.call(e)
			} finally {
				isWebDavRemoteBackupBusy.value = false
			}
		}
	}

	fun clearWebDavRemoteBackups() {
		launchJob(Dispatchers.Default) {
			try {
				isWebDavRemoteBackupBusy.value = true
				val files = webDavUploader.listAllBackupFiles()
				files.forEach { file ->
					webDavUploader.deleteRemote(file.name, file.namespace)
				}
				webDavRemoteBackups.value = emptyList()
				onActionDone.call(ReversibleAction(R.string.webdav_remote_backups_cleared, null))
			} catch (e: Exception) {
				errorEvent.call(e)
			} finally {
				isWebDavRemoteBackupBusy.value = false
			}
		}
	}

	private suspend fun inspectWebDavRemoteBackups(files: List<BackupFileInfo>) {
		val inspected = files.map { file ->
			val tempFile = File.createTempFile("webdav_backup_inspect", ".bk.zip", appContext.cacheDir)
			try {
				webDavUploader.downloadBackup(file.name, tempFile, file.namespace)
				runCatching {
					BackupPayloadGuard.requireRestorableWorkSnapshot(
						file = tempFile,
						operation = "WebDAV backup inspection",
					)
				}.fold(
					onSuccess = { WebDavRemoteBackupState(file, WebDavRemoteBackupRestoreStatus.RESTORABLE) },
					onFailure = { WebDavRemoteBackupState(file, WebDavRemoteBackupRestoreStatus.UNRESTORABLE) },
				)
			} finally {
				if (tempFile.exists()) tempFile.delete()
			}
		}
		webDavRemoteBackups.value = inspected
	}

	private suspend fun restoreWebDavBackup(
		file: BackupFileInfo,
		restoreMode: ManualWebDavRestoreMode,
	) {
		val tempFile = File.createTempFile("webdav_backup_manual", ".bk.zip", appContext.cacheDir)
		try {
			Log.d(TAG, "restoreWebDavBackup: downloading ${file.name}...")
			webDavUploader.downloadBackup(file.name, tempFile, file.namespace)
			val inspection = BackupPayloadGuard.requireRestorableWorkSnapshot(
				file = tempFile,
				operation = "manual WebDAV restore",
			)
			Log.d(
				TAG,
				"restoreWebDavBackup: downloaded name=${file.name} size=${tempFile.length()}b " +
					"entries=${inspection.describe()}",
			)
			Log.d(TAG, "restoreWebDavBackup: starting restore from zip...")
			val restoreResult = java.util.zip.ZipInputStream(FileInputStream(tempFile)).use { zis ->
				repository.restoreBackup(
					input = zis,
					sections = buildManualRestoreSections(),
					progress = null,
					restoreMode = restoreMode.toRepositoryRestoreMode(),
				)
			}
			Log.d(TAG, "restoreWebDavBackup: restore complete, committing...")
			val restoreContext = repository.resolveRestoreSemanticContext(restoreResult.backupIndex)
			backupWebDavRestoreCoordinator.commitManualRestore(
				state = BackupWebDavRestoreCoordinator.RestoreSemanticState(
					semanticSchemaVersion = restoreContext.semanticSchemaVersion,
					transportGeneration = restoreContext.transportGeneration,
				),
			)
			Log.d(TAG, "restoreWebDavBackup: committed, done")
			onActionDone.call(
				ReversibleAction(
					if (restoreContext.isLegacySemanticSchema && restoreResult.legacyJarReposImported) {
						R.string.webdav_restore_success_legacy_requires_normalization_with_jar_hint
					} else if (restoreContext.isLegacySemanticSchema) {
						R.string.webdav_restore_success_legacy_requires_normalization
					} else if (restoreResult.legacyJarReposImported) {
						R.string.webdav_restore_success_legacy_jar_hint
					} else {
						R.string.webdav_restore_success
					},
					null,
				),
			)
			updateWebDavLastAction()
		} finally {
			if (tempFile.exists()) tempFile.delete()
		}
	}

	private fun ManualWebDavRestoreMode.toRepositoryRestoreMode(): BackupRepository.RestoreMode {
		return when (this) {
			ManualWebDavRestoreMode.SNAPSHOT_REPLACE -> BackupRepository.RestoreMode.SNAPSHOT_REPLACE
			ManualWebDavRestoreMode.MERGE -> BackupRepository.RestoreMode.MERGE
		}
	}

	private fun buildManualRestoreSections() = setOf(
		com.mangaverse.app.backups.domain.BackupSection.INDEX,
		com.mangaverse.app.backups.domain.BackupSection.HISTORY,
		com.mangaverse.app.backups.domain.BackupSection.CATEGORIES,
		com.mangaverse.app.backups.domain.BackupSection.FAVOURITES,
		com.mangaverse.app.backups.domain.BackupSection.BOOKMARKS,
		com.mangaverse.app.backups.domain.BackupSection.STATS,
		com.mangaverse.app.backups.domain.BackupSection.WORK_HISTORY,
		com.mangaverse.app.backups.domain.BackupSection.WORK_FAVOURITES,
		com.mangaverse.app.backups.domain.BackupSection.WORK_STATS,
		com.mangaverse.app.backups.domain.BackupSection.SOURCES,
		com.mangaverse.app.backups.domain.BackupSection.EXTENSION_REPOS,
		com.mangaverse.app.backups.domain.BackupSection.SETTINGS,
		com.mangaverse.app.backups.domain.BackupSection.SETTINGS_READER_GRID,
		com.mangaverse.app.backups.domain.BackupSection.ENTITY_GRAPH_ENTITIES,
		com.mangaverse.app.backups.domain.BackupSection.ENTITY_GRAPH_BINDINGS,
		com.mangaverse.app.backups.domain.BackupSection.ENTITY_GRAPH_RELATIONS,
		com.mangaverse.app.backups.domain.BackupSection.ENTITY_GRAPH_PREFS,
	)

	fun updateSummaryData() {
		updateBackupsDirectory()
		updateLastBackupDate()
		updateWebDavLastAction()
	}

	private fun updateBackupsDirectory() = launchJob(Dispatchers.Default) {
		val dir = settings.periodicalBackupDirectory
		backupsDirectory.value = if (dir != null) {
			dir.toUserFriendlyString()
		} else {
			BackupUtils.getAppBackupDir(appContext).path
		}
	}

	private fun updateLastBackupDate() = launchJob(Dispatchers.Default) {
		lastBackupDate.value = backupStorage.getLastBackupDate()
	}

	private fun updateWebDavLastAction() = launchJob(Dispatchers.Default) {
		val upload = settings.backupWebDavLastUploadTime
		val autoRestore = settings.backupWebDavLastRestoreTime
		val manualRestore = settings.backupWebDavLastManualRestoreTime
		val max = listOf(upload, autoRestore, manualRestore).maxOrNull() ?: 0L
		val label = when (max) {
			upload -> when (settings.backupWebDavLastUploadKind) {
				"manual" -> R.string.action_manual_upload
				else -> R.string.action_auto_upload
			}
			autoRestore -> R.string.action_auto_restore
			manualRestore -> R.string.action_manual_restore
			else -> null
		}
		webDavLastAction.value = label?.let { it to max }
	}

	private fun Uri.toUserFriendlyString(): String? {
		val df = DocumentFile.fromTreeUri(appContext, this)
		if (df?.canWrite() != true) {
			return null
		}
		return resolveFile(appContext)?.path ?: toString()
	}
}
