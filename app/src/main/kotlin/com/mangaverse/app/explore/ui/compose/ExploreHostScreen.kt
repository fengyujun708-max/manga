package com.mangaverse.app.explore.ui.compose

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import coil3.request.ImageRequest.Builder
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.yield
import com.mangaverse.app.R
import com.mangaverse.app.core.model.ContentSourceAvailability
import com.mangaverse.app.core.model.ContentSourceInfo
import com.mangaverse.app.core.model.getLocale
import com.mangaverse.app.core.model.isLocal
import com.mangaverse.app.core.model.getTitle
import com.mangaverse.app.core.model.unwrap
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.ListMode
import com.mangaverse.app.core.prefs.observeAsState
import com.mangaverse.app.core.ui.compose.ContentSourceResolvedIcon
import com.mangaverse.app.core.ui.compose.AppLayoutTokens
import com.mangaverse.app.core.ui.compose.CompactTopBarHorizontalPadding
import com.mangaverse.app.core.ui.compose.HorizontalRailAnimatedVisibility
import com.mangaverse.app.core.ui.compose.rememberRailAnimationFactor
import com.mangaverse.app.core.ui.compose.KototoroPullToRefreshBox
import com.mangaverse.app.core.ui.compose.LocalHeroTransitionInProgress
import com.mangaverse.app.core.ui.compose.LocalNavAnimatedVisibilityScope
import com.mangaverse.app.core.ui.compose.LocalSharedTransitionScope
import com.mangaverse.app.core.ui.compose.VerticalRailAnimatedVisibility
import com.mangaverse.app.core.ui.compose.clearFailedContentSourceIcons
import com.mangaverse.app.core.ui.compose.compactPosterRailCardStyle
import com.mangaverse.app.core.ui.compose.contentCoverSharedKey
import com.mangaverse.app.core.ui.compose.HeroCoverSnapshotStore
import com.mangaverse.app.core.ui.compose.logHeroTransition
import com.mangaverse.app.core.ui.compose.performSelectionHapticFeedback
import com.mangaverse.app.core.ui.compose.rememberHorizontalRailScrollIntensity
import com.mangaverse.app.core.ui.compose.sharedCoverMemoryCacheKey
import com.mangaverse.app.core.ui.compose.ScrollToTopEffect
import com.mangaverse.app.core.ui.compose.rememberSafePainter
import com.mangaverse.app.core.ui.compose.unclippedBoundsInWindow
import com.mangaverse.app.core.ui.theme.LocalMaterialExpressiveComponentsEnabled
import com.mangaverse.app.core.ui.image.panoramaBlur
import com.mangaverse.app.core.util.ext.mangaExtra
import com.mangaverse.app.details.ui.model.DetailsOrigin
import com.mangaverse.app.explore.ui.ExploreViewModel
import com.mangaverse.app.explore.ui.model.ContentSourceItem
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.list.ui.model.LoadingState
import com.mangaverse.app.list.ui.model.secondaryTitleText
import com.mangaverse.app.list.ui.model.supportingText
import com.mangaverse.app.list.ui.model.buildInfoText
import com.mangaverse.app.core.parser.external.ExternalContentSource
import com.mangaverse.app.parsers.model.ContentType
import java.util.Locale

private const val BrowseLoadMoreBuffer = 4
private val SourceGridHorizontalPadding = AppLayoutTokens.compactItemHorizontalPadding

private inline fun traceExploreRoute(message: () -> String) = Unit

internal data class SourceQuickAccessMetrics(
    val preferredColumns: Int,
    val minCardWidth: androidx.compose.ui.unit.Dp,
    val cardHeight: androidx.compose.ui.unit.Dp,
    val gridSpacing: androidx.compose.ui.unit.Dp,
    val iconContainerSize: androidx.compose.ui.unit.Dp,
    val iconSize: androidx.compose.ui.unit.Dp,
    val titleTextSize: TextUnit,
)

@Immutable
private data class ExploreScreenPrefs(
    val gridScale: Float,
    val isSourcesGroupedByLanguage: Boolean,
    val browseListMode: ListMode,
    val panoramaCoverBlur: Int,
)

private data class SourceQuickAccessGroup(
    val title: String?,
    val sources: List<ContentSourceItem>,
)

private data class SourceQuickAccessRows(
    val title: String?,
    val rows: List<List<ContentSourceItem>>,
)

private data class BrowseSourceItems(
    val sources: List<ContentSourceItem>,
    val isLoadingOnly: Boolean,
)

