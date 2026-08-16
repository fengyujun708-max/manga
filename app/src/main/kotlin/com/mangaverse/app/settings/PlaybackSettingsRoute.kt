package com.mangaverse.app.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import javax.inject.Inject
import com.mangaverse.app.R
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.ui.theme.KototoroTheme
import com.mangaverse.app.settings.compose.PlaybackSettingsScreen


@Composable
fun PlaybackSettingsRoute(
    settings: AppSettings,
    onAiSettingsClick: () -> Unit,
) {
    PlaybackSettingsScreen(
        settings = settings,
        onAiSettingsClick = onAiSettingsClick,
    )
}
