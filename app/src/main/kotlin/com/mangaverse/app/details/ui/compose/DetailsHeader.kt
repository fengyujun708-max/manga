package com.mangaverse.app.details.ui.compose

import android.text.format.Formatter
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mangaverse.app.R
import com.mangaverse.app.core.model.iconResId
import com.mangaverse.app.core.model.containsAdultTagKeyword
import com.mangaverse.app.core.model.getTitle
import com.mangaverse.app.core.model.titleResId
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.InterfaceStyle
import com.mangaverse.app.core.model.getContentType
import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.core.model.FavouriteCategory
import com.mangaverse.app.core.model.ContentSourceInfo
import com.mangaverse.app.core.ui.compose.ContentSourceIcon
import com.mangaverse.app.core.ui.compose.KototoroLoadingIndicator
import com.mangaverse.app.core.ui.compose.KototoroLinearProgressIndicator
import com.mangaverse.app.core.ui.compose.SheetDragHandle
import com.mangaverse.app.core.ui.compose.iconResForUi
import com.mangaverse.app.core.ui.compose.rememberSafePainter
import com.mangaverse.app.core.ui.compose.rememberResolvedSourceTitle
import com.mangaverse.app.core.ui.compose.sharedCoverMemoryCacheKey
import com.mangaverse.app.core.ui.model.titleRes
import com.mangaverse.app.core.ui.glass.GlassDefaults
import com.mangaverse.app.core.ui.compose.AppLayoutTokens
import com.mangaverse.app.core.ui.glass.GlassSurface
import com.mangaverse.app.core.ui.glass.LiquidGlassSurface
import com.mangaverse.app.core.ui.glass.rememberGlassPrefsOrFallback
import com.mangaverse.app.core.ui.glass.rememberGlassSurfaceColors
import com.mangaverse.app.core.ui.theme.LocalInterfaceStyleTokens
import com.mangaverse.app.core.ui.theme.LocalMaterialExpressiveComponentsEnabled
import com.mangaverse.app.core.ui.theme.LocalInterfaceStyle
import com.mangaverse.app.core.ui.theme.isDarkTheme
import com.mangaverse.app.main.ui.compose.GlassDropdownMenu
import com.mangaverse.app.main.ui.compose.CompactDropdownMenuItem
import com.mangaverse.app.main.ui.compose.CompactDropdownMenuDivider
import com.mangaverse.app.core.util.ext.mangaExtra
import com.mangaverse.app.core.util.ext.mangaSourceExtra
import com.mangaverse.app.core.util.ext.takeIfUsableImageUri
import com.mangaverse.app.core.util.ext.computeSize
import com.mangaverse.app.core.util.ext.toLocaleOrNull
import com.mangaverse.app.details.data.ContentDetails
import com.mangaverse.app.explore.data.SourcePreset
import com.mangaverse.app.details.ui.model.DetailsSourceOption
import com.mangaverse.app.details.ui.model.DetailsSourceDisplayContext
import com.mangaverse.app.details.ui.model.DetailsSourceDisplayStrings
import com.mangaverse.app.details.ui.model.DetailsSourceRole
import com.mangaverse.app.details.ui.model.DetailsSupplementAction
import com.mangaverse.app.details.ui.model.EntityChapterSourceInfo
import com.mangaverse.app.details.ui.model.HistoryInfo
import com.mangaverse.app.details.ui.model.toPresentationModel
import com.mangaverse.app.main.ui.compose.SearchFilterSheet
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentTag
import com.mangaverse.app.core.domain.model.ScrobblingStatus
import com.mangaverse.app.parsers.model.ContentType
import java.util.Locale
import kotlin.math.roundToInt

private fun Color.withDetailsMinAlpha(minAlpha: Float): Color {
    return copy(alpha = alpha.coerceAtLeast(minAlpha))
}

private fun Color.detailsPanelContainerColor(): Color = withDetailsMinAlpha(0.70f)