private fun prepareBrowseSourceItems(items: List<ListModel>): BrowseSourceItems {
    val sources = ArrayList<ContentSourceItem>()
    items.forEach { item ->
        if (item is ContentSourceItem) {
            sources += item
        }
    }
    return BrowseSourceItems(
        sources = sources,
        isLoadingOnly = sources.isEmpty() && items.any { it is LoadingState },
    )
}

internal fun sourceQuickAccessMetrics(gridScale: Float): SourceQuickAccessMetrics {
    val titleTextSize = resolveSourceQuickAccessTitleTextSize(gridScale)
    return when {
        gridScale <= 0.8f -> SourceQuickAccessMetrics(
            preferredColumns = 5,
            minCardWidth = 64.dp,
            cardHeight = 92.dp,
            gridSpacing = 4.dp,
            iconContainerSize = 56.dp,
            iconSize = 46.dp,
            titleTextSize = titleTextSize,
        )
        gridScale < 1.15f -> SourceQuickAccessMetrics(
            preferredColumns = 4,
            minCardWidth = 80.dp,
            cardHeight = 108.dp,
            gridSpacing = 5.dp,
            iconContainerSize = 68.dp,
            iconSize = 56.dp,
            titleTextSize = titleTextSize,
        )
        else -> SourceQuickAccessMetrics(
            preferredColumns = 3,
            minCardWidth = 108.dp,
            cardHeight = 134.dp,
            gridSpacing = 6.dp,
            iconContainerSize = 88.dp,
            iconSize = 72.dp,
            titleTextSize = titleTextSize,
        )
    }
}

internal fun resolveSourceQuickAccessTitleTextSize(gridScale: Float): TextUnit {
    val normalized = ((gridScale.coerceIn(0.5f, 1.5f) - 0.5f) / 1f).coerceIn(0f, 1f)
    return (10f + 4f * normalized).sp
}

