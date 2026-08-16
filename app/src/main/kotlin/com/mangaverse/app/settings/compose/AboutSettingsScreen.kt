package com.mangaverse.app.settings.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mangaverse.app.BuildConfig
import com.mangaverse.app.R
import com.mangaverse.app.core.github.VersionId
import com.mangaverse.app.core.github.isStable
import com.mangaverse.app.core.prefs.AppSettings

@Composable
fun AboutSettingsScreen(
    onLinkClick: (key: String) -> Unit,
    onCrashLogsClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0, 0) }
        LazyColumn(state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = SettingsContentHorizontalPadding,
                end = SettingsContentHorizontalPadding,
                top = 8.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "about_overview") {
                SettingsPreferenceSection(title = stringResource(R.string.about)) {
                    SettingsActionPreference(
                        title = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                        onClick = {},
                    )
                    SettingsSectionDivider()
                    SettingsActionPreference(
                        title = stringResource(R.string.crash_logs),
                        summary = stringResource(R.string.crash_logs_summary),
                        onClick = onCrashLogsClick,
                    )
                }
            }
            item(key = "about_links") {
                SettingsPreferenceSection(title = stringResource(R.string.more)) {
                    SettingsActionPreference(
                        title = stringResource(R.string.user_manual),
                        summary = stringResource(R.string.url_user_manual),
                        onClick = { onLinkClick(AppSettings.KEY_LINK_MANUAL) },
                    )
                    SettingsSectionDivider()
                    SettingsActionPreference(
                        title = stringResource(R.string.source_code),
                        summary = stringResource(R.string.url_github),
                        onClick = { onLinkClick(AppSettings.KEY_LINK_GITHUB) },
                    )
                    SettingsSectionDivider()
                    SettingsActionPreference(
                        title = stringResource(R.string.about_donate),
                        summary = stringResource(R.string.url_donate),
                        onClick = { onLinkClick(AppSettings.KEY_LINK_DONATE) },
                    )
                    SettingsSectionDivider()
                    SettingsActionPreference(
                        title = stringResource(R.string.about_discord),
                        summary = stringResource(R.string.url_discord),
                        onClick = { onLinkClick(AppSettings.KEY_LINK_DISCORD) },
                    )
                }
            }
        }
    }
}
