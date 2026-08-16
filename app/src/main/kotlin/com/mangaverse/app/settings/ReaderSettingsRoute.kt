package com.mangaverse.app.settings

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import com.mangaverse.app.R
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.settings.compose.ReaderSettingsScreen
import com.mangaverse.app.core.ui.theme.KototoroTheme

@Composable
fun ReaderSettingsRoute(
    settings: AppSettings,
    onReaderTapActionsClick: () -> Unit,
    onReaderAiSettingsEntryClick: () -> Unit,
) {
    ReaderSettingsScreen(
        settings = settings,
        onReaderTapActionsClick = onReaderTapActionsClick,
        onReaderAiSettingsEntryClick = onReaderAiSettingsEntryClick,
    )
}