private fun calculateSourceGridColumns(
    availableWidth: androidx.compose.ui.unit.Dp,
    metrics: SourceQuickAccessMetrics,
    browseListMode: ListMode,
): Int {
    if (browseListMode != ListMode.GRID && browseListMode != ListMode.COMPACT_GRID) {
        return 1
    }
    val spacing = metrics.gridSpacing
    val rawColumns = ((availableWidth + spacing) / (metrics.minCardWidth + spacing))
        .toInt()
        .coerceAtLeast(1)
    return rawColumns.coerceAtLeast(metrics.preferredColumns)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun KototoroExploreHostRoute(
    appRouter: AppRouter,
    contentPadding: PaddingValues,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    onSourceSelectionTopBarChanged: (ExploreSourceSelectionTopBarState?) -> Unit = {},
    onNavigateToDetails: ((DetailsOrigin, String?) -> Unit)? = null,
    onOpenSourceList: ((com.mangaverse.app.parsers.model.ContentSource) -> Unit)? = null,
    onOpenUniverse: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
) {
    val sourceItems by exploreViewModel.content.collectAsStateWithLifecycle()
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }
    ScrollToTopEffect {
        listState.scrollToItem(0)
    }
    var savedBrowseListIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedBrowseListOffset by rememberSaveable { mutableIntStateOf(0) }
    var shouldRestoreBrowseScroll by rememberSaveable { mutableStateOf(false) }
    var hasLeftBrowse by rememberSaveable { mutableStateOf(false) }
    var canRestoreBrowseScroll by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? androidx.activity.ComponentActivity
    val settings = remember(context.applicationContext) { AppSettings(context.applicationContext) }
    val screenPrefs by settings.observeAsState(
        AppSettings.KEY_GRID_SIZE,
        AppSettings.KEY_SOURCES_GROUPED_BY_LANGUAGE,
        AppSettings.KEY_LIST_MODE_BROWSE,
        AppSettings.KEY_PANORAMA_BLUR,
    ) {
        ExploreScreenPrefs(
            gridScale = gridSize / 100f,
            isSourcesGroupedByLanguage = isSourcesGroupedByLanguage,
            browseListMode = browseListMode,
            panoramaCoverBlur = panoramaCoverBlur,
        )
    }
    val gridScale = screenPrefs.gridScale
    val panoramaCoverBlur = screenPrefs.panoramaCoverBlur
    val isSourcesGroupedByLanguage = screenPrefs.isSourcesGroupedByLanguage
    val browseListMode = screenPrefs.browseListMode
    var selectedSourceIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val hapticFeedback = LocalHapticFeedback.current
    val browseSourceItems = remember(sourceItems) {
        prepareBrowseSourceItems(sourceItems)
    }
    val sources = browseSourceItems.sources
    val sourceInfoById = remember(sources) {
        buildMap(sources.size) {
            sources.forEach { source ->
                put(source.id, source.source)
            }
        }
    }
    val isSourcesLoadingOnly = browseSourceItems.isLoadingOnly
    val isBrowseContentReady = sources.isNotEmpty()
    val currentSourceTrace by rememberUpdatedState(sourceItems.size to sources.size)
    LaunchedEffect(sourceItems) {
        traceExploreRoute {
            "content emitted lifecycle=${lifecycleOwner.lifecycle.currentState} " +
                "sourceModels=${sourceItems.size} sources=${sources.size} sourceLoading=$isSourcesLoadingOnly"
        }
    }
    LaunchedEffect(contentPadding.calculateTopPadding(), contentPadding.calculateBottomPadding()) {
        traceExploreRoute {
            "insets top=${contentPadding.calculateTopPadding()} bottom=${contentPadding.calculateBottomPadding()} " +
                "list=${listState.firstVisibleItemIndex}:${listState.firstVisibleItemScrollOffset} " +
                "saved=$savedBrowseListIndex:$savedBrowseListOffset restore=$shouldRestoreBrowseScroll " +
                "left=$hasLeftBrowse canRestore=$canRestoreBrowseScroll"
        }
    }
    val selectedSources = remember(selectedSourceIds, sourceInfoById) {
        selectedSourceIds.mapNotNull(sourceInfoById::get)
    }
    val sourceMetrics = remember(gridScale) { sourceQuickAccessMetrics(gridScale) }
    var isSourcesExpanded by rememberSaveable(sources.size, browseListMode, isSourcesGroupedByLanguage) {
        mutableStateOf(false)
    }
    val sourceContentWidth = remember(configuration.screenWidthDp, contentPadding, layoutDirection) {
        configuration.screenWidthDp.dp -
            contentPadding.calculateStartPadding(layoutDirection) -
            contentPadding.calculateEndPadding(layoutDirection) -
            SourceGridHorizontalPadding * 2
    }
    val sourceColumns = remember(sourceContentWidth, sourceMetrics, browseListMode) {
        calculateSourceGridColumns(
            availableWidth = sourceContentWidth,
            metrics = sourceMetrics,
            browseListMode = browseListMode,
        )
    }
    val sourceCollapsedVisibleCount = remember(sourceColumns) { sourceColumns * 5 }
    val sourceGroups = remember(sources, isSourcesGroupedByLanguage, context) {
        sources.toQuickAccessGroups(
            isGroupedByLanguage = isSourcesGroupedByLanguage,
            context = context,
        )
    }
    val areSourcesExpanded = isSourcesExpanded
    val visibleSourceGroups = remember(sourceGroups, sourceCollapsedVisibleCount, areSourcesExpanded) {
        sourceGroups.takeVisibleSourceGroups(
            maxSources = if (areSourcesExpanded) Int.MAX_VALUE else sourceCollapsedVisibleCount,
        )
    }
    val visibleSourceRows = remember(visibleSourceGroups, sourceColumns) {
        visibleSourceGroups.map { group ->
            SourceQuickAccessRows(
                title = group.title,
                rows = group.sources.chunked(sourceColumns),
            )
        }
    }
    val hasMoreSources = sources.size > sourceCollapsedVisibleCount

    BackHandler(enabled = selectedSourceIds.isNotEmpty()) {
        selectedSourceIds = emptySet()
    }

    SideEffect {
        if (selectedSourceIds.isNotEmpty()) {
            val isSingleSelection = selectedSources.size == 1
            val canPin = selectedSources.isNotEmpty() && selectedSources.all { !it.isPinned }
            val canUnpin = selectedSources.isNotEmpty() && selectedSources.all { it.isPinned }
            val canDisable = selectedSources.isNotEmpty() && !exploreViewModel.isAllSourcesEnabled.value && selectedSources.all {
                val unwrapped = it.mangaSource.unwrap()
                !unwrapped.isLocal && unwrapped !is ExternalContentSource
            }
            val canDelete = selectedSources.isNotEmpty() && selectedSources.all { it.mangaSource is ExternalContentSource }
            val markEmptyTitleRes = if (selectedSources.all { it.availability == ContentSourceAvailability.EMPTY }) {
                R.string.source_mark_available
            } else {
                R.string.source_mark_empty
            }

            onSourceSelectionTopBarChanged(
                ExploreSourceSelectionTopBarState(
                    selectedCount = selectedSourceIds.size,
                    isSingleSelection = isSingleSelection,
                    canPin = canPin,
                    canUnpin = canUnpin,
                    canDisable = canDisable,
                    canDelete = canDelete,
                    markEmptyTitleRes = markEmptyTitleRes,
                    onClearSelection = { selectedSourceIds = emptySet() },
                    onSettings = {
                        selectedSources.singleOrNull()?.let { appRouter.openSourceSettings(it) }
                        selectedSourceIds = emptySet()
                    },
                    onDisable = {
                        exploreViewModel.disableSources(selectedSources)
                        selectedSourceIds = emptySet()
                    },
                    onDelete = {
                        selectedSources.forEach { item ->
                            (item.mangaSource as? ExternalContentSource)?.let { source ->
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_DELETE,
                                    android.net.Uri.parse("package:${source.packageName}"),
                                )
                                activity?.startActivity(intent)
                            }
                        }
                        selectedSourceIds = emptySet()
                    },
                    onShortcut = {
                        selectedSources.singleOrNull()?.let { exploreViewModel.requestPinShortcut(it) }
                        selectedSourceIds = emptySet()
                    },
                    onPin = {
                        exploreViewModel.setSourcesPinned(selectedSources, isPinned = true)
                        selectedSourceIds = emptySet()
                    },
                    onUnpin = {
                        exploreViewModel.setSourcesPinned(selectedSources, isPinned = false)
                        selectedSourceIds = emptySet()
                    },
                    onToggleEmptyAvailability = {
                        exploreViewModel.toggleEmptySourceAvailability(selectedSources)
                        selectedSourceIds = emptySet()
                    },
                ),
            )
        } else {
            onSourceSelectionTopBarChanged(null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onSourceSelectionTopBarChanged(null)
        }
    }

    DisposableEffect(lifecycleOwner) {
        traceExploreRoute {
            "route mounted lifecycle=${lifecycleOwner.lifecycle.currentState} " +
                "list=${listState.firstVisibleItemIndex}:${listState.firstVisibleItemScrollOffset} " +
                "saved=$savedBrowseListIndex:$savedBrowseListOffset restore=$shouldRestoreBrowseScroll " +
                "left=$hasLeftBrowse canRestore=$canRestoreBrowseScroll"
        }
        val observer = LifecycleEventObserver { _, event ->
            traceExploreRoute {
                val (sourceModelCount, sourceCount) = currentSourceTrace
                "lifecycle event=$event state=${lifecycleOwner.lifecycle.currentState} " +
                    "sourceModels=$sourceModelCount sources=$sourceCount " +
                    "list=${listState.firstVisibleItemIndex}:${listState.firstVisibleItemScrollOffset} " +
                    "saved=$savedBrowseListIndex:$savedBrowseListOffset restore=$shouldRestoreBrowseScroll " +
                    "left=$hasLeftBrowse canRestore=$canRestoreBrowseScroll"
            }
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    if (shouldRestoreBrowseScroll) {
                        hasLeftBrowse = true
                        canRestoreBrowseScroll = false
                        return@LifecycleEventObserver
                    }
                    val index = listState.firstVisibleItemIndex
                    val offset = listState.firstVisibleItemScrollOffset
                    if (index != 0 || offset != 0) {
                        savedBrowseListIndex = index
                        savedBrowseListOffset = offset
                        shouldRestoreBrowseScroll = true
                    } else {
                        savedBrowseListIndex = 0
                        savedBrowseListOffset = 0
                        shouldRestoreBrowseScroll = true
                    }
                    hasLeftBrowse = true
                    canRestoreBrowseScroll = false
                }
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME -> {
                    if (shouldRestoreBrowseScroll && hasLeftBrowse) {
                        canRestoreBrowseScroll = true
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            traceExploreRoute {
                "route disposed lifecycle=${lifecycleOwner.lifecycle.currentState} " +
                    "list=${listState.firstVisibleItemIndex}:${listState.firstVisibleItemScrollOffset} " +
                    "saved=$savedBrowseListIndex:$savedBrowseListOffset restore=$shouldRestoreBrowseScroll " +
                    "left=$hasLeftBrowse canRestore=$canRestoreBrowseScroll"
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(
        isBrowseContentReady,
        shouldRestoreBrowseScroll,
        canRestoreBrowseScroll,
        savedBrowseListIndex,
        savedBrowseListOffset,
    ) {
        if (shouldRestoreBrowseScroll || canRestoreBrowseScroll) {
            traceExploreRoute {
                "restore evaluate contentReady=$isBrowseContentReady " +
                    "list=${listState.firstVisibleItemIndex}:${listState.firstVisibleItemScrollOffset} " +
                    "saved=$savedBrowseListIndex:$savedBrowseListOffset restore=$shouldRestoreBrowseScroll " +
                    "left=$hasLeftBrowse canRestore=$canRestoreBrowseScroll"
            }
        }
        if (!shouldRestoreBrowseScroll || !canRestoreBrowseScroll || !isBrowseContentReady) {
            return@LaunchedEffect
        }
        val targetIndex = savedBrowseListIndex.coerceAtLeast(0)
        val totalItems = snapshotFlow { listState.layoutInfo.totalItemsCount }
            .filter { it > targetIndex }
            .first()
        val restoreIndex = targetIndex.coerceAtMost(totalItems - 1)
        val restoreOffset = savedBrowseListOffset
        if (listState.firstVisibleItemIndex == restoreIndex &&
            listState.firstVisibleItemScrollOffset == restoreOffset
        ) {
            traceExploreRoute { "restore skipped alreadyAt=$restoreIndex:$restoreOffset" }
            shouldRestoreBrowseScroll = false
            hasLeftBrowse = false
            canRestoreBrowseScroll = false
            return@LaunchedEffect
        }
        traceExploreRoute {
            "restore started target=$restoreIndex:$restoreOffset totalItems=$totalItems " +
                "current=${listState.firstVisibleItemIndex}:${listState.firstVisibleItemScrollOffset}"
        }
        repeat(if (restoreIndex == 0 && restoreOffset == 0) 3 else 1) {
            listState.scrollToItem(
                index = restoreIndex,
                scrollOffset = restoreOffset,
            )
            yield()
            if (listState.firstVisibleItemIndex == restoreIndex &&
                listState.firstVisibleItemScrollOffset == restoreOffset
            ) {
                return@repeat
            }
        }
        if (listState.firstVisibleItemIndex != restoreIndex ||
            listState.firstVisibleItemScrollOffset != restoreOffset
        ) {
            traceExploreRoute {
                "restore mismatch target=$restoreIndex:$restoreOffset " +
                    "actual=${listState.firstVisibleItemIndex}:${listState.firstVisibleItemScrollOffset}"
            }
            return@LaunchedEffect
        }
        traceExploreRoute {
            "restore completed target=$restoreIndex:$restoreOffset " +
                "actual=${listState.firstVisibleItemIndex}:${listState.firstVisibleItemScrollOffset}"
        }
        shouldRestoreBrowseScroll = false
        hasLeftBrowse = false
        canRestoreBrowseScroll = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ===== 内容流 =====
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 120.dp,
            ),
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize(),
        ) {
                sourceQuickAccessItems(
                    metrics = sourceMetrics,
                    browseListMode = browseListMode,
                    columns = sourceColumns,
                    visibleGroups = visibleSourceRows,
                    selectedSourceIds = selectedSourceIds,
                    hasMoreSources = hasMoreSources,
                    isExpanded = areSourcesExpanded,
                    topBackgroundOverlap = 0.dp,
                    onToggleExpanded = { isSourcesExpanded = !isSourcesExpanded },
                    onManageClick = appRouter::openManageSources,
                    onOpenUniverse = onOpenUniverse,
                    onOpenAccount = onOpenAccount,
                    onSourceClick = { source ->
                        if (selectedSourceIds.isNotEmpty()) {
                            hapticFeedback.performSelectionHapticFeedback()
                            selectedSourceIds = selectedSourceIds.toggle(source.id)
                        } else {
                            onOpenSourceList?.invoke(source.source) ?: appRouter.openList(source.source, null, null)
                        }
                    },
                    onSourceLongClick = { source ->
                        selectedSourceIds = selectedSourceIds.toggle(source.id)
                    },
                )
                if (isSourcesLoadingOnly) {
                    item(key = "source_quick_access_loading", contentType = "source_quick_access_loading") {
                        BrowseSourcesSkeleton(
                            metrics = sourceMetrics,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(start = CompactTopBarHorizontalPadding, end = CompactTopBarHorizontalPadding, bottom = 36.dp),
                        )
                    }
                }
            }
        }
    }

@Composable
private fun SourcesQuickAccessSection(
    sources: List<ContentSourceItem>,
    metrics: SourceQuickAccessMetrics,
    browseListMode: ListMode,
    isGroupedByLanguage: Boolean,
    selectedSourceIds: Set<Long>,
    forceExpanded: Boolean = false,
    onSourceClick: (ContentSourceItem) -> Unit,
    onSourceLongClick: (ContentSourceItem) -> Unit,
    onManageClick: () -> Unit,
    onOpenUniverse: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_extension),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.explore_tab_sources),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TextButton(
                onClick = onManageClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            ) {
                Text(
                    text = stringResource(R.string.extension_management),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(
                onClick = onOpenUniverse,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            ) {
                Text(
                    text = "漫画宇宙",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            TextButton(
                onClick = onOpenAccount,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            ) {
                Text(
                    text = "登录",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            var isExpanded by rememberSaveable(sources.size) { mutableStateOf(false) }
            val columns = remember(maxWidth, metrics, browseListMode) {
                calculateSourceGridColumns(
                    availableWidth = maxWidth,
                    metrics = metrics,
                    browseListMode = browseListMode,
                )
            }
            val collapsedRowCount = if (maxWidth < 520.dp) 5 else 4
            val collapsedVisibleCount = columns * collapsedRowCount
            val groupedSources = remember(sources, isGroupedByLanguage, context) {
                sources.toQuickAccessGroups(
                    isGroupedByLanguage = isGroupedByLanguage,
                    context = context,
                )
            }
            val effectiveExpanded = forceExpanded || isExpanded
            val visibleGroups = remember(groupedSources, collapsedVisibleCount, effectiveExpanded) {
                groupedSources.takeVisibleSourceGroups(
                    maxSources = if (effectiveExpanded) Int.MAX_VALUE else collapsedVisibleCount,
                )
            }
            val hasMoreSources = !forceExpanded && sources.size > collapsedVisibleCount

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                visibleGroups.forEach { group ->
                    group.title?.let { title ->
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 2.dp),
                        )
                    }
                    SourceQuickAccessGrid(
                        metrics = metrics,
                        browseListMode = browseListMode,
                        columns = columns,
                        sources = group.sources,
                        selectedSourceIds = selectedSourceIds,
                        onSourceClick = onSourceClick,
                        onSourceLongClick = onSourceLongClick,
                    )
                }
                if (hasMoreSources) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = if (effectiveExpanded) {
                                stringResource(R.string.show_less)
                            } else {
                                "${stringResource(R.string.show_more)} (${sources.size - collapsedVisibleCount})"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceQuickAccessGrid(
    metrics: SourceQuickAccessMetrics,
    browseListMode: ListMode,
    columns: Int,
    sources: List<ContentSourceItem>,
    selectedSourceIds: Set<Long>,
    onSourceClick: (ContentSourceItem) -> Unit,
    onSourceLongClick: (ContentSourceItem) -> Unit,
) {
    val rows = remember(sources, columns) { sources.chunked(columns) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(metrics.gridSpacing),
    ) {
        rows.forEach { rowSources ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(metrics.gridSpacing),
            ) {
                rowSources.forEach { source ->
                    Box(modifier = Modifier.weight(1f)) {
                        SourceQuickAccessCard(
                            metrics = metrics,
                            browseListMode = browseListMode,
                            source = source,
                            isSelected = source.id in selectedSourceIds,
                            onClick = { onSourceClick(source) },
                            onLongClick = { onSourceLongClick(source) },
                        )
                    }
                }
                repeat(columns - rowSources.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun LazyListScope.sourceQuickAccessItems(
    metrics: SourceQuickAccessMetrics,
    browseListMode: ListMode,
    columns: Int,
    visibleGroups: List<SourceQuickAccessRows>,
    selectedSourceIds: Set<Long>,
    hasMoreSources: Boolean,
    isExpanded: Boolean,
    topBackgroundOverlap: androidx.compose.ui.unit.Dp,
    onToggleExpanded: () -> Unit,
    onManageClick: () -> Unit,
    onOpenUniverse: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onSourceClick: (ContentSourceItem) -> Unit,
    onSourceLongClick: (ContentSourceItem) -> Unit,
) {
    item(key = "source_quick_access_header", contentType = "source_quick_access_header") {
        SourceQuickAccessHeader(
            onManageClick = onManageClick,
            onOpenUniverse = onOpenUniverse,
            onOpenAccount = onOpenAccount,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(
                    start = CompactTopBarHorizontalPadding,
                    end = CompactTopBarHorizontalPadding,
                    top = topBackgroundOverlap,
                    bottom = 4.dp,
                ),
        )
    }
    visibleGroups.forEachIndexed { groupIndex, group ->
        group.title?.let { title ->
            item(
                key = "source_group_${groupIndex}_$title",
                contentType = "source_group_header",
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(
                            start = SourceGridHorizontalPadding,
                            end = SourceGridHorizontalPadding,
                            top = 4.dp,
                            bottom = 4.dp,
                        ),
                )
            }
        }
        val rows = group.rows
        itemsIndexed(
            items = rows,
            key = { rowIndex, rowSources ->
                val firstId = rowSources.firstOrNull()?.id ?: rowIndex.toLong()
                "source_row_${groupIndex}_${rowIndex}_$firstId"
            },
            contentType = { _, _ -> "source_row" },
        ) { rowIndex, rowSources ->
            SourceQuickAccessRow(
                metrics = metrics,
                browseListMode = browseListMode,
                columns = columns,
                sources = rowSources,
                selectedSourceIds = selectedSourceIds,
                onSourceClick = onSourceClick,
                onSourceLongClick = onSourceLongClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(
                        start = SourceGridHorizontalPadding,
                        end = SourceGridHorizontalPadding,
                        bottom = if (rowIndex == rows.lastIndex) 0.dp else metrics.gridSpacing,
                    ),
            )
        }
    }
    if (hasMoreSources) {
        item(key = "source_quick_access_more", contentType = "source_quick_access_more") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(
                    onClick = onToggleExpanded,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = if (isExpanded) {
                            stringResource(R.string.show_less)
                        } else {
                            stringResource(R.string.show_more)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceQuickAccessHeader(
    onManageClick: () -> Unit,
    onOpenUniverse: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_extension),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.explore_tab_sources),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onManageClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            ) {
                Text(
                    text = stringResource(R.string.extension_management),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(
                onClick = onOpenUniverse,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            ) {
                Text(
                    text = "漫画宇宙",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            TextButton(
                onClick = onOpenAccount,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            ) {
                Text(
                    text = "登录",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SourceQuickAccessRow(
    metrics: SourceQuickAccessMetrics,
    browseListMode: ListMode,
    columns: Int,
    sources: List<ContentSourceItem>,
    selectedSourceIds: Set<Long>,
    onSourceClick: (ContentSourceItem) -> Unit,
    onSourceLongClick: (ContentSourceItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(metrics.gridSpacing),
    ) {
        sources.forEach { source ->
            Box(modifier = Modifier.weight(1f)) {
                SourceQuickAccessCard(
                    metrics = metrics,
                    browseListMode = browseListMode,
                    source = source,
                    isSelected = source.id in selectedSourceIds,
                    onClick = { onSourceClick(source) },
                    onLongClick = { onSourceLongClick(source) },
                )
            }
        }
        repeat(columns - sources.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SourceQuickAccessCard(
    metrics: SourceQuickAccessMetrics,
    browseListMode: ListMode,
    source: ContentSourceItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val actualSource = source.source.mangaSource
    val title = actualSource.getTitle(context)
    val isGridCard = browseListMode == ListMode.GRID || browseListMode == ListMode.COMPACT_GRID
    val expressive = LocalMaterialExpressiveComponentsEnabled.current
    val cardShape = RoundedCornerShape(
        when {
            expressive && isGridCard -> 20.dp
            expressive -> 18.dp
            isGridCard -> 14.dp
            else -> 12.dp
        },
    )
    val cardBackground = when {
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.background
    }
    val iconShape = RoundedCornerShape(if (expressive) 14.dp else if (isGridCard) 14.dp else 12.dp)
    val iconBackground = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (expressive) 0.62f else if (isGridCard) 0.44f else 0.52f,
    )

    if (isGridCard) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.cardHeight)
                .clip(cardShape)
                .background(cardBackground)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(metrics.iconContainerSize)
                    .clip(iconShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                ContentSourceResolvedIcon(
                    source = actualSource,
                    modifier = Modifier.size(metrics.iconSize),
                    styleResId = R.style.FaviconDrawable_SourceIcon,
                    throttleNetworkLoad = true,
                    contentDescription = title,
                )
                SourceAvailabilityBadge(
                    availability = source.source.availability,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
                if (source.source.isPinned) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        tonalElevation = 1.dp,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin_small),
                            contentDescription = stringResource(R.string.source_pinned),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(3.dp)
                                .size(10.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = metrics.titleTextSize,
                    lineHeight = (metrics.titleTextSize.value + 2f).sp,
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(cardShape)
                .background(cardBackground)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(iconShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                ContentSourceResolvedIcon(
                    source = actualSource,
                    modifier = Modifier.size(28.dp),
                    styleResId = R.style.FaviconDrawable_SourceIcon,
                    throttleNetworkLoad = true,
                    contentDescription = title,
                )
                SourceAvailabilityBadge(
                    availability = source.source.availability,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
                if (source.source.isPinned) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        tonalElevation = 1.dp,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pin_small),
                            contentDescription = stringResource(R.string.source_pinned),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(2.dp)
                                .size(9.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (browseListMode == ListMode.DETAILED_LIST) 2.dp else 0.dp),
            ) {
                Text(
                    text = title,
                    style = if (browseListMode == ListMode.DETAILED_LIST) {
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (browseListMode == ListMode.DETAILED_LIST) {
                    Text(
                        text = actualSource.getLocale()?.getDisplayName(Locale.getDefault()).orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceAvailabilityBadge(
    availability: ContentSourceAvailability,
    modifier: Modifier = Modifier,
) {
    if (availability != ContentSourceAvailability.EMPTY) {
        return
    }
    Surface(
        modifier = modifier.padding(3.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.error,
        tonalElevation = 1.dp,
    ) {
        Text(
            text = stringResource(R.string.source_empty_badge_short),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

private fun Set<Long>.toggle(id: Long): Set<Long> {
    return if (id in this) this - id else this + id
}

@Composable
private fun sourceTypeAccent(contentType: ContentType): Color = when (contentType) {
    ContentType.VIDEO -> MaterialTheme.colorScheme.tertiary
    ContentType.NOVEL -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
}


private fun List<ContentSourceItem>.toQuickAccessGroups(
    isGroupedByLanguage: Boolean,
    context: android.content.Context,
): List<SourceQuickAccessGroup> {
    if (isEmpty()) {
        return emptyList()
    }
    if (!isGroupedByLanguage) {
        return listOf(SourceQuickAccessGroup(title = null, sources = this))
    }
    val result = ArrayList<SourceQuickAccessGroup>()
    val (pinned, unpinned) = partition { it.source.isPinned }
    if (pinned.isNotEmpty()) {
        result += SourceQuickAccessGroup(
            title = context.getString(R.string.source_pinned),
            sources = pinned,
        )
    }
    val grouped = unpinned
        .groupBy { sourceItem ->
            sourceItem.source.mangaSource.getLocale()
                ?.getDisplayName(Locale.getDefault())
                ?.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                }
                ?: context.getString(R.string.other)
        }
        .toSortedMap()
    grouped.forEach { (language, sourcesInLanguage) ->
        if (sourcesInLanguage.isNotEmpty()) {
            result += SourceQuickAccessGroup(
                title = language,
                sources = sourcesInLanguage,
            )
        }
    }
    return result
}

private fun List<SourceQuickAccessGroup>.takeVisibleSourceGroups(
    maxSources: Int,
): List<SourceQuickAccessGroup> {
    if (maxSources == Int.MAX_VALUE) {
        return this
    }
    var remaining = maxSources
    val result = ArrayList<SourceQuickAccessGroup>(size)
    for (group in this) {
        if (remaining <= 0) break
        val visibleSources = group.sources.take(remaining)
        if (visibleSources.isNotEmpty()) {
            result += group.copy(sources = visibleSources)
            remaining -= visibleSources.size
        }
    }
    return result
}

@Composable
private fun BrowseSourcesSkeleton(
    metrics: SourceQuickAccessMetrics,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(metrics.gridSpacing),
    ) {
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(metrics.gridSpacing),
            ) {
                repeat(metrics.preferredColumns.coerceAtMost(4)) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ExploreSkeletonBlock(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(metrics.cardHeight),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreSkeletonBlock(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
    )
}

private fun buildExploreCoverRequest(
    context: android.content.Context,
    coverUrl: String?,
    content: com.mangaverse.app.parsers.model.Content,
    size: Int? = null,
    blurPercent: Int = 0,
    sharedMemoryCacheKey: String? = null,
    crossfadeEnabled: Boolean = true,
): ImageRequest {
    val builder = ImageRequest.Builder(context)
        .data(normalizeExploreCoverUrl(coverUrl))
        .mangaExtra(content)
        .crossfade(crossfadeEnabled)
        .panoramaBlur(blurPercent)
    if (sharedMemoryCacheKey != null) {
        builder.memoryCacheKey(sharedMemoryCacheKey)
        builder.diskCacheKey(sharedMemoryCacheKey)
    }
    if (size != null) {
        builder.size(size)
    }
    return builder.build()
}

private fun normalizeExploreCoverUrl(url: String?): String? = when {
    url == null -> null
    url.startsWith("//") -> "https:$url"
    else -> url
}
