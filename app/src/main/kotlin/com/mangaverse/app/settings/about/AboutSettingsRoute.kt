package com.mangaverse.app.settings.about

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.mangaverse.app.R
import com.mangaverse.app.core.nav.router
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.ui.theme.KototoroTheme
import com.mangaverse.app.core.util.ext.observeEvent
import com.mangaverse.app.settings.SettingsActivity
import com.mangaverse.app.settings.SettingsDestination
import com.mangaverse.app.settings.compose.AboutSettingsScreen
import javax.inject.Inject

@Composable
fun AboutSettingsRoute(
    settings: AppSettings,
    viewModel: AboutSettingsViewModel,
    onLinkClick: (String) -> Unit,
    onCrashLogsClick: () -> Unit,
) {
    AboutSettingsScreen(
        onLinkClick = { key -> onLinkClick(key) },
        onCrashLogsClick = onCrashLogsClick,
    )
}
