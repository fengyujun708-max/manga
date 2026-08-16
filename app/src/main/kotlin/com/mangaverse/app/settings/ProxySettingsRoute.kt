package com.mangaverse.app.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.settings.compose.ProxySettingsScreen

@Composable
fun ProxySettingsRoute(
    settings: AppSettings,
    testSummaryFlow: MutableStateFlow<String?>,
    isTestRunningFlow: MutableStateFlow<Boolean>,
    onTestConnection: () -> Unit,
) {
    val testSummary by testSummaryFlow.collectAsState()
    val isTestRunning by isTestRunningFlow.collectAsState()
    ProxySettingsScreen(
        settings = settings,
        testSummary = testSummary,
        isTestRunning = isTestRunning,
        onTestConnection = onTestConnection,
    )
}
