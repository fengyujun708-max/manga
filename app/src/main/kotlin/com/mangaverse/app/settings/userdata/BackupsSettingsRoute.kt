package com.mangaverse.app.settings.userdata

import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.snackbar.Snackbar
import com.mangaverse.app.R
import com.mangaverse.app.core.nav.router
import com.mangaverse.app.core.os.OpenDocumentTreeHelper
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.observeAsState
import com.mangaverse.app.core.ui.theme.KototoroTheme
import com.mangaverse.app.core.util.ext.getDisplayMessage
import com.mangaverse.app.core.util.ext.tryLaunch
import com.mangaverse.app.settings.SettingsActivity
import com.mangaverse.app.settings.compose.BackupsSettingsScreen
import com.mangaverse.app.settings.compose.BackupsSettingsUiState
import com.mangaverse.app.settings.compose.SettingsChoiceOption
import com.mangaverse.app.settings.compose.WebDavRemoteBackupUiItem
import javax.inject.Inject
import com.mangaverse.app.backups.external.ExternalBackupApp
import com.mangaverse.app.backups.ui.periodical.PeriodicalBackupSettingsViewModel

@Composable
fun BackupsSettingsRoute(
    settings: AppSettings,
    viewModel: PeriodicalBackupSettingsViewModel,
    onBackupOutputClick: () -> Unit,
    onCreateBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onImportKotatsuOrLegacyBackupClick: () -> Unit,
    onExportKotatsuBackupClick: () -> Unit,
    onExportMihonBackupClick: () -> Unit,
    onExportUsagiBackupClick: () -> Unit,
    onImportExternalBackupFilePick: (ExternalBackupApp) -> Unit,
) {
    val context = LocalContext.current
    val lastBackupDate = viewModel.lastBackupDate.collectAsStateWithLifecycle().value
    val backupDirectory = viewModel.backupsDirectory.collectAsStateWithLifecycle().value
    val webDavLastAction = viewModel.webDavLastAction.collectAsStateWithLifecycle().value
    val isWebDavCheckLoading = viewModel.isWebDavCheckLoading.collectAsStateWithLifecycle().value
    val webDavUploadBusyMessageRes = viewModel.webDavUploadBusyMessageRes.collectAsStateWithLifecycle().value
    val webDavRestoreBusyMessageRes = viewModel.webDavRestoreBusyMessageRes.collectAsStateWithLifecycle().value
    val webDavRemoteBackups = viewModel.webDavRemoteBackups.collectAsStateWithLifecycle().value
    val isWebDavRemoteBackupBusy = viewModel.isWebDavRemoteBackupBusy.collectAsStateWithLifecycle().value
    val backupFrequency =
        settings.observeAsState(AppSettings.KEY_BACKUP_PERIODICAL_FREQUENCY) { periodicalBackupFrequency }.value
    val isPeriodicalTrimEnabled =
        settings.observeAsState(AppSettings.KEY_BACKUP_PERIODICAL_TRIM) { isPeriodicalBackupTrimEnabled }.value
    val periodicalBackupCount =
        settings.observeAsState(AppSettings.KEY_BACKUP_PERIODICAL_COUNT) { periodicalBackupCount }.value
    val isWebDavEnabled =
        settings.observeAsState(AppSettings.KEY_BACKUP_WEBDAV_ENABLED) { isBackupWebDavUploadEnabled }.value
    val webDavServerUrl =
        settings.observeAsState(AppSettings.KEY_BACKUP_WEBDAV_URL) { backupWebDavServerUrl.orEmpty() }.value
    val webDavUsername =
        settings.observeAsState(AppSettings.KEY_BACKUP_WEBDAV_USERNAME) { backupWebDavUsername.orEmpty() }.value
    val webDavPassword =
        settings.observeAsState(AppSettings.KEY_BACKUP_WEBDAV_PASSWORD) { backupWebDavPassword.orEmpty() }.value
    val webDavRemotePath =
        settings.observeAsState(AppSettings.KEY_BACKUP_WEBDAV_PATH) { backupWebDavRemotePath.orEmpty() }.value
    val isWebDavAutoRestoreEnabled =
        settings.observeAsState(AppSettings.KEY_BACKUP_WEBDAV_AUTO_RESTORE) { isBackupWebDavAutoRestoreEnabled }.value
    val isWebDavKeepLocalCopyEnabled =
        settings.observeAsState(AppSettings.KEY_BACKUP_WEBDAV_KEEP_LOCAL_COPY) { isBackupWebDavKeepLocalCopyEnabled }.value
    val snackbarHostState = remember { SnackbarHostState() }
    val backupFrequencyLabels = context.resources.getStringArray(R.array.backup_frequency)
    val backupFrequencyValues = context.resources.getStringArray(R.array.values_backup_frequency)
    val backupFrequencyOptions = backupFrequencyLabels.zip(backupFrequencyValues).mapNotNull { (label, value) ->
        value.toFloatOrNull()?.let { SettingsChoiceOption(it, label) }
    }
    var isExternalImportDialogVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel.onError, context, snackbarHostState) {
        viewModel.onError.collect { event ->
            event?.consume { error ->
                snackbarHostState.showSnackbar(error.getDisplayMessage(context.resources))
            }
        }
    }
    LaunchedEffect(viewModel.onActionDone, context, snackbarHostState) {
        viewModel.onActionDone.collect { event ->
            event?.consume { action ->
                snackbarHostState.showSnackbar(context.getString(action.stringResId))
            }
        }
    }

    val lastBackupSummary = when {
        lastBackupDate != null -> context.getString(
            R.string.last_successful_backup,
            DateUtils.getRelativeTimeSpanString(lastBackupDate.time),
        )
        else -> null
    }
    val webDavLastActionSummary = webDavLastAction?.let {
        context.getString(it.first) + " - " + DateUtils.getRelativeTimeSpanString(it.second)
    }
    val isWebDavBusy = webDavUploadBusyMessageRes != null || webDavRestoreBusyMessageRes != null
    val webDavRemoteBackupItems = webDavRemoteBackups.map { remote ->
        val file = remote.file
        val status = when (remote.restoreStatus) {
            WebDavRemoteBackupRestoreStatus.UNKNOWN -> context.getString(R.string.webdav_remote_backup_status_unknown)
            WebDavRemoteBackupRestoreStatus.RESTORABLE -> context.getString(R.string.webdav_remote_backup_status_restorable)
            WebDavRemoteBackupRestoreStatus.UNRESTORABLE -> context.getString(R.string.webdav_remote_backup_status_unrestorable)
        }
        val summary = buildString {
            append(status)
            append(" - ")
            append(Formatter.formatFileSize(context, file.size))
            append(" - ")
            append(DateUtils.getRelativeTimeSpanString(file.lastModified.time))
            file.dataVersion?.let {
                append(" - v")
                append(it)
            }
            append(" - ")
            append(file.namespace.name)
        }
        WebDavRemoteBackupUiItem(
            file = file,
            title = file.name,
            summary = summary,
            restoreStatus = remote.restoreStatus,
        )
    }
    val isAnyWebDavBusy = isWebDavBusy || isWebDavRemoteBackupBusy
    val state = BackupsSettingsUiState(
        backupOutputSummary = when (backupDirectory) {
            null -> context.getString(R.string.invalid_value_message)
            "" -> ""
            else -> backupDirectory
        },
        isBackupOutputInvalid = backupDirectory == null,
        backupFrequency = backupFrequency,
        isPeriodicalTrimEnabled = isPeriodicalTrimEnabled,
        periodicalBackupCount = periodicalBackupCount,
        lastBackupSummary = lastBackupSummary,
        isExternalImportDialogVisible = isExternalImportDialogVisible,
        isWebDavEnabled = isWebDavEnabled,
        webDavServerUrl = webDavServerUrl,
        webDavUsername = webDavUsername,
        webDavPassword = webDavPassword,
        webDavRemotePath = webDavRemotePath,
        isWebDavCheckLoading = isWebDavCheckLoading,
        isWebDavAutoRestoreEnabled = isWebDavAutoRestoreEnabled,
        isWebDavKeepLocalCopyEnabled = isWebDavKeepLocalCopyEnabled,
        webDavLastActionSummary = webDavLastActionSummary,
        isWebDavPolicyNoteVisible = !isWebDavKeepLocalCopyEnabled && isWebDavEnabled,
        webDavUploadBusySummary = webDavUploadBusyMessageRes?.let(context::getString),
        webDavRestoreBusySummary = webDavRestoreBusyMessageRes?.let(context::getString),
        webDavRemoteBackupBusySummary = if (isWebDavRemoteBackupBusy) {
            context.getString(R.string.webdav_remote_backups_loading)
        } else {
            null
        },
        webDavRemoteBackups = webDavRemoteBackupItems,
        isWebDavBusy = isAnyWebDavBusy,
    )

    BackupsSettingsScreen(
        backupRestoreTitle = context.getString(R.string.backup_restore),
        state = state,
        snackbarHostState = snackbarHostState,
        backupFrequencyOptions = backupFrequencyOptions,
        onBackupOutputClick = onBackupOutputClick,
        onBackupFrequencyChange = { settings.periodicalBackupFrequency = it },
        onPeriodicalTrimChange = { settings.isPeriodicalBackupTrimEnabled = it },
        onPeriodicalBackupCountChange = { settings.periodicalBackupCount = it },
        onCreateBackupClick = onCreateBackupClick,
        onRestoreBackupClick = onRestoreBackupClick,
        onImportKotatsuOrLegacyBackupClick = onImportKotatsuOrLegacyBackupClick,
        onExportKotatsuBackupClick = onExportKotatsuBackupClick,
        onExportMihonBackupClick = onExportMihonBackupClick,
        onExportUsagiBackupClick = onExportUsagiBackupClick,
        onImportExternalBackupClick = { isExternalImportDialogVisible = true },
        onDismissExternalImportDialog = { isExternalImportDialogVisible = false },
        onImportExternalBackupAppClick = { app ->
            isExternalImportDialogVisible = false
            onImportExternalBackupFilePick(app)
        },
        onWebDavEnabledChange = {
            viewModel.setWebDavEnabled(it)
        },
        onWebDavServerUrlChange = { settings.backupWebDavServerUrl = it },
        onWebDavUsernameChange = { settings.backupWebDavUsername = it },
        onWebDavPasswordChange = { settings.backupWebDavPassword = it },
        onWebDavRemotePathChange = { settings.backupWebDavRemotePath = it },
        onWebDavTestClick = { viewModel.checkWebDav() },
        onWebDavUploadNowClick = { viewModel.uploadWebDavNow() },
        onWebDavRestoreNowClick = { mode -> viewModel.restoreWebDavNow(mode) },
        onWebDavRefreshRemoteBackupsClick = { viewModel.refreshWebDavRemoteBackups(inspectPayloads = false) },
        onWebDavInspectRemoteBackupsClick = { viewModel.refreshWebDavRemoteBackups(inspectPayloads = true) },
        onWebDavRestoreRemoteBackupClick = { file, mode -> viewModel.restoreWebDavRemoteBackup(file, mode) },
        onWebDavDeleteRemoteBackupClick = { viewModel.deleteWebDavRemoteBackup(it) },
        onWebDavClearRemoteBackupsClick = { viewModel.clearWebDavRemoteBackups() },
        onWebDavAutoRestoreChange = { settings.isBackupWebDavAutoRestoreEnabled = it },
        onWebDavKeepLocalCopyChange = { settings.isBackupWebDavKeepLocalCopyEnabled = it },
    )
}