private fun Color.detailsButtonContainerColor(): Color = withDetailsMinAlpha(0.80f)


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailsHeader(
    mangaDetails: ContentDetails?,
    historyInfo: HistoryInfo,
    favouriteCategories: Set<FavouriteCategory>,
    readingStatus: ScrobblingStatus,
    unifiedRating: Float,
    canEditUnifiedRating: Boolean,
    metadataSourceOptions: List<DetailsSourceOption>,
    readingSourceOptions: List<DetailsSourceOption>,
    supplementalActions: List<DetailsSupplementAction>,
    resolvedContentType: ContentType?,
    metadataLanguageCode: String?,
    readingLanguageCode: String?,
    translatedTitle: String?,
    translatedDescription: String?,
    isShowingTranslation: Boolean,
    panoramaEnabled: Boolean,
    settings: AppSettings,
    collapseProgressProvider: () -> Float,
    coverVisualAlphaProvider: () -> Float,
    coverUrl: String?,
    fallbackCoverUrl: String?,
    sharedElementKey: String? = null,
    showWorkActions: Boolean = true,
    outerHorizontalPadding: Dp = AppLayoutTokens.screenHorizontalPadding,
    onInfoCardBoundsSync: (Float, Float) -> Unit,
    onCoverClick: (String?) -> Unit,
    onFavoriteClick: () -> Unit,
    onSourceClick: (ContentSource) -> Unit,
    onOpenReadingSourceSheet: () -> Unit,
    onOpenChapters: () -> Unit,
    onOpenSupplementalAction: (DetailsSupplementAction) -> Unit,
    onAuthorClick: (String) -> Unit,
    onTagClick: (ContentTag) -> Unit,
    onUpdateReadingStatus: (ScrobblingStatus) -> Unit,
    onUpdateUnifiedRating: (Float) -> Unit,
) {
    val context = LocalContext.current
    val expressive = LocalMaterialExpressiveComponentsEnabled.current
    val isDarkTheme = MaterialTheme.colorScheme.isDarkTheme()
    val immersiveTitleColor = if (isDarkTheme) Color.White else Color.Black
    val content = mangaDetails?.toContent()
    val originalTitle = content?.title.orEmpty()
    val displayTitle = translatedTitle ?: originalTitle
    val displayDescription = translatedDescription ?: mangaDetails?.description?.toString().orEmpty()
    val fallbackDescription = stringResource(R.string.no_description)
    val scrobblingStatuses = stringArrayResource(R.array.scrobbling_statuses)
    val defaultLocale = Locale.getDefault()
    val primaryAuthor = content?.authors?.firstOrNull { it.isNotBlank() }
    val author = primaryAuthor ?: stringResource(R.string.unknown_author)
    val hasKnownAuthor = primaryAuthor != null
    val originalLanguage = metadataLanguageCode
        ?.toLocaleOrNull()
        ?.getDisplayName(defaultLocale)
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(defaultLocale) else it.toString() }
        .orEmpty()
    val readingLanguage = readingLanguageCode
        ?.toLocaleOrNull()
        ?.getDisplayName(defaultLocale)
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(defaultLocale) else it.toString() }
        .orEmpty()
    val languageSummary = when {
        originalLanguage.isBlank() && readingLanguage.isBlank() -> stringResource(R.string.unknown)
        originalLanguage.isBlank() -> readingLanguage
        readingLanguage.isBlank() || readingLanguage == originalLanguage -> originalLanguage
        else -> "$originalLanguage -> $readingLanguage"
    }
    val chapterProgressLabel = when {
        historyInfo.totalChapters > 0 && historyInfo.currentChapter >= 0 -> "${historyInfo.currentChapter + 1}/${historyInfo.totalChapters}"
        historyInfo.totalChapters > 0 -> "0/${historyInfo.totalChapters}"
        else -> "-"
    }
    val isFavourite = favouriteCategories.isNotEmpty()
    val contentRating = content?.contentRating
    val alternateTitlesText = remember(isShowingTranslation, originalTitle, displayTitle, content?.altTitles) {
        buildList {
            if (isShowingTranslation && originalTitle.isNotBlank() && originalTitle != displayTitle) {
                add(originalTitle)
            }
            addAll(content?.altTitles.orEmpty().filter { it.isNotBlank() && it != displayTitle })
        }.distinct().joinToString(" / ")
    }
    val ratingLabel = remember(content?.hasRating, content?.rating, defaultLocale) {
        content
            ?.takeIf { it.hasRating }
            ?.let { String.format(defaultLocale, "%.1f", it.rating * 10f) }
    }
    val state = content?.state
    val progressLabel = if (historyInfo.history != null) {
        "${(historyInfo.percent * 100f).roundToInt()}%"
    } else {
        "-"
    }
    val localContent = mangaDetails?.local
    val onDeviceSizeLabel by produceState<String?>(
        initialValue = null,
        key1 = localContent?.file?.absolutePath,
    ) {
        val file = localContent?.file
        value = if (file != null && file.exists()) {
            Formatter.formatFileSize(context, file.computeSize())
        } else {
            null
        }
    }
    val metadataSourceOption = metadataSourceOptions.firstOrNull { it.isSelected } ?: metadataSourceOptions.firstOrNull()
    val readingSourceOption = readingSourceOptions.firstOrNull { it.isSelected } ?: readingSourceOptions.firstOrNull()
    val readingSourceLabelRes = when (resolvedContentType) {
        ContentType.VIDEO,
        ContentType.HENTAI_VIDEO -> R.string.details_playback_source
        else -> R.string.details_reading_source
    }
    val readingLanguageLabelRes = when (resolvedContentType) {
        ContentType.VIDEO,
        ContentType.HENTAI_VIDEO -> R.string.details_playback_language_short
        else -> R.string.details_reading_language_short
    }
    val metadataDisplayModel = metadataSourceOption?.resolveDisplayModel(
        role = DetailsSourceRole.ENTITY_METADATA,
        currentContent = content,
        strings = DetailsSourceDisplayStrings(
            unavailableText = stringResource(R.string.details_metadata_binding_unavailable),
            metadataBindingLabel = stringResource(R.string.details_entity_metadata_binding),
            currentProjectionLabel = stringResource(R.string.details_current_projection),
            switchableProjectionLabel = stringResource(R.string.details_switchable_projection),
        ),
        isSelected = true,
    )
    val readingDisplayModel = if (showWorkActions) {
        readingSourceOption?.resolveDisplayModel(
            role = DetailsSourceRole.READING_PROJECTION,
            currentContent = content,
            strings = DetailsSourceDisplayStrings(
                unavailableText = stringResource(R.string.details_reading_source_unavailable),
                metadataBindingLabel = stringResource(R.string.details_entity_metadata_binding),
                currentProjectionLabel = stringResource(readingSourceLabelRes),
                switchableProjectionLabel = stringResource(R.string.details_switchable_projection),
            ),
            isSelected = true,
        )
    } else {
        null
    }

    val normalizedCoverUrl = coverUrl?.takeIfUsableImageUri()
    val normalizedFallbackCoverUrl = fallbackCoverUrl?.takeIfUsableImageUri()
    var hasCoverLoadFailed by remember(normalizedCoverUrl) { mutableStateOf(false) }
    val currentCoverUrl = if (hasCoverLoadFailed && normalizedFallbackCoverUrl != null) {
        normalizedFallbackCoverUrl
    } else {
        normalizedCoverUrl
    }

    var isDescriptionExpanded by remember(settings.isDescriptionExpanded) { mutableStateOf(settings.isDescriptionExpanded) }
    var isTitleExpanded by rememberSaveable(displayTitle) { mutableStateOf(false) }
    var canExpandTitle by remember(displayTitle) { mutableStateOf(false) }
    val description = displayDescription.ifBlank { fallbackDescription }
    val collapsedDescriptionMaxLines = 3
    var canExpandDescription by remember(description) { mutableStateOf(false) }
    val coverModel = remember(content?.source?.name, content?.url, currentCoverUrl) {
        when {
            currentCoverUrl != null -> {
                val cacheKey = sharedCoverMemoryCacheKey(
                    sourceName = content?.source?.name,
                    ownerKey = content?.url,
                    url = currentCoverUrl,
                )
                ImageRequest.Builder(context)
                    .data(currentCoverUrl)
                    .memoryCacheKey(cacheKey)
                    .diskCacheKey(cacheKey)
                    .crossfade(false)
                    .apply { content?.let { mangaExtra(it) } }
                    .build()
            }
            else -> null
        }
    }
    val isNsfw = content?.isNsfw() == true
    val infoItems = buildList {
        content?.let {
            add(
                DetailsInfoItem(
                    label = stringResource(R.string.author),
                    value = author,
                    iconRes = R.drawable.ic_info_outline,
                    valueMuted = !hasKnownAuthor,
                    onClick = if (primaryAuthor != null) {
                        { onAuthorClick(primaryAuthor) }
                    } else {
                        null
                    },
                ),
            )
        }
        add(
            DetailsInfoItem(
                label = stringResource(R.string.state),
                value = state?.let { stringResource(it.titleResId) } ?: stringResource(R.string.unknown),
                iconRes = state?.iconResId ?: R.drawable.ic_info_outline,
                valueMuted = state == null,
            ),
        )
        add(
            DetailsInfoItem(
                label = stringResource(readingLanguageLabelRes),
                value = languageSummary,
                iconRes = R.drawable.ic_language,
                valueMuted = originalLanguage.isBlank() && readingLanguage.isBlank(),
            ),
        )
        add(
            DetailsInfoItem(
                label = stringResource(R.string.chapters),
                value = chapterProgressLabel,
                iconRes = R.drawable.ic_book_page,
                onClick = onOpenChapters,
                showNavigationIndicator = true,
            ),
        )
        if (localContent != null) {
            add(
                DetailsInfoItem(
                    label = stringResource(R.string.on_device),
                    value = onDeviceSizeLabel ?: "-",
                    iconRes = R.drawable.ic_storage,
                ),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = outerHorizontalPadding,
                vertical = 12.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            DetailsCoverFrame(
                coverModel = coverModel,
                contentDescription = displayTitle,
                showNsfwBadge = isNsfw,
                sourceName = content?.source?.name,
                ownerKey = content?.url,
                coverUrl = currentCoverUrl,
                sharedElementKey = sharedElementKey,
                topBadgeText = ratingLabel,
                topBadgeIconRes = R.drawable.ic_star_small,
                onClick = { onCoverClick(currentCoverUrl) },
                onState = { state ->
                    if (state is coil3.compose.AsyncImagePainter.State.Error) {
                        hasCoverLoadFailed = true
                    }
                },
                modifier = Modifier
                    .graphicsLayer {
                        val coverCollapseProgress = (collapseProgressProvider() / 0.48f).coerceIn(0f, 1f)
                        alpha = (1f - coverCollapseProgress) * coverVisualAlphaProvider().coerceIn(0f, 1f)
                    },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        val textCollapseProgress = ((collapseProgressProvider() - 0.08f) / 0.44f).coerceIn(0f, 1f)
                        alpha = 1f - textCollapseProgress
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SelectionContainer(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 27.sp,
                        ),
                        color = if (panoramaEnabled) {
                            immersiveTitleColor
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                if (canExpandTitle && !isTitleExpanded) {
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Black, Color.Transparent),
                                            startY = size.height * 0.62f,
                                            endY = size.height,
                                        ),
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                            }
                            .clickable(enabled = canExpandTitle) {
                                isTitleExpanded = !isTitleExpanded
                            },
                        maxLines = if (isTitleExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { textLayoutResult ->
                            val hasCollapsedOverflow = textLayoutResult.hasVisualOverflow ||
                                textLayoutResult.lineCount > 3
                            if (canExpandTitle != hasCollapsedOverflow) {
                                canExpandTitle = hasCollapsedOverflow
                            }
                        },
                    )
                }
                if (alternateTitlesText.isNotEmpty()) {
                    Text(
                        text = alternateTitlesText,
                        style = MaterialTheme.typography.labelMedium.copy(lineHeight = 16.sp),
                        color = if (panoramaEnabled) {
                            immersiveTitleColor.copy(alpha = 0.82f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (metadataDisplayModel != null || readingDisplayModel != null) {
                    DetailsSourceSummaryRow(
                        metadataDisplayModel = metadataDisplayModel,
                        readingDisplayModel = readingDisplayModel,
                        onMetadataIconClick = {
                            when {
                                metadataSourceOption?.source != null -> onSourceClick(metadataSourceOption.source)
                            }
                        },
                        onMetadataNameClick = {},
                        onReadingIconClick = {
                            when {
                                readingSourceOption?.source != null -> onSourceClick(readingSourceOption.source)
                            }
                        },
                        onReadingNameClick = onOpenReadingSourceSheet,
                    )
                }
                if (supplementalActions.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(supplementalActions, key = { it.title + it.url }) { action ->
                            SuggestionChip(
                                onClick = { onOpenSupplementalAction(action) },
                                label = { Text(action.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                ),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .graphicsLayer {
                            val actionsCollapseProgress =
                                ((collapseProgressProvider() - 0.18f) / 0.36f).coerceIn(0f, 1f)
                            alpha = 1f - actionsCollapseProgress
                        }
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (showWorkActions) {
                        DetailsHeaderIconButton(
                            iconRes = if (isFavourite) R.drawable.ic_heart else R.drawable.ic_heart_outline,
                            onClick = onFavoriteClick,
                            filled = isFavourite,
                            buttonSize = 32.dp,
                            iconSize = 17.dp,
                        )
                    }
                    if (showWorkActions) {
                        RatingStatusChip(
                            rating = unifiedRating,
                            canEditRating = canEditUnifiedRating,
                            status = readingStatus,
                            scrobblingStatuses = scrobblingStatuses,
                            onUpdateRating = onUpdateUnifiedRating,
                            onUpdateStatus = onUpdateReadingStatus,
                        )
                    }
                }
            }
        }

        val showProgress = historyInfo.history != null && historyInfo.percent > 0f
        val showInfoCard = infoItems.isNotEmpty() || showProgress
        if (showInfoCard) {
            DetailsInfoPanelSurface(
                panoramaEnabled = panoramaEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInRoot()
                        onInfoCardBoundsSync(bounds.top, bounds.bottom)
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    if (infoItems.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            infoItems.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    rowItems.forEach { item ->
                                        MetadataItem(
                                            label = item.label,
                                            value = item.value,
                                            iconRes = item.iconRes,
                                            modifier = Modifier.weight(1f),
                                            valueMuted = item.valueMuted,
                                            onClick = item.onClick,
                                            showNavigationIndicator = item.showNavigationIndicator,
                                        )
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    if (showProgress) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = if (infoItems.isNotEmpty()) 0.dp else 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            KototoroLinearProgressIndicator(
                                progress = { historyInfo.percent.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                            )
                            Text(
                                text = progressLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }




        DetailsReadableSurface(
            panoramaEnabled = panoramaEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.description),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                SelectionContainer {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = canExpandDescription,
                                role = Role.Button,
                            ) {
                                isDescriptionExpanded = !isDescriptionExpanded
                            }
                            .animateContentSize()
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                if (canExpandDescription && !isDescriptionExpanded) {
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Black, Color.Transparent),
                                            startY = size.height * 0.62f,
                                            endY = size.height,
                                        ),
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                            },
                        maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else collapsedDescriptionMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { textLayoutResult ->
                            val hasCollapsedOverflow = textLayoutResult.hasVisualOverflow ||
                                textLayoutResult.lineCount > collapsedDescriptionMaxLines
                            if (canExpandDescription != hasCollapsedOverflow) {
                                canExpandDescription = hasCollapsedOverflow
                            }
                        },
                    )
                }
            }
        }

        if (!content?.tags.isNullOrEmpty()) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 24.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        content?.tags.orEmpty().forEach { tag ->
                            val isSensitiveTag = isSensitiveDetailsTag(tag)
                            SuggestionChip(
                                onClick = { onTagClick(tag) },
                                modifier = Modifier.heightIn(min = 24.dp),
                                shape = RoundedCornerShape(8.dp),
                                label = {
                                    Text(
                                        text = tag.title,
                                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 12.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSensitiveTag) {
                                        Color(0xFFE3B341).copy(alpha = 0.22f)
                                    } else {
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.48f)
                                    },
                                    labelColor = if (isSensitiveTag) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = if (isSensitiveTag) {
                                        Color(0xFFE3B341).copy(alpha = 0.68f)
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsInfoPanelSurface(
    panoramaEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    DetailsReadableSurface(
        panoramaEnabled = panoramaEnabled,
        modifier = modifier,
        content = content,
    )
}

@Composable
private fun DetailsReadableSurface(
    panoramaEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isIosStyle = LocalInterfaceStyle.current == InterfaceStyle.IOS
    val shape = RoundedCornerShape(LocalInterfaceStyleTokens.current.groupCornerRadius)
    if (isIosStyle) {
        LiquidGlassSurface(
            modifier = modifier,
            style = GlassDefaults.regularStyle().copy(
                containerAlpha = 0.88f,
                borderAlpha = 0.18f,
            ),
            shape = shape,
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = if (panoramaEnabled) {
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f),
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsSourceSummaryRow(
    metadataDisplayModel: SourceOptionDisplayModel?,
    readingDisplayModel: SourceOptionDisplayModel?,
    onMetadataIconClick: () -> Unit,
    onMetadataNameClick: () -> Unit,
    onReadingIconClick: () -> Unit,
    onReadingNameClick: () -> Unit,
) {
    val metadataTitle = metadataDisplayModel?.selectorTitle.orEmpty()
    val readingTitle = readingDisplayModel?.selectorTitle.orEmpty()
    val metadataFallback = stringResource(R.string.details_metadata_binding_unavailable)
    val readingFallback = stringResource(R.string.details_reading_source_unavailable)
    BoxWithConstraints(modifier = Modifier.wrapContentWidth()) {
        val maxSegmentWidth = ((maxWidth - 1.dp) / 2f).coerceAtLeast(1.dp)
        Surface(
            modifier = Modifier.widthIn(max = maxWidth),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.26f)),
        ) {
            Row(
                modifier = Modifier.wrapContentWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SourceSummarySegment(
                    label = metadataTitle,
                    fallbackLabel = metadataFallback,
                    displayModel = metadataDisplayModel,
                    color = MaterialTheme.colorScheme.primary,
                    onIconClick = onMetadataIconClick,
                    onNameClick = onMetadataNameClick,
                    modifier = Modifier.widthIn(max = maxSegmentWidth),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                )
                SourceSummarySegment(
                    label = readingTitle,
                    fallbackLabel = readingFallback,
                    displayModel = readingDisplayModel,
                    color = MaterialTheme.colorScheme.tertiary,
                    onIconClick = onReadingIconClick,
                    onNameClick = onReadingNameClick,
                    modifier = Modifier.widthIn(max = maxSegmentWidth),
                )
            }
        }
    }
}

@Composable
private fun SourceSummarySegment(
    label: String,
    fallbackLabel: String,
    displayModel: SourceOptionDisplayModel?,
    color: Color,
    onIconClick: () -> Unit,
    onNameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasResolvedSource = displayModel != null
    Row(
        modifier = modifier
            .background(color.copy(alpha = if (hasResolvedSource) 0.18f else 0.10f))
            .padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = if (hasResolvedSource) onIconClick else onNameClick)
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            SourceSummaryIcon(displayModel = displayModel)
        }
        Text(
            text = label.ifBlank { fallbackLabel },
            modifier = Modifier.clickable(onClick = onNameClick),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasResolvedSource) 1f else 0.68f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SourceSummaryIcon(
    displayModel: SourceOptionDisplayModel?,
) {
    when {
        displayModel?.source != null -> {
            ContentSourceIcon(
                source = displayModel.source,
                modifier = Modifier.size(14.dp),
                contentDescription = null,
            )
        }
        else -> {
            Icon(
                painter = painterResource(R.drawable.ic_manga_source),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun RatingStatusChip(
    rating: Float,
    canEditRating: Boolean,
    status: ScrobblingStatus,
    scrobblingStatuses: Array<String>,
    onUpdateRating: (Float) -> Unit,
    onUpdateStatus: (ScrobblingStatus) -> Unit,
) {
    var expanded by remember(status) { mutableStateOf(false) }
    var menuAnchorBounds by remember { mutableStateOf<Rect?>(null) }
    val supportedStatuses = ScrobblingStatus.entries
    Box(
        modifier = Modifier.onGloballyPositioned { menuAnchorBounds = it.boundsInRoot() },
    ) {
        Surface(
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        ) {
            Row(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactRatingChip(
                    rating = rating,
                    enabled = canEditRating,
                    onRatingChanged = onUpdateRating,
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = scrobblingStatuses.getOrElse(status.ordinal) { status.name },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        GlassDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(28.dp),
            style = GlassDefaults.subtleStyle(),
            useRootOverlay = LocalInterfaceStyle.current == InterfaceStyle.IOS,
            anchorBounds = menuAnchorBounds,
        ) {
            supportedStatuses.forEach { candidate ->
                CompactDropdownMenuItem(
                    text = {
                        Text(
                            text = scrobblingStatuses.getOrElse(candidate.ordinal) { candidate.name },
                        )
                    },
                    onClick = {
                        expanded = false
                        onUpdateStatus(candidate)
                    },
                    leadingIcon = if (status == candidate) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun CompactRatingChip(
    rating: Float,
    enabled: Boolean,
    onRatingChanged: (Float) -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var menuAnchorBounds by remember { mutableStateOf<Rect?>(null) }
    val score = (rating.coerceIn(0f, 1f) * 10f).roundToInt()
    val contentAlpha = if (score > 0) 1f else 0.62f
    Box(
        modifier = Modifier.onGloballyPositioned { menuAnchorBounds = it.boundsInRoot() },
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable {
                    if (enabled) {
                        expanded = true
                    } else {
                        Toast.makeText(
                            context,
                            R.string.details_rating_requires_tracking,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (score > 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }.copy(alpha = if (enabled) contentAlpha else 0.45f),
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface
                }.copy(alpha = contentAlpha),
                maxLines = 1,
            )
        }
        GlassDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(24.dp),
            style = GlassDefaults.subtleStyle(),
            useRootOverlay = LocalInterfaceStyle.current == InterfaceStyle.IOS,
            anchorBounds = menuAnchorBounds,
        ) {
            (0..10).forEach { candidate ->
                CompactDropdownMenuItem(
                    text = { Text(candidate.toString()) },
                    onClick = {
                        expanded = false
                        onRatingChanged(candidate / 10f)
                    },
                    leadingIcon = if (candidate == score) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsSourceSelectorButton(
    modifier: Modifier = Modifier,
    label: String,
    currentDisplayModel: SourceOptionDisplayModel?,
    onPrimaryClick: () -> Unit,
    isMenuEnabled: Boolean,
    onMenuClick: () -> Unit,
) {
    val isPrimaryEnabled = currentDisplayModel != null

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.34f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 42.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isPrimaryEnabled) {
                                Modifier.clickable(onClick = onPrimaryClick)
                            } else {
                                Modifier
                            },
                        )
                        .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when {
                        currentDisplayModel?.source != null -> {
                            ContentSourceIcon(
                                source = currentDisplayModel.source,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        else -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_manga_source),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Text(
                        text = currentDisplayModel?.selectorTitle.orEmpty(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isPrimaryEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                )
                Box(
                    modifier = Modifier
                        .clickable(enabled = isMenuEnabled, onClick = onMenuClick)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isMenuEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        },
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private fun isSensitiveDetailsTag(tag: ContentTag): Boolean {
    return tag.title.containsAdultTagKeyword()
}

private data class SourceOptionDisplayModel(
    val title: String,
    val subtitle: String,
    val selectorTitle: String,
    val selectorSubtitle: String,
    val coverUrl: String?,
    val source: ContentSource?,
    val isSelected: Boolean,
    val badgeText: String? = null,
    val isActiveProjection: Boolean = false,
)

@Composable
private fun DetailsSourceOption.resolveDisplayModel(
    role: DetailsSourceRole,
    currentContent: Content?,
    strings: DetailsSourceDisplayStrings,
    isSelected: Boolean,
): SourceOptionDisplayModel {
    val sourceTitle = if (source != null) rememberResolvedSourceTitle(source) else ""
    val presentation = toPresentationModel(
        context = DetailsSourceDisplayContext(
            role = role,
            currentContentTitle = currentContent?.title,
            currentContentSourceName = currentContent?.source?.name,
            linkedTrackingTitle = null,
            resolvedSourceTitle = sourceTitle,
            resolvedTrackingTitle = "",
            isSelected = isSelected,
            strings = strings,
        ),
    )
    val coverUrl = coverUrl
        ?: currentContent?.coverUrl?.takeIf { source != null && currentContent.source.name == source.name }
    val selectorTitle = when {
        sourceTitle.isNotBlank() -> sourceTitle
        !subtitle.isNullOrBlank() -> subtitle.orEmpty()
        else -> presentation.title
    }
    val selectorSubtitle = when {
        presentation.title.isNotBlank() && presentation.title != selectorTitle -> presentation.title
        presentation.subtitle.isNotBlank() -> presentation.subtitle
        else -> ""
    }
    return SourceOptionDisplayModel(
        title = presentation.title,
        subtitle = presentation.subtitle,
        selectorTitle = selectorTitle,
        selectorSubtitle = selectorSubtitle,
        coverUrl = coverUrl,
        source = source,
        isSelected = isSelected,
    )
}

@Composable
private fun SourceOptionCard(
    displayModel: SourceOptionDisplayModel,
    onClick: () -> Unit,
    scrobblingStatuses: Array<String>,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val expressive = LocalMaterialExpressiveComponentsEnabled.current
    val optionCardColors = rememberGlassSurfaceColors(style = GlassDefaults.subtleStyle())
    Surface(
        modifier = modifier
            .width(112.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(if (expressive) 20.dp else 12.dp),
        color = when {
            displayModel.isActiveProjection -> if (expressive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            }
            displayModel.isSelected -> if (expressive) {
                optionCardColors.containerColor.detailsButtonContainerColor()
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            }
            expressive -> optionCardColors.containerColor.detailsPanelContainerColor()
            else -> optionCardColors.containerColor
        },
        border = when {
            displayModel.isActiveProjection -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            displayModel.isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            else -> optionCardColors.border
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp, 80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val resolvedCoverUrl = displayModel.coverUrl?.takeIfUsableImageUri()
                when {
                    resolvedCoverUrl != null -> {
                        val cacheKey = remember(displayModel.source?.name, resolvedCoverUrl) {
                            sharedCoverMemoryCacheKey(
                                sourceName = displayModel.source?.name,
                                ownerKey = displayModel.title,
                                url = resolvedCoverUrl,
                            )?.let { "${it}#details-source-cover" }
                        }
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(resolvedCoverUrl)
                                .memoryCacheKey(cacheKey)
                                .diskCacheKey(cacheKey)
                                .apply { displayModel.source?.let(::mangaSourceExtra) }
                                .crossfade(false)
                                .build(),
                            contentDescription = displayModel.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    displayModel.source != null -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            ContentSourceIcon(
                                source = displayModel.source,
                                modifier = Modifier.size(20.dp),
                                contentDescription = null,
                            )
                        }
                    }
                    else -> {
                        Icon(
                            painter = painterResource(R.drawable.ic_extension),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                displayModel.badgeText?.let { badgeText ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.92f),
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = displayModel.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = displayModel.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ReadingSourceSheet(
    currentOptions: List<DetailsSourceOption>,
    selectedOption: DetailsSourceOption?,
    searchSources: List<ContentSourceInfo>,
    searchQuery: String,
    searchSections: List<com.mangaverse.app.details.ui.ReadingSearchSectionUiState>,
    isLoading: Boolean,
    hasSearched: Boolean,
    scopeFilterUiState: com.mangaverse.app.details.ui.ReadingSearchScopeFilterUiState,
    languagePresets: List<SourcePreset>,
    activeLanguagePresetId: Long,
    currentContent: Content?,
    entityChapterSourceInfo: EntityChapterSourceInfo?,
    unavailableText: String,
    label: String,
    onDismissRequest: () -> Unit,
    onSelectOption: (DetailsSourceOption) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLanguagePresetSelected: (Long) -> Unit,
    onManageLanguagePresets: () -> Unit,
    onSourceTypeToggle: (com.mangaverse.app.core.jsonsource.SourceType) -> Unit,
    onContentKindToggle: (com.mangaverse.app.search.domain.SearchContentKind) -> Unit,
    onPinnedOnlyChange: (Boolean) -> Unit,
    onHideEmptyChange: (Boolean) -> Unit,
    onTemporaryOpenResult: (Content) -> Unit,
    onMigrateResult: (Content) -> Unit,
    onDeleteProjection: (DetailsSourceOption) -> Unit,
    onActivateProjection: (DetailsSourceOption) -> Unit,
) {
    val context = LocalContext.current
    var pendingMigrationTarget by remember { mutableStateOf<Content?>(null) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    val visibleSections = remember(searchSources, searchSections, hasSearched, isLoading) {
        when {
            searchSections.isNotEmpty() -> searchSections
            hasSearched || isLoading -> emptyList()
            else -> searchSources.map { source ->
                com.mangaverse.app.details.ui.ReadingSearchSectionUiState(source = source)
            }
        }
    }
    val isInitialSearchState = remember(searchSections, hasSearched, isLoading) {
        searchSections.isEmpty() && !hasSearched && !isLoading
    }
    val resultSections = remember(visibleSections, isInitialSearchState) {
        if (isInitialSearchState) {
            visibleSections
        } else {
            visibleSections.filter { section ->
                !section.isPending && (section.items.isNotEmpty() || section.isLoading)
            }
        }
    }
    val emptySections = remember(visibleSections, isInitialSearchState, scopeFilterUiState.hideEmpty) {
        if (scopeFilterUiState.hideEmpty || isInitialSearchState) {
            emptyList()
        } else {
            visibleSections.filter { !it.isPending && !it.isLoading && it.errorMessage == null && it.items.isEmpty() }
        }
    }
    val errorSections = remember(visibleSections, isInitialSearchState, scopeFilterUiState.hideEmpty) {
        if (scopeFilterUiState.hideEmpty || isInitialSearchState) {
            emptyList()
        } else {
            visibleSections.filter { !it.isPending && !it.isLoading && it.errorMessage != null && it.items.isEmpty() }
        }
    }
    var showEmptySources by rememberSaveable(emptySections.map { it.source.mangaSource.name }) {
        mutableStateOf(false)
    }
    var showUnavailableSources by rememberSaveable(errorSections.map { it.source.mangaSource.name }) {
        mutableStateOf(false)
    }
	    DetailsSourceOverlayDialog(
	        onDismissRequest = onDismissRequest,
	    ) { panelDragModifier ->
	            Column(
	                modifier = Modifier
	                    .fillMaxWidth()
	                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
	            verticalArrangement = Arrangement.spacedBy(12.dp),
	        ) {
	            Column(
	                modifier = panelDragModifier.fillMaxWidth(),
	                verticalArrangement = Arrangement.spacedBy(10.dp),
	            ) {
	                Text(
	                    text = stringResource(R.string.details_current_projection),
	                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
	                    color = MaterialTheme.colorScheme.onSurface,
	                )
                    Text(
                        text = buildString {
                            append(stringResource(R.string.details_current_projection_sheet_hint, label))
                            entityChapterSourceInfo
                                ?.projectionCount
                                ?.takeIf { it > 0 }
                                ?.let { count ->
                                    append(' ')
                                    append(
                                        stringResource(
                                            R.string.entity_graph_chapter_source_projection_count,
                                            count,
                                        ),
                                    )
                                }
                            if (
                                entityChapterSourceInfo?.currentReadingProjectionMangaId != null &&
                                entityChapterSourceInfo.currentReadingProjectionMangaId != entityChapterSourceInfo.activeProjectionMangaId
                            ) {
                                append(' ')
                                append(stringResource(R.string.details_temporary_projection_sheet_hint))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
	                if (currentOptions.isNotEmpty()) {
	                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
	                    itemsIndexed(
	                        items = currentOptions,
	                        key = { index, option -> "${option.key}:$index" },
	                    ) { _, option ->
                            val isTemporaryProjection =
                                option.targetMangaId != null &&
                                    entityChapterSourceInfo?.currentReadingProjectionMangaId == option.targetMangaId &&
                                    entityChapterSourceInfo.activeProjectionMangaId != option.targetMangaId
                            val isActiveProjection =
                                option.targetMangaId != null &&
                                    entityChapterSourceInfo?.activeProjectionMangaId == option.targetMangaId
                            var showMenu by remember(option.key) { mutableStateOf(false) }
                            var menuAnchorBounds by remember(option.key) { mutableStateOf<Rect?>(null) }
                            Box(
                                modifier = Modifier.onGloballyPositioned { menuAnchorBounds = it.boundsInRoot() },
                            ) {
                                SourceOptionCard(
                                    displayModel = option.resolveDisplayModel(
                                        role = DetailsSourceRole.READING_PROJECTION,
                                        currentContent = currentContent,
                                        strings = DetailsSourceDisplayStrings(
                                            unavailableText = unavailableText,
                                            metadataBindingLabel = stringResource(R.string.details_entity_metadata_binding),
                                            currentProjectionLabel = label,
                                            switchableProjectionLabel = stringResource(R.string.details_switchable_projection),
                                        ),
                                        isSelected = option == selectedOption || option.isSelected,
                                    ).copy(
                                        badgeText = when {
                                            isActiveProjection -> stringResource(R.string.details_active_projection_badge)
                                            isTemporaryProjection -> stringResource(R.string.details_temporary_projection_badge)
                                            else -> null
                                        },
                                        isActiveProjection = isActiveProjection,
                                    ),
                                    scrobblingStatuses = emptyArray(),
                                    onClick = {
                                        onDismissRequest()
                                        onSelectOption(option)
                                        },
                                    onLongClick = {
                                        if (option.targetMangaId != null) {
                                            showMenu = true
                                        }
                                    },
                                )
                                GlassDropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    // ReadingSourceSheet is rendered in its own Dialog window. The
                                    // app-level root overlay would be behind that window.
                                    useRootOverlay = false,
                                    anchorBounds = menuAnchorBounds,
                                ) {
                                    CompactDropdownMenuItem(
                                        text = { Text(stringResource(R.string.details_remove_projection)) },
                                        onClick = {
                                            showMenu = false
                                            onDeleteProjection(option)
                                        },
                                    )
                                    CompactDropdownMenuDivider()
                                    CompactDropdownMenuItem(
                                        text = { Text(stringResource(R.string.details_activate_projection)) },
                                        onClick = {
                                            showMenu = false
                                            onActivateProjection(option)
                                            onDismissRequest()
                                        },
                                    )
                                }
                            }
	                        }
	                    }
	                }
	                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
	                    SourceSearchField(
	                        value = searchQuery,
	                        onValueChange = onSearchQueryChange,
	                        onSearch = onSearch,
                            modifier = Modifier.weight(1f),
	                    )
                        FilledTonalButton(
                            onClick = onSearch,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        FilledTonalButton(
                            onClick = { showFilterSheet = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_filter_menu),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            if (scopeFilterUiState.appliedFilterCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(scopeFilterUiState.appliedFilterCount.toString())
                            }
                        }
                    }
	            }
	            Box(
	                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
            ) {
                when {
                    resultSections.isEmpty() && emptySections.isEmpty() && errorSections.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (isLoading) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = stringResource(R.string.search),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            } else {
                                Text(
                                    text = if (hasSearched) {
                                        stringResource(R.string.details_source_search_no_visible_results)
                                    } else {
                                        stringResource(R.string.nothing_found)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            itemsIndexed(
                                items = resultSections,
                                key = { index, section -> "reading_section:${section.source.mangaSource.name}:$index" },
                            ) { _, section ->
                                ReadingSearchSection(
                                    section = section,
                                    hasSearched = hasSearched,
                                    onItemClick = { item ->
                                        onTemporaryOpenResult(item)
                                    },
                                    onMigrateClick = { item ->
                                        pendingMigrationTarget = item
                                    },
                                )
                            }
                            if (emptySections.isNotEmpty()) {
                                item(key = "reading_section_empty_header") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showEmptySources = !showEmptySources },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.details_source_search_no_results_group),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.graphicsLayer {
                                                rotationZ = if (showEmptySources) 180f else 0f
                                            },
                                        )
                                    }
                                }
                                if (showEmptySources) {
                                    itemsIndexed(
                                        items = emptySections,
                                        key = { index, section -> "reading_section_empty:${section.source.mangaSource.name}:$index" },
                                    ) { _, section ->
                                        ReadingSearchSection(
                                            section = section,
                                            hasSearched = true,
                                            onItemClick = { item ->
                                                onTemporaryOpenResult(item)
                                            },
                                            onMigrateClick = { item ->
                                                pendingMigrationTarget = item
                                            },
                                        )
                                    }
                                }
                            }
                            if (errorSections.isNotEmpty()) {
                                item(key = "reading_section_errors_header") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showUnavailableSources = !showUnavailableSources },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.unavailable),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.graphicsLayer {
                                                rotationZ = if (showUnavailableSources) 180f else 0f
                                            },
                                        )
                                    }
                                }
                                if (showUnavailableSources) {
                                    itemsIndexed(
                                        items = errorSections,
                                        key = { index, section -> "reading_section_error:${section.source.mangaSource.name}:$index" },
                                    ) { _, section ->
                                        ReadingSearchSection(
                                            section = section,
                                            hasSearched = hasSearched,
                                            onItemClick = { item ->
                                                onTemporaryOpenResult(item)
                                            },
                                            onMigrateClick = { item ->
                                                pendingMigrationTarget = item
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    pendingMigrationTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingMigrationTarget = null },
            title = { Text(stringResource(R.string.manga_migration)) },
            text = {
                Text(
                    stringResource(
                        R.string.migrate_confirmation,
                        currentContent?.title.orEmpty(),
                        currentContent?.source?.getTitle(context).orEmpty(),
                        target.title,
                        target.source.getTitle(context),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingMigrationTarget = null
                        onDismissRequest()
                        onMigrateResult(target)
                    },
                ) {
                    Text(stringResource(R.string.migrate))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingMigrationTarget = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
	}

    if (showFilterSheet) {
        SearchFilterSheet(
            sourceTypes = scopeFilterUiState.sourceTypes,
            contentKinds = scopeFilterUiState.contentKinds,
            pinnedOnly = scopeFilterUiState.pinnedOnly,
            hideEmpty = scopeFilterUiState.hideEmpty,
            languagePresets = languagePresets,
            activeLanguagePresetId = activeLanguagePresetId,
            onSourceTypeToggle = onSourceTypeToggle,
            onContentKindToggle = onContentKindToggle,
            onPinnedOnlyChange = onPinnedOnlyChange,
            onHideEmptyChange = onHideEmptyChange,
            onLanguagePresetSelected = onLanguagePresetSelected,
            onManageLanguagePresets = onManageLanguagePresets,
            onDismissRequest = { showFilterSheet = false },
        )
    }
}

@Composable
private fun SourceSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val expressive = LocalMaterialExpressiveComponentsEnabled.current
    val shape = RoundedCornerShape(if (expressive) 999.dp else 22.dp)
    val searchFieldColors = rememberGlassSurfaceColors(style = GlassDefaults.subtleStyle())
    val containerColor = if (expressive) {
        if (isFocused) {
            searchFieldColors.containerColor.detailsButtonContainerColor()
        } else {
            searchFieldColors.containerColor.detailsButtonContainerColor()
        }
    } else if (isFocused) {
        searchFieldColors.containerColor
    } else {
        searchFieldColors.containerColor.copy(
            alpha = (searchFieldColors.containerColor.alpha * 0.92f).coerceAtLeast(0.12f),
        )
    }
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(
            modifier = modifier.height(44.dp),
            shape = shape,
            color = containerColor,
            border = if (expressive) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
            } else {
                searchFieldColors.border
            },
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxSize()
                    .onFocusChanged { isFocused = it.isFocused },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.bodyMedium.fontSize,
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() },
                ),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, end = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (value.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            innerTextField()
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun DetailsSourceOverlayDialog(
    onDismissRequest: () -> Unit,
    content: @Composable (panelDragModifier: Modifier) -> Unit,
) {
    var panelOffsetY by remember { mutableFloatStateOf(0f) }
    val expressive = LocalMaterialExpressiveComponentsEnabled.current
    val panelColors = rememberGlassSurfaceColors(
        style = GlassDefaults.regularStyle(),
        glassPrefs = rememberDetailsSourceOverlayGlassPrefs(),
    )
    val density = androidx.compose.ui.platform.LocalDensity.current
    val dismissThresholdPx = remember(density) {
        with(density) { 96.dp.toPx() }
    }
    val panelDragModifier = Modifier.pointerInput(dismissThresholdPx, onDismissRequest) {
        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                panelOffsetY = (panelOffsetY + dragAmount).coerceAtLeast(0f)
            },
            onDragCancel = {
                panelOffsetY = 0f
            },
            onDragEnd = {
                if (panelOffsetY > dismissThresholdPx) {
                    onDismissRequest()
                } else {
                    panelOffsetY = 0f
                }
            },
        )
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.18f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .offset { IntOffset(0, panelOffsetY.roundToInt()) }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                ),
                shape = RoundedCornerShape(topStart = if (expressive) 36.dp else 28.dp, topEnd = if (expressive) 36.dp else 28.dp),
                color = panelColors.containerColor.detailsPanelContainerColor(),
                border = panelColors.border,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SheetDragHandle(
                        modifier = Modifier
                            .then(panelDragModifier)
                            .align(Alignment.CenterHorizontally),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            content(panelDragModifier)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberDetailsSourceOverlayGlassPrefs() =
    rememberGlassPrefsOrFallback()
@Composable
private fun ReadingSearchSection(
    section: com.mangaverse.app.details.ui.ReadingSearchSectionUiState,
    hasSearched: Boolean,
    onItemClick: (Content) -> Unit,
    onMigrateClick: (Content) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expressive = LocalMaterialExpressiveComponentsEnabled.current
    val sectionColors = rememberGlassSurfaceColors(style = GlassDefaults.subtleStyle())
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (expressive) 28.dp else 20.dp),
        color = sectionColors.containerColor.detailsPanelContainerColor(),
        border = sectionColors.border,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = rememberResolvedSourceTitle(section.source.mangaSource),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            section.errorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            when {
                section.items.isNotEmpty() -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                    ) {
                        itemsIndexed(
                            items = section.items,
                            key = { index, item -> "${item.id}:${item.source.name}:$index" },
                        ) { _, item ->
                            ReadingSearchResultCard(
                                item = item,
                                onClick = { onItemClick(item) },
                                onMigrateClick = { onMigrateClick(item) },
                            )
                        }
                    }
                }
                section.isLoading -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.search),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                hasSearched && section.errorMessage == null -> {
                    Text(
                        text = stringResource(R.string.nothing_found),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
@Composable
private fun DetailsSearchRowSurface(
    content: @Composable () -> Unit,
) {
    val expressive = LocalMaterialExpressiveComponentsEnabled.current
    if (expressive) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            style = GlassDefaults.subtleStyle(),
            shape = RoundedCornerShape(24.dp),
        ) {
            content()
        }
    } else {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            style = GlassDefaults.subtleStyle(),
            shape = RoundedCornerShape(20.dp),
        ) {
            content()
        }
    }
}
@Composable
private fun SourceCoverImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (model == null) {
            Icon(
                painter = rememberSafePainter(R.drawable.ic_placeholder),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        } else {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun ReadingSearchResultRow(
    item: Content,
    onClick: () -> Unit,
) {
    val latestChapterInfo = remember(item) { item.readingSearchLatestChapterInfo() }
    val context = LocalContext.current
    val expressive = LocalMaterialExpressiveComponentsEnabled.current
    val resultCardColors = rememberGlassSurfaceColors(style = GlassDefaults.subtleStyle())
    val coverUrl = item.coverUrl?.takeIfUsableImageUri()
    val coverRequest = remember(item.id, coverUrl, item.source) {
        coverUrl?.let {
            ImageRequest.Builder(context)
                .data(it)
                .mangaSourceExtra(item.source)
                .build()
        }
    }
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        style = GlassDefaults.subtleStyle(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceCoverImage(
                model = coverRequest,
                contentDescription = item.title,
                modifier = Modifier
                    .width(42.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = rememberResolvedSourceTitle(item.source),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                latestChapterInfo?.let { latestInfo ->
                    Text(
                        text = when (latestInfo) {
                            is ReadingSearchLatestChapterInfo.Numbered -> {
                                stringResource(
                                    R.string.details_search_result_latest_chapter,
                                    latestInfo.number,
                                )
                            }
                            is ReadingSearchLatestChapterInfo.Titled -> {
                                stringResource(
                                    R.string.details_search_result_latest_title,
                                    latestInfo.title,
                                )
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ReadingSearchResultCard(
    item: Content,
    onClick: () -> Unit,
    onMigrateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestChapterInfo = remember(item) { item.readingSearchLatestChapterInfo() }
    val context = LocalContext.current
    val expressive = LocalMaterialExpressiveComponentsEnabled.current
    val resultCardColors = rememberGlassSurfaceColors(style = GlassDefaults.subtleStyle())
    val coverUrl = item.coverUrl?.takeIfUsableImageUri()
    val coverRequest = remember(item.id, coverUrl, item.source) {
        coverUrl?.let {
            ImageRequest.Builder(context)
                .data(it)
                .mangaSourceExtra(item.source)
                .build()
        }
    }
    Surface(
        modifier = modifier.width(108.dp),
        shape = RoundedCornerShape(if (expressive) 24.dp else 18.dp),
        color = resultCardColors.containerColor.detailsPanelContainerColor(),
        border = resultCardColors.border,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onMigrateClick,
                )
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SourceCoverImage(
                model = coverRequest,
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val chaptersCount = item.chapters?.size ?: 0
            if (chaptersCount > 0) {
                Text(
                    text = pluralStringResource(R.plurals.chapters, chaptersCount, chaptersCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            latestChapterInfo?.let { latestInfo ->
                Text(
                    text = when (latestInfo) {
                        is ReadingSearchLatestChapterInfo.Numbered -> {
                            stringResource(
                                R.string.details_search_result_latest_chapter,
                                latestInfo.number,
                            )
                        }
                        is ReadingSearchLatestChapterInfo.Titled -> {
                            stringResource(
                                R.string.details_search_result_latest_title,
                                latestInfo.title,
                            )
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalButton(
                onClick = onMigrateClick,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_replace),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.migrate),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private sealed interface ReadingSearchLatestChapterInfo {
    data class Numbered(val number: String) : ReadingSearchLatestChapterInfo
    data class Titled(val title: String) : ReadingSearchLatestChapterInfo
}

private fun Content.readingSearchLatestChapterInfo(): ReadingSearchLatestChapterInfo? {
    val chapters = chapters.orEmpty()
    if (chapters.isEmpty()) return null

    val numberedChapter = chapters
        .asSequence()
        .filter { it.number > 0f }
        .maxByOrNull { it.number }
    if (numberedChapter != null) {
        return ReadingSearchLatestChapterInfo.Numbered(
            numberedChapter.numberString().orEmpty(),
        )
    }

    val titledChapter = chapters.firstNotNullOfOrNull { chapter ->
        chapter.title?.takeIf { it.isNotBlank() }
    } ?: return null
    return ReadingSearchLatestChapterInfo.Titled(titledChapter)
}

@Composable
private fun EntityChapterSourceCard(
    info: EntityChapterSourceInfo,
) {
    val chapterSourceTitle = info.projectionTitle?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.entity_graph_chapter_source_unavailable)
    val supportingText = if (info.source != null) {
        buildString {
            append(stringResource(R.string.entity_graph_chapter_source_selected_hint))
            if (info.projectionCount > 1) {
                append(' ')
                append(stringResource(R.string.entity_graph_chapter_source_projection_count, info.projectionCount))
            }
        }
    } else {
        stringResource(R.string.entity_graph_chapter_source_unavailable_hint)
    }
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        style = GlassDefaults.subtleStyle(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_book_page),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.entity_graph_chapter_source),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = chapterSourceTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun Modifier.offsetX(
    maxOffset: Dp,
    progress: Float,
): Modifier = this.then(
    Modifier.offset(x = maxOffset * progress),
)
