package com.mangaverse.app.settings

import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mangaverse.app.R
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.observeAsState
import com.mangaverse.app.core.ui.theme.KototoroTheme
import com.mangaverse.app.settings.compose.NotificationSettingsScreen
import com.mangaverse.app.settings.compose.NotificationSettingsUiState
import com.mangaverse.app.settings.utils.RingtonePickContract
import com.mangaverse.app.tracker.work.TrackerNotificationHelper
import javax.inject.Inject

@Composable
fun NotificationSettingsRoute(
    settings: AppSettings,
    onNotificationSoundClick: () -> Unit,
    onNotificationVibrateClick: () -> Unit,
) {
    val context = LocalContext.current
    val isTrackerNotificationsEnabled = settings.observeAsState(
        AppSettings.KEY_TRACKER_NOTIFICATIONS,
    ) { isTrackerNotificationsEnabled }.value
    val notificationSound = settings.observeAsState(
        AppSettings.KEY_NOTIFICATIONS_SOUND,
    ) { notificationSound }.value
    val notificationLight = settings.observeAsState(
        AppSettings.KEY_NOTIFICATIONS_LIGHT,
    ) { notificationLight }.value
    val snackbarHostState = remember { SnackbarHostState() }
    val ringtoneSummary = RingtoneManager.getRingtone(context, notificationSound)
        ?.getTitle(context)
        ?: context.getString(R.string.silent)

    val state = NotificationSettingsUiState(
        isTrackerNotificationsEnabled = isTrackerNotificationsEnabled,
        ringtoneSummary = ringtoneSummary,
        isNotificationLightEnabled = notificationLight,
        isNotificationsInfoVisible = !isTrackerNotificationsEnabled,
    )

    NotificationSettingsScreen(
        notificationsTitle = context.getString(R.string.notifications),
        state = state,
        snackbarHostState = snackbarHostState,
        onTrackerNotificationsEnabledChange = { settings.isTrackerNotificationsEnabled = it },
        onNotificationSoundClick = onNotificationSoundClick,
        onNotificationVibrateClick = onNotificationVibrateClick,
        onNotificationLightChange = { settings.notificationLight = it },
    )
}
