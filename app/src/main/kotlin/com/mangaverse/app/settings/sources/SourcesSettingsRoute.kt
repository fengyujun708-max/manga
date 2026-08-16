package com.mangaverse.app.settings.sources

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangaverse.app.R
import com.mangaverse.app.core.nav.router
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.TriStateOption
import com.mangaverse.app.core.prefs.observeAsState
import com.mangaverse.app.core.ui.theme.KototoroTheme
import com.mangaverse.app.explore.data.SourcesSortOrder
import com.mangaverse.app.settings.SettingsActivity
import com.mangaverse.app.settings.compose.AdultContentFilterTarget
import com.mangaverse.app.settings.compose.SettingsChoiceOption
import com.mangaverse.app.settings.compose.SourcesSettingsScreen
import com.mangaverse.app.settings.compose.SourcesSettingsUiState
import javax.inject.Inject

@Composable
fun SourcesSettingsRoute(
    settings: AppSettings,
    viewModel: SourcesSettingsViewModel,
    onSetupWizardClick: () -> Unit,
    onGlobalTagBlacklistClick: () -> Unit,
) {
    val installedJarNames by viewModel.installedJarNames.collectAsStateWithLifecycle()
    val sourcesSortOrder = settings.observeAsState(AppSettings.KEY_SOURCES_ORDER) { sourcesSortOrder }.value
    val isSourcesGridMode = settings.observeAsState(AppSettings.KEY_SOURCES_GRID) { isSourcesGridMode }.value
    val isSourcesGroupedByLanguage =
        settings.observeAsState(AppSettings.KEY_SOURCES_GROUPED_BY_LANGUAGE) { isSourcesGroupedByLanguage }.value
    val jarPriorityOrder = settings.observeAsState(AppSettings.KEY_JAR_PRIORITY_ORDER) { jarPriorityOrder }.value
    val resolvedJarPriorityOrder = remember(jarPriorityOrder, installedJarNames) {
        viewModel.resolveJarPriorityOrder(jarPriorityOrder)
    }
    val isShowBrokenSources =
        settings.observeAsState(AppSettings.KEY_SHOW_BROKEN_SOURCES) { isShowBrokenSources }.value
    val isNsfwContentDisabled =
        settings.observeAsState(AppSettings.KEY_DISABLE_NSFW) { isNsfwContentDisabled }.value
    val isHistoryExcludeNsfw =
        settings.observeAsState(AppSettings.KEY_HISTORY_EXCLUDE_NSFW) { isHistoryExcludeNsfw }.value
    val isFavouritesExcludeNsfw =
        settings.observeAsState(AppSettings.KEY_FAVOURITES_EXCLUDE_NSFW) { isFavouritesExcludeNsfw }.value
    val isFeedExcludeNsfw =
        settings.observeAsState(AppSettings.KEY_FEED_EXCLUDE_NSFW) { isFeedExcludeNsfw }.value
    val isTrackerNsfwDisabled =
        settings.observeAsState(AppSettings.KEY_TRACKER_NO_NSFW) { isTrackerNsfwDisabled }.value
    val isSuggestionsExcludeNsfw =
        settings.observeAsState(AppSettings.KEY_SUGGESTIONS_EXCLUDE_NSFW) { isSuggestionsExcludeNsfw }.value
    val incognitoModeForNsfw =
        settings.observeAsState(AppSettings.KEY_INCOGNITO_NSFW) { incognitoModeForNsfw }.value
    val globalTagBlacklist =
        settings.observeAsState(AppSettings.KEY_GLOBAL_TAG_BLACKLIST) { globalTagBlacklist }.value
    val isTagsWarningsEnabled =
        settings.observeAsState(AppSettings.KEY_TAGS_WARNINGS) { isTagsWarningsEnabled }.value
    val isMirrorSwitchingEnabled =
        settings.observeAsState(AppSettings.KEY_MIRROR_SWITCHING) { isMirrorSwitchingEnabled }.value
    val snackbarHostState = remember { SnackbarHostState() }

    val sortOrderOptions = SourcesSortOrder.entries.map {
        SettingsChoiceOption(it, stringResource(it.titleResId))
    }
    val incognitoOptions = listOf(
        SettingsChoiceOption(TriStateOption.ENABLED, stringResource(R.string.enable)),
        SettingsChoiceOption(TriStateOption.ASK, stringResource(R.string.ask_every_time)),
        SettingsChoiceOption(TriStateOption.DISABLED, stringResource(R.string.disable)),
    )

    val state = SourcesSettingsUiState(
        sourcesSortOrder = sourcesSortOrder,
        isSourcesGridMode = isSourcesGridMode,
        isSourcesGroupedByLanguage = isSourcesGroupedByLanguage,
        jarPriorityOrder = resolvedJarPriorityOrder,
        isShowBrokenSources = isShowBrokenSources,
        adultContentFilterTargets = buildSet {
            if (isNsfwContentDisabled) add(AdultContentFilterTarget.SOURCES_AND_BROWSE)
            if (isHistoryExcludeNsfw) add(AdultContentFilterTarget.HISTORY)
            if (isFavouritesExcludeNsfw) add(AdultContentFilterTarget.FAVOURITES)
            if (isFeedExcludeNsfw) add(AdultContentFilterTarget.FEED)
            if (isTrackerNsfwDisabled) add(AdultContentFilterTarget.UPDATES)
            if (isSuggestionsExcludeNsfw) add(AdultContentFilterTarget.SUGGESTIONS)
        },
        incognitoModeForNsfw = incognitoModeForNsfw,
        blacklistedTagCount = globalTagBlacklist.size,
        isTagsWarningsEnabled = isTagsWarningsEnabled,
        isMirrorSwitchingEnabled = isMirrorSwitchingEnabled,
    )

    SourcesSettingsScreen(
        overviewTitle = stringResource(R.string.remote_sources),
        remoteSourcesTitle = stringResource(R.string.remote_sources),
        adultFilteringTitle = stringResource(R.string.adult_content_filtering),
        moreTitle = stringResource(R.string.more),
        state = state,
        snackbarHostState = snackbarHostState,
        sortOrderOptions = sortOrderOptions,
        incognitoOptions = incognitoOptions,
        onSourcesSortOrderChange = { settings.sourcesSortOrder = it },
        onSourcesGridModeChange = { settings.isSourcesGridMode = it },
        onSourcesGroupedByLanguageChange = { settings.isSourcesGroupedByLanguage = it },
        onSetupWizardClick = onSetupWizardClick,
        onJarPriorityOrderChange = viewModel::persistJarPriorityOrder,
        onShowBrokenSourcesChange = { settings.isShowBrokenSources = it },
        onAdultContentFilterTargetsChange = { targets ->
            val hideFromSourcesAndBrowse = AdultContentFilterTarget.SOURCES_AND_BROWSE in targets
            val hideFromHistory = AdultContentFilterTarget.HISTORY in targets
            val hideFromFavourites = AdultContentFilterTarget.FAVOURITES in targets
            val hideFromFeed = AdultContentFilterTarget.FEED in targets
            val hideFromUpdates = AdultContentFilterTarget.UPDATES in targets
            val hideFromSuggestions = AdultContentFilterTarget.SUGGESTIONS in targets
            if (hideFromSourcesAndBrowse != isNsfwContentDisabled) {
                settings.isNsfwContentDisabled = hideFromSourcesAndBrowse
            }
            if (hideFromHistory != isHistoryExcludeNsfw) {
                settings.isHistoryExcludeNsfw = hideFromHistory
            }
            if (hideFromFavourites != isFavouritesExcludeNsfw) {
                settings.isFavouritesExcludeNsfw = hideFromFavourites
            }
            if (hideFromFeed != isFeedExcludeNsfw) {
                settings.isFeedExcludeNsfw = hideFromFeed
            }
            if (hideFromUpdates != isTrackerNsfwDisabled) {
                settings.isTrackerNsfwDisabled = hideFromUpdates
            }
            if (hideFromSuggestions != isSuggestionsExcludeNsfw) {
                settings.isSuggestionsExcludeNsfw = hideFromSuggestions
            }
        },
        onIncognitoModeForNsfwChange = { settings.incognitoModeForNsfw = it },
        onGlobalTagBlacklistClick = onGlobalTagBlacklistClick,
        onTagsWarningsEnabledChange = { settings.isTagsWarningsEnabled = it },
        onMirrorSwitchingChange = { settings.isMirrorSwitchingEnabled = it },
    )
}
