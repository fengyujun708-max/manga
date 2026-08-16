package com.mangaverse.app.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import javax.inject.Inject
import com.mangaverse.app.R
import com.mangaverse.app.core.nav.router
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.observeAsState
import com.mangaverse.app.core.ui.theme.KototoroTheme
import com.mangaverse.app.settings.compose.ServicesSettingsScreen
import com.mangaverse.app.settings.compose.ServicesSettingsUiState

@Composable
fun ServicesSettingsRoute(
    settings: AppSettings,
    onSuggestionsClick: () -> Unit,
) {
    val context = LocalContext.current
    val suggestionsEnabled = settings.observeAsState(AppSettings.KEY_SUGGESTIONS) { isSuggestionsEnabled }.value
    val isBrowseTrackingRecommendationsEnabled =
        settings.observeAsState(AppSettings.KEY_BROWSE_TRACKING_RECOMMENDATIONS) { isBrowseTrackingRecommendationsEnabled }.value
    val isBrowseMoreTrackingRecommendationsEnabled =
        settings.observeAsState(AppSettings.KEY_BROWSE_MORE_TRACKING_RECOMMENDATIONS) {
            isBrowseMoreTrackingRecommendationsEnabled
        }.value
    val isRelatedContentEnabled =
        settings.observeAsState(AppSettings.KEY_RELATED_MANGA) { isRelatedContentEnabled }.value
    val isReadingTimeEstimationEnabled =
        settings.observeAsState(AppSettings.KEY_READING_TIME) { isReadingTimeEstimationEnabled }.value
    val snackbarHostState = remember { SnackbarHostState() }

    val state = ServicesSettingsUiState(
        suggestionsSummary = if (suggestionsEnabled) {
            context.getString(R.string.enabled)
        } else {
            context.getString(R.string.disabled)
        },
        isBrowseTrackingRecommendationsEnabled = isBrowseTrackingRecommendationsEnabled,
        isBrowseMoreTrackingRecommendationsEnabled = isBrowseMoreTrackingRecommendationsEnabled,
        isRelatedContentEnabled = isRelatedContentEnabled,
        isReadingTimeEstimationEnabled = isReadingTimeEstimationEnabled,
    )

    ServicesSettingsScreen(
        servicesTitle = context.getString(R.string.services),
        state = state,
        snackbarHostState = snackbarHostState,
        onSuggestionsClick = onSuggestionsClick,
        onBrowseTrackingRecommendationsChange = { settings.isBrowseTrackingRecommendationsEnabled = it },
        onBrowseMoreTrackingRecommendationsChange = { settings.isBrowseMoreTrackingRecommendationsEnabled = it },
        onRelatedContentChange = { settings.isRelatedContentEnabled = it },
        onReadingTimeChange = { settings.isReadingTimeEstimationEnabled = it },
    )
}
