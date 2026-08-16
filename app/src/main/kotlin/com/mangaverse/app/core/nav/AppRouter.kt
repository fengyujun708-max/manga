package com.mangaverse.app.core.nav

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.util.Log
import androidx.annotation.CheckResult
import androidx.annotation.UiContext
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.findFragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.hilt.android.EntryPointAccessors
import com.mangaverse.app.BuildConfig
import com.mangaverse.app.R
import com.mangaverse.app.browser.BrowserActivity
import com.mangaverse.app.browser.cloudflare.CloudFlareActivity
import com.mangaverse.app.core.exceptions.CloudFlareProtectedException
import com.mangaverse.app.core.image.CoilMemoryCacheKey
import com.mangaverse.app.core.model.FavouriteCategory
import com.mangaverse.app.core.model.ContentSourceInfo
import com.mangaverse.app.core.model.appUrl
import com.mangaverse.app.core.model.getTitle
import com.mangaverse.app.core.model.isBroken
import com.mangaverse.app.core.model.isLocal
import com.mangaverse.app.core.model.parcelable.ParcelableContent
import com.mangaverse.app.core.model.parcelable.ParcelableContentPage
import com.mangaverse.app.core.model.parcelable.ParcelableContentListFilter
import com.mangaverse.app.details.ui.model.DetailsOrigin
import com.mangaverse.app.entitygraph.domain.EntityType
import com.mangaverse.app.work.domain.WorkResolver
import com.mangaverse.app.core.network.CommonHeaders
import com.mangaverse.app.core.parser.external.ExternalContentSource
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.ReaderMode
import com.mangaverse.app.core.prefs.TriStateOption
import com.mangaverse.app.core.ui.BaseComposeActivity
import com.mangaverse.app.core.ui.compose.rememberDrawablePainter
import com.mangaverse.app.core.ui.compose.rememberSafePainter
import com.mangaverse.app.core.ui.dialog.BigButtonsAlertDialog
import com.mangaverse.app.core.ui.dialog.ErrorDetailsActivity
import com.mangaverse.app.core.ui.dialog.buildAlertDialog
import com.mangaverse.app.core.util.ext.connectivityManager
import com.mangaverse.app.core.util.ext.findActivity
import com.mangaverse.app.core.util.ext.getDisplayMessage
import com.mangaverse.app.core.util.ext.getThemeDrawable
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.core.util.ext.toSerializableThrowable
import com.mangaverse.app.core.util.ext.getParcelableExtraCompat
import com.mangaverse.app.core.util.ext.toFileOrNull
import com.mangaverse.app.core.jsonsource.JsonContentSource
import com.mangaverse.app.core.util.ext.toUriOrNull
import com.mangaverse.app.details.ui.DetailsActivity
import com.mangaverse.app.details.ui.related.RelatedContentActivity
import com.mangaverse.app.download.ui.compose.DownloadDialog
import com.mangaverse.app.download.ui.list.DownloadsActivity
import com.mangaverse.app.favourites.ui.FavouritesActivity
import com.mangaverse.app.favourites.ui.categories.FavouriteCategoriesActivity
import com.mangaverse.app.favourites.ui.categories.edit.FavouritesCategoryEditActivity
import com.mangaverse.app.favourites.ui.categories.select.compose.FavoriteCategoryDialogRoute
import com.mangaverse.app.filter.ui.FilterCoordinator
import com.mangaverse.app.filter.ui.sheet.FilterSheetRoute
import com.mangaverse.app.filter.ui.tags.TagsCatalogRoute
import com.mangaverse.app.explore.ui.model.BrowseGroupTab
import com.mangaverse.app.explore.ui.preset.SourcePresetListActivity
import com.mangaverse.app.history.ui.HistoryActivity
import com.mangaverse.app.image.ui.ImageActivity
import com.mangaverse.app.list.ui.config.ListConfigRoute
import com.mangaverse.app.list.ui.config.ListConfigSection
import com.mangaverse.app.local.ui.compose.ImportDialog
import com.mangaverse.app.local.ui.info.compose.LocalInfoDialogRoute
import com.mangaverse.app.main.ui.MainActivity
import com.mangaverse.app.main.ui.welcome.WelcomeRoute
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentPage
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentType

import com.mangaverse.app.parsers.model.ContentTag
import com.mangaverse.app.parsers.model.SortOrder
import com.mangaverse.app.parsers.util.ellipsize
import com.mangaverse.app.parsers.util.isNullOrEmpty
import com.mangaverse.app.parsers.util.mapToArray
import com.mangaverse.app.reader.ui.ReaderActivity
import com.mangaverse.app.reader.ui.ReaderState
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.parser.ContentDataRepository
import kotlinx.coroutines.launch
import com.mangaverse.app.reader.ui.colorfilter.ColorFilterConfigActivity
import com.mangaverse.app.search.domain.SearchKind
import com.mangaverse.app.search.domain.SearchContentKind
import com.mangaverse.app.search.ui.ContentListActivity
import com.mangaverse.app.search.ui.multi.SearchActivity
import com.mangaverse.app.settings.sources.blacklist.GlobalTagBlacklistActivity
import com.mangaverse.app.settings.SettingsActivity
import com.mangaverse.app.settings.override.OverrideConfigActivity
import com.mangaverse.app.settings.reader.ReaderTapGridConfigActivity
import com.mangaverse.app.settings.sources.auth.SourceAuthActivity
import com.mangaverse.app.settings.sources.catalog.SourcesCatalogActivity
import com.mangaverse.app.settings.sources.unified.UnifiedSourceKind
import com.mangaverse.app.settings.storage.ContentDirectorySelectRoute
import com.mangaverse.app.settings.storage.ContentDirectorySelectViewModel
import com.mangaverse.app.settings.storage.directories.ContentDirectoriesActivity
import com.mangaverse.app.settings.tracker.categories.TrackerCategoriesConfigRoute

import java.io.File
import androidx.appcompat.R as appcompatR

@Composable
private fun AppRouterChoiceDialog(
    icon: @Composable () -> Unit,
    title: String,
    options: List<String>,
    dismissLabel: String,
    onDismissRequest: () -> Unit,
    onOptionSelected: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = icon,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, option ->
                    TextButton(
                        onClick = { onOptionSelected(index) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(option, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(dismissLabel)
            }
        },
    )
}

class AppRouter private constructor(
    private val activity: FragmentActivity?,
    private val fragment: Fragment?,
) {

    constructor(activity: FragmentActivity) : this(activity, null)

    constructor(fragment: Fragment) : this(null, fragment)

    private val settings: AppSettings by lazy {
        EntryPointAccessors.fromApplication<AppRouterEntryPoint>(checkNotNull(contextOrNull())).settings
    }

    private val mangaRepositoryFactory: ContentRepository.Factory by lazy {
        EntryPointAccessors.fromApplication<AppRouterEntryPoint>(checkNotNull(contextOrNull())).mangaRepositoryFactory
    }

    private val contentDataRepository: ContentDataRepository by lazy {
        EntryPointAccessors.fromApplication<AppRouterEntryPoint>(checkNotNull(contextOrNull())).contentDataRepository
    }

    private val workResolver: WorkResolver by lazy {
        EntryPointAccessors.fromApplication<AppRouterEntryPoint>(checkNotNull(contextOrNull())).workResolver
    }

    private val jsonSourceManager: com.mangaverse.app.core.jsonsource.JsonSourceManager by lazy {
        EntryPointAccessors.fromApplication<AppRouterEntryPoint>(checkNotNull(contextOrNull())).jsonSourceManager
    }

    private val spaceFeatureFlagsRepository: /* DELETED */ by lazy {
        EntryPointAccessors.fromApplication<AppRouterEntryPoint>(
            checkNotNull(contextOrNull()),
        ).spaceFeatureFlagsRepository
    }

    private val spaceRepository: /* DELETED */ by lazy {
        EntryPointAccessors.fromApplication<AppRouterEntryPoint>(
            checkNotNull(contextOrNull()),
        ).spaceRepository
    }

    private fun prepareImmersiveIntent(intent: Intent): Intent {
        ImmersiveSpaceSwitcherTransition.attachDetailsOrigin(intent)
        intent.putExtra(EXTRA_HAS_IN_APP_CALLER, true)
        val immersiveSwitchEnabled = spaceFeatureFlagsRepository.flags.value.effectiveImmersiveSwitchEnabled
        val flags = immersiveTaskFlags(immersiveSwitchEnabled)
        if (immersiveSwitchEnabled) {
            intent.putExtra(EXTRA_IMMERSIVE_SESSION_SPACE_ID, spaceRepository.activeSpace.value.value)
        }
        if (flags != 0) {
            intent.addFlags(flags)
        }
        return intent
    }

    /** Activities **/

    fun openList(source: ContentSource, filter: ContentListFilter?, sortOrder: SortOrder?) {
        startActivity(listIntent(contextOrNull() ?: return, source, filter, sortOrder))
    }

    fun openList(tag: ContentTag) = openList(tag.source, ContentListFilter(tags = setOf(tag)), null)

    fun openSearch(
        query: String,
        kind: SearchKind = SearchKind.SIMPLE,
        sourceTypes: Set<com.mangaverse.app.core.jsonsource.SourceType>? = null,
        contentKinds: Set<SearchContentKind>? = null,
        advancedTitle: String? = null,
        advancedTags: String? = null,
        advancedAuthor: String? = null,
        pinnedOnly: Boolean = false,
        hideEmpty: Boolean = false,
    ) {
        val intent = Intent(contextOrNull() ?: return, SearchActivity::class.java)
            .putExtra(KEY_QUERY, query)
            .putExtra(KEY_KIND, kind)
            .putExtra(KEY_ADVANCED_TITLE, advancedTitle)
            .putExtra(KEY_ADVANCED_TAGS, advancedTags)
            .putExtra(KEY_ADVANCED_AUTHOR, advancedAuthor)
            .putExtra(KEY_PINNED_ONLY, pinnedOnly)
            .putExtra(KEY_HIDE_EMPTY, hideEmpty)
        if (!sourceTypes.isNullOrEmpty()) {
            intent.putExtra(KEY_SOURCE_TYPES, com.mangaverse.app.search.domain.sourceTypesToNames(sourceTypes))
        }
        if (!contentKinds.isNullOrEmpty()) {
            intent.putExtra(KEY_CONTENT_KINDS, com.mangaverse.app.search.domain.searchContentKindsToNames(contentKinds))
        }
        startActivity(intent)
    }

    fun openSearch(source: ContentSource, query: String) = openList(source, ContentListFilter(query = query), null)

    fun openSourcePresets() {
        startActivity(SourcePresetListActivity::class.java)
    }

    fun openDetails(manga: Content, anchor: View? = null) {
        val context = contextOrNull() ?: return
        val intent = detailsIntent(context, DetailsOrigin.LocalMangaContent(ParcelableContent(manga)))
        startActivity(intent, null)
    }

    fun openResolvedDetails(
        manga: Content,
        anchor: View? = null,
        sharedElementKey: String? = null,
    ) {
        val lifecycleOwner = getLifecycleOwner() ?: return
        lifecycleOwner.lifecycleScope.launch {
            when (val origin = resolveDetailsOriginForContent(manga)) {
                is DetailsOrigin.EntityGraph -> {
                    openEntityDetails(
                        entityId = origin.entityId,
                        initialProjectionLocalMangaId = origin.initialProjectionLocalMangaId ?: manga.id,
                        sharedElementKey = sharedElementKey,
                    )
                }
                is DetailsOrigin.LocalMangaContent -> openDetails(manga, anchor)
                is DetailsOrigin.LocalMangaId -> openDetails(origin.mangaId)
                else -> openDetails(manga, anchor)
            }
        }
    }

    fun openTemporaryDetails(manga: Content) {
        val context = contextOrNull() ?: return
        val intent = detailsIntent(context, DetailsOrigin.LocalMangaContent(ParcelableContent(manga)))
            .putExtra(KEY_TEMPORARY_DETAILS, true)
        startActivity(intent, null)
    }

    fun openDetails(mangaId: Long) {
        startActivity(detailsIntent(contextOrNull() ?: return, DetailsOrigin.LocalMangaId(mangaId)))
    }

    fun openDetails(link: Uri) {
        startActivity(
            Intent(contextOrNull() ?: return, DetailsActivity::class.java)
                .setData(link),
        )
    }

    fun openEntityDetails(
        entityId: Long,
        preferredLocalMangaId: Long? = null,
        initialProjectionLocalMangaId: Long? = null,
        remoteId: Long? = null,
        url: String? = null,
        sharedElementKey: String? = null,
    ) {
        val origin = DetailsOrigin.EntityGraph(
            entityId = entityId,
            preferredLocalMangaId = preferredLocalMangaId,
            initialProjectionLocalMangaId = initialProjectionLocalMangaId,
            serviceId = null,
            remoteId = remoteId,
            url = url,
        )
        PendingDetailsNavigation.set(origin, sharedElementKey)
        startActivity(
            detailsIntent(
                contextOrNull() ?: return,
                origin,
            ),
        )
    }

    fun openReader(
		manga: Content,
		anchor: View? = null,
		contentTypeOverride: ContentType? = null,
		state: ReaderState? = null,
	) {
		openReader(
			ReaderIntent.Builder(contextOrNull() ?: return)
				.manga(manga)
				.state(state)
				.build(),
			anchor,
		)
	}

	fun openReader(intent: ReaderIntent, anchor: View? = null) {
		val activityIntent = intent.intent
		// Intercept video sources when ReaderIntent carries a Content extra and route accordingly
		runCatching {
			val parcelable = activityIntent.getParcelableExtraCompat<ParcelableContent>(KEY_MANGA)
			val manga = parcelable?.manga ?: run {
				val contentIntent = ContentIntent(activityIntent)
				val mangaId = contentIntent.mangaId
				if (mangaId != ContentIntent.ID_NONE) {
					kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
						contentDataRepository.findDisplayContentById(mangaId, withChapters = false)
							?: contentDataRepository.findContentById(mangaId, withChapters = false)
					}
				} else null
			}
			if (manga != null) {
                // 对于漫画内容，直接使用历史记录中的状态继续阅读
                val state = activityIntent.getParcelableExtraCompat<ReaderState>(ReaderIntent.EXTRA_STATE)
                val readerIntent = Intent(contextOrNull() ?: return, ReaderActivity::class.java)
                    .putExtra(KEY_MANGA, ParcelableContent(manga))
                    .putExtra(KEY_ID, manga.id)
                if (state != null) {
                    readerIntent.putExtra(ReaderIntent.EXTRA_STATE, state)
                }
                startActivity(
                    prepareImmersiveIntent(readerIntent),
                    anchor?.let { scaleUpActivityOptionsOf(it) },
                )
                return
            }
        }.getOrElse { /* ignore and fallback to reader */ }
        if (settings.isReaderMultiTaskEnabled && activityIntent.data != null) {
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        }
        startActivity(
            prepareImmersiveIntent(activityIntent),
            anchor?.let { view -> scaleUpActivityOptionsOf(view) },
        )
    }

    fun openAlternatives(manga: Content) {
        val composeActivity = activity as? BaseComposeActivity
        if (composeActivity != null) {
            composeActivity.showComposeModal {
                AlternativesSheetRoute(
                    manga = manga,
                    onOpenDetails = {
                        composeActivity.dismissComposeModal()
                        openDetails(it)
                    },
                    onOpenSourceSearch = { source, query ->
                        openSearch(source, query)
                    },
                    onDismissRequest = composeActivity::dismissComposeModal,
                )
            }
            return
        }
    }

    fun openRelated(manga: Content) {
        startActivity(
            Intent(contextOrNull(), RelatedContentActivity::class.java)
                .putExtra(KEY_MANGA, ParcelableContent(manga))
                .putExtra(KEY_ID, manga.id),
        )
    }

    fun openImage(url: String, source: ContentSource?, anchor: View? = null, preview: CoilMemoryCacheKey? = null) {
        startActivity(
            Intent(contextOrNull(), ImageActivity::class.java)
                .setData(Uri.parse(url))
                .putExtra(KEY_SOURCE, source?.name)
                .putExtra(KEY_PREVIEW, preview),
            anchor?.let { scaleUpActivityOptionsOf(it) },
        )
    }

    fun openSourcesCatalog() = startActivity(SourcesCatalogActivity::class.java)

    fun openDownloads() = startActivity(DownloadsActivity::class.java)

    fun openDirectoriesSettings() = startActivity(ContentDirectoriesActivity::class.java)

    fun openBrowser(url: String, source: ContentSource?, title: String?) {
        startActivity(browserIntent(contextOrNull() ?: return, url, source, title))
    }

    fun openBrowser(manga: Content) = openBrowser(
        url = manga.publicUrl,
        source = manga.source,
        title = manga.title,
    )

    fun openColorFilterConfig(manga: Content, page: ContentPage) {
        startActivity(
            Intent(contextOrNull(), ColorFilterConfigActivity::class.java)
                .putExtra(KEY_MANGA, ParcelableContent(manga))
                .putExtra(KEY_PAGES, ParcelableContentPage(page)),
        )
    }

    fun openHistory(groupTab: BrowseGroupTab? = null) {
        startActivity(historyIntent(contextOrNull() ?: return, groupTab))
    }

    fun openFavorites() = startActivity(FavouritesActivity::class.java)

    fun openFavorites(category: FavouriteCategory) {
        startActivity(
            Intent(contextOrNull() ?: return, FavouritesActivity::class.java)
                .putExtra(KEY_ID, category.id)
                .putExtra(KEY_TITLE, category.title),
        )
    }

    fun openFavoriteCategories() = startActivity(FavouriteCategoriesActivity::class.java)

    fun openFavoriteCategoryEdit(categoryId: Long) {
        startActivity(
            Intent(contextOrNull() ?: return, FavouritesCategoryEditActivity::class.java)
                .putExtra(KEY_ID, categoryId),
        )
    }

    fun openFavoriteCategoryCreate() = openFavoriteCategoryEdit(FavouritesCategoryEditActivity.NO_ID)

    fun openContentOverrideConfig(manga: Content) {
        val intent = overrideEditIntent(contextOrNull() ?: return, manga)
        startActivity(intent)
    }

    fun openSettings() {
        val hostActivity = activity
        startActivity(
            Intent(contextOrNull() ?: return, SettingsActivity::class.java)
                .putExtra(SettingsActivity.EXTRA_USE_HORIZONTAL_ROUTE_TRANSITION, true),
        )
        hostActivity?.applyHorizontalRouteOpenTransition()
    }

    fun openEntityOrganizeSettings(selectedContentIds: Set<Long> = emptySet()) {
        val hostActivity = activity
        startActivity(
            SettingsActivity.newEntityOrganizeIntent(
                context = contextOrNull() ?: return,
                selectedContentIds = selectedContentIds,
            ),
            hostActivity?.let(::activityTransitionOptionsOf),
        )
    }

    fun openTranslationSettings() {
        val hostActivity = activity
        startActivity(
            translationSettingsIntent(contextOrNull() ?: return),
            hostActivity?.let(::activityTransitionOptionsOf),
        )
    }

    fun openReaderSettings() {
        val hostActivity = activity
        startActivity(
            readerSettingsIntent(contextOrNull() ?: return),
            hostActivity?.let(::activityTransitionOptionsOf),
        )
    }

    fun openProxySettings() {
        val hostActivity = activity
        startActivity(
            proxySettingsIntent(contextOrNull() ?: return),
            hostActivity?.let(::activityTransitionOptionsOf),
        )
    }

    fun openDownloadsSetting() {
        val hostActivity = activity
        startActivity(
            downloadsSettingsIntent(contextOrNull() ?: return),
            hostActivity?.let(::activityTransitionOptionsOf),
        )
    }

    fun openSourceSettings(source: ContentSource) {
        startActivity(sourceSettingsIntent(contextOrNull() ?: return, source))
    }

    fun openSuggestionsSettings() {
        val hostActivity = activity
        startActivity(
            suggestionsSettingsIntent(contextOrNull() ?: return),
            hostActivity?.let(::activityTransitionOptionsOf),
        )
    }

    fun openSourcesSettings() {
        val hostActivity = activity
        startActivity(
            sourcesSettingsIntent(contextOrNull() ?: return),
            hostActivity?.let(::activityTransitionOptionsOf),
        )
    }

    fun openGlobalTagBlacklist() {
        startActivity(GlobalTagBlacklistActivity.newIntent(contextOrNull() ?: return))
    }

    fun openReaderTapGridSettings() = startActivity(ReaderTapGridConfigActivity::class.java)

    fun openSourceAuth(source: ContentSource) {
        startActivity(sourceAuthIntent(contextOrNull() ?: return, source))
    }

    fun openManageSources() {
        startActivity(
            manageSourcesIntent(contextOrNull() ?: return),
        )
    }

    @CheckResult
    fun openExternalBrowser(url: String, chooserTitle: CharSequence? = null): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = url.toUriOrNull() ?: return false
        return startActivitySafe(
            if (!chooserTitle.isNullOrEmpty()) {
                Intent.createChooser(intent, chooserTitle)
            } else {
                intent
            },
        )
    }

    /** Dialogs **/

    fun showDownloadDialog(manga: Content, snackbarHost: View? = null) = showDownloadDialog(setOf(manga), snackbarHost)

    fun showDownloadDialog(manga: Collection<Content>, snackbarHost: View? = null) {
        if (manga.isEmpty()) {
            return
        }
        val composeActivity = activity as? BaseComposeActivity
        if (composeActivity != null) {
            val mangaList = manga.toList()
            composeActivity.showComposeModal {
                DownloadDialog(
                    mangaList = mangaList,
                    snackbarHostState = composeActivity.snackbarHostState,
                    onOpenDownloads = ::openDownloads,
                    onDismiss = composeActivity::dismissComposeModal,
                )
            }
            return
        }
    }

    fun showLocalInfoDialog(manga: Content) {
        val composeActivity = activity as? BaseComposeActivity
        if (composeActivity != null) {
            composeActivity.showComposeModal {
                LocalInfoDialogRoute(
                    manga = manga,
                    onDismissRequest = composeActivity::dismissComposeModal,
                )
            }
            return
        }
    }

    fun showDirectorySelectDialog(contentType: String = ContentDirectorySelectViewModel.CONTENT_TYPE_MANGA) {
        val composeActivity = activity as? BaseComposeActivity ?: return
        composeActivity.showComposeModal {
            ContentDirectorySelectRoute(
                contentType = contentType,
                onDismiss = composeActivity::dismissComposeModal,
                onError = { error ->
                    composeActivity.lifecycleScope.launch {
                        composeActivity.snackbarHostState.showSnackbar(error.getDisplayMessage(composeActivity.resources))
                    }
                },
            )
        }
    }

    fun showFavoriteDialog(manga: Content) = showFavoriteDialog(setOf(manga))

    fun showFavoriteDialog(manga: Collection<Content>) {
        if (manga.isEmpty()) {
            return
        }
        val composeActivity = activity as? BaseComposeActivity
        if (composeActivity != null) {
            val mangaList = manga.toList()
            composeActivity.showComposeModal {
                FavoriteCategoryDialogRoute(
                    manga = mangaList,
                    onManageCategories = ::openFavoriteCategories,
                    onDismiss = composeActivity::dismissComposeModal,
                )
            }
            return
        }
    }

    fun showTagDialog(tag: ContentTag) {
        val context = contextOrNull() ?: return
        val composeActivity = activity as? BaseComposeActivity
        if (composeActivity != null) {
            composeActivity.showComposeModal {
                AppRouterChoiceDialog(
                    icon = {
                        Icon(
                            painter = rememberSafePainter(R.drawable.ic_tag),
                            contentDescription = null,
                        )
                    },
                    title = tag.title,
                    options = listOf(
                        stringResource(R.string.search_on_s, tag.source.getTitle(composeActivity)),
                        stringResource(R.string.search_everywhere),
                    ),
                    dismissLabel = stringResource(R.string.close),
                    onDismissRequest = composeActivity::dismissComposeModal,
                    onOptionSelected = { which ->
                        composeActivity.dismissComposeModal()
                        when (which) {
                            0 -> openList(tag)
                            1 -> openSearch(tag.title, SearchKind.TAG)
                        }
                    },
                )
            }
            return
        }
        buildAlertDialog(context) {
            setIcon(R.drawable.ic_tag)
            setTitle(tag.title)
            setItems(
                arrayOf(
                    context.getString(R.string.search_on_s, tag.source.getTitle(context)),
                    context.getString(R.string.search_everywhere),
                ),
            ) { _, which ->
                when (which) {
                    0 -> openList(tag)
                    1 -> openSearch(tag.title, SearchKind.TAG)
                }
            }
            setNegativeButton(R.string.close, null)
            setCancelable(true)
        }.show()
    }

    fun showAuthorDialog(author: String, source: ContentSource) {
        val context = contextOrNull() ?: return
        val composeActivity = activity as? BaseComposeActivity
        if (composeActivity != null) {
            composeActivity.showComposeModal {
                AppRouterChoiceDialog(
                    icon = {
                        Icon(
                            painter = rememberSafePainter(R.drawable.ic_user),
                            contentDescription = null,
                        )
                    },
                    title = author,
                    options = listOf(
                        stringResource(R.string.search_on_s, source.getTitle(composeActivity)),
                        stringResource(R.string.search_everywhere),
                    ),
                    dismissLabel = stringResource(R.string.close),
                    onDismissRequest = composeActivity::dismissComposeModal,
                    onOptionSelected = { which ->
                        composeActivity.dismissComposeModal()
                        when (which) {
                            0 -> openList(source, ContentListFilter(author = author), null)
                            1 -> openSearch(author, SearchKind.AUTHOR)
                        }
                    },
                )
            }
            return
        }
        buildAlertDialog(context) {
            setIcon(R.drawable.ic_user)
            setTitle(author)
            setItems(
                arrayOf(
                    context.getString(R.string.search_on_s, source.getTitle(context)),
                    context.getString(R.string.search_everywhere),
                ),
            ) { _, which ->
                when (which) {
                    0 -> openList(source, ContentListFilter(author = author), null)
                    1 -> openSearch(author, SearchKind.AUTHOR)
                }
            }
            setNegativeButton(R.string.close, null)
            setCancelable(true)
        }.show()
    }

    fun showShareDialog(manga: Content) {
        if (manga.isBroken) {
            return
        }
        if (manga.isLocal) {
            manga.url.toUriOrNull()?.toFileOrNull()?.let {
                shareFile(it)
            }
            return
        }
        val context = contextOrNull() ?: return
        val composeActivity = activity as? BaseComposeActivity
        if (composeActivity != null) {
            composeActivity.showComposeModal {
                AppRouterChoiceDialog(
                    icon = {
                        Icon(
                            painter = rememberDrawablePainter(
                                composeActivity.getThemeDrawable(appcompatR.attr.actionModeShareDrawable),
                            ),
                            contentDescription = null,
                        )
                    },
                    title = stringResource(R.string.share),
                    options = listOf(
                        stringResource(R.string.link_to_manga_in_app),
                        stringResource(R.string.link_to_manga_on_s, manga.source.getTitle(composeActivity)),
                    ),
                    dismissLabel = stringResource(android.R.string.cancel),
                    onDismissRequest = composeActivity::dismissComposeModal,
                    onOptionSelected = { which ->
                        composeActivity.dismissComposeModal()
                        when (which) {
                            0 -> shareLink(manga.appUrl.toString(), manga.title)
                            1 -> shareLink(manga.publicUrl, manga.title)
                        }
                    },
                )
            }
            return
        }
        buildAlertDialog(context) {
            setIcon(context.getThemeDrawable(appcompatR.attr.actionModeShareDrawable))
            setTitle(R.string.share)
            setItems(
                arrayOf(
                    context.getString(R.string.link_to_manga_in_app),
                    context.getString(R.string.link_to_manga_on_s, manga.source.getTitle(context)),
                ),
            ) { _, which ->
                val link = when (which) {
                    0 -> manga.appUrl.toString()
                    1 -> manga.publicUrl
                    else -> return@setItems
                }
                shareLink(link, manga.title)
            }
            setNegativeButton(android.R.string.cancel, null)
            setCancelable(true)
        }.show()
    }

    fun showErrorDialog(error: Throwable, url: String? = null) {
        startActivitySafe(
            Intent(contextOrNull(), ErrorDetailsActivity::class.java)
                .putExtra(KEY_ERROR, error.toSerializableThrowable() as java.io.Serializable)
                .putExtra(KEY_URL, url),
        )
    }

	fun showBackupRestoreDialog(
		fileUri: Uri,
		restoreFormat: BackupRestoreFormat = BackupRestoreFormat.KOTOTORO_CURRENT,
	) {
        val composeActivity = (activity ?: fragment?.activity) as? BaseComposeActivity ?: return
        composeActivity.showComposeModal {
			RestoreDialogRoute(
				uri = fileUri,
				restoreFormat = restoreFormat,
                onRestoreStarted = {
                    closeWelcomeSheet()
                    composeActivity.dismissComposeModal()
                },
                onUnsupported = {
                    composeActivity.lifecycleScope.launch {
                        composeActivity.snackbarHostState.showSnackbar(
                            composeActivity.getString(R.string.operation_not_supported),
                        )
                    }
                },
                onDismiss = composeActivity::dismissComposeModal,
            )
        }
    }

    fun showImportDialog() {
        val composeActivity = activity as? BaseComposeActivity
        if (composeActivity != null) {
            composeActivity.showComposeModal {
                ImportDialog(
                    onDismissRequest = composeActivity::dismissComposeModal,
                )
            }
            return
        }
    }

    fun showFilterSheet(): Boolean {
        if (!isFilterSupported()) {
            return false
        }
        val composeActivity = activity as? BaseComposeActivity
        val filterOwner = activity as? FilterCoordinator.Owner
        if (composeActivity != null && filterOwner != null) {
            val modalKey = FILTER_SHEET_MODAL_KEY
            composeActivity.showComposeModal(key = modalKey) {
                FilterSheetRoute(
                    filter = filterOwner.filterCoordinator,
                    isEmbedded = false,
                    onDismiss = { composeActivity.dismissComposeModal(modalKey) },
                    onOpenTagCatalog = { groupTitle, excludeMode ->
                        showTagsCatalogSheet(excludeMode = excludeMode, groupTitle = groupTitle)
                    },
                )
            }
            return true
        }
        return false
    }

    fun showTagsCatalogSheet(excludeMode: Boolean, groupTitle: String? = null) {
        if (!isFilterSupported()) {
            return
        }
        val composeActivity = activity as? BaseComposeActivity
        val filterOwner = activity as? FilterCoordinator.Owner
        if (composeActivity != null && filterOwner != null) {
            val modalKey = buildTagsCatalogModalKey(excludeMode = excludeMode, groupTitle = groupTitle)
            composeActivity.showComposeModal(key = modalKey) {
                TagsCatalogRoute(
                    filter = filterOwner.filterCoordinator,
                    isExcludeTag = excludeMode,
                    groupTitle = groupTitle,
                    onDismiss = { composeActivity.dismissComposeModal(modalKey) },
                )
            }
            return
        }
    }

    fun showListConfigSheet(section: ListConfigSection) {
        val composeActivity = activity as? BaseComposeActivity
        if (composeActivity != null) {
            composeActivity.showComposeModal {
                ListConfigRoute(
                    section = section,
                    onDismissRequest = composeActivity::dismissComposeModal,
                )
            }
            return
        }
    }

    fun showStatisticSheet(manga: Content) {
        val composeActivity = activity as? BaseComposeActivity
        if (composeActivity != null) {
            composeActivity.showComposeModal {
                ContentStatsRoute(
                    manga = manga,
                    onOpenDetails = {
                        composeActivity.dismissComposeModal()
                        openDetails(manga)
                    },
                    onDismissRequest = composeActivity::dismissComposeModal,
                )
            }
            return
        }
    }

    fun showWelcomeSheet() {
        val composeActivity = (activity ?: fragment?.activity) as? BaseComposeActivity ?: return
        composeActivity.dismissComposeModal(WELCOME_MODAL_KEY)
        composeActivity.showComposeModal(key = WELCOME_MODAL_KEY) {
            WelcomeRoute(
                // 登录成功或用户跳过前置步骤后关闭向导。
                // 注意：若用户仍未登录，MainActivity 的 session 监听会立即重新打开，
                // 因此这里可以安全地允许关闭，强制登录由会话监听兜底。
                onDismissRequest = { composeActivity.dismissComposeModal(WELCOME_MODAL_KEY) },
                onRestoreBackup = { uri ->
                    showBackupRestoreDialog(uri)
                },
                onOpenDocumentUnsupported = {
                    composeActivity.lifecycleScope.launch {
                        composeActivity.snackbarHostState.showSnackbar(
                            composeActivity.getString(R.string.operation_not_supported),
                        )
                    }
                },
            )
        }
    }

    fun showTrackerCategoriesConfigSheet() {
        val composeActivity = (activity ?: fragment?.activity) as? BaseComposeActivity ?: return
        composeActivity.showComposeModal {
            TrackerCategoriesConfigRoute(
                onDismissRequest = composeActivity::dismissComposeModal,
            )
        }
    }

    fun askForDownloadOverMeteredNetwork(onConfirmed: (allow: Boolean) -> Unit) {
        val context = contextOrNull() ?: return
        when (settings.allowDownloadOnMeteredNetwork) {
            TriStateOption.ENABLED -> onConfirmed(true)
            TriStateOption.DISABLED -> onConfirmed(false)
            TriStateOption.ASK -> {
                if (!context.connectivityManager.isActiveNetworkMetered) {
                    onConfirmed(true)
                    return
                }
                val listener = DialogInterface.OnClickListener { _, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            settings.allowDownloadOnMeteredNetwork = TriStateOption.ENABLED
                            onConfirmed(true)
                        }

                        DialogInterface.BUTTON_NEUTRAL -> {
                            onConfirmed(true)
                        }

                        DialogInterface.BUTTON_NEGATIVE -> {
                            settings.allowDownloadOnMeteredNetwork = TriStateOption.DISABLED
                            onConfirmed(false)
                        }
                    }
                }
                BigButtonsAlertDialog.Builder(context)
                    .setIcon(R.drawable.ic_network_cellular)
                    .setTitle(R.string.download_cellular_confirm)
                    .setPositiveButton(R.string.allow_always, listener)
                    .setNeutralButton(R.string.allow_once, listener)
                    .setNegativeButton(R.string.dont_allow, listener)
                    .create()
                    .show()
            }
        }
    }

    /** Public utils **/

    fun isFilterSupported(): Boolean = when {
        fragment != null -> FilterCoordinator.find(fragment) != null
        activity != null -> activity is FilterCoordinator.Owner
        else -> false
    }

    fun closeWelcomeSheet(): Boolean {
        val composeActivity = (activity ?: fragment?.activity) as? BaseComposeActivity ?: return false
        composeActivity.dismissComposeModal(WELCOME_MODAL_KEY)
        return true
    }

    private suspend fun resolveDetailsOriginForContent(content: Content): DetailsOrigin {
        return withContext(Dispatchers.IO) {
            val entityId = workResolver.resolveByMangaId(content.id).entityId
            val canResolveProjection = entityId != null &&
                contentDataRepository.findContentById(content.id, withChapters = false) != null
            if (entityId != null && canResolveProjection) {
                DetailsOrigin.EntityGraph(
                    entityId = entityId,
                    initialProjectionLocalMangaId = content.id,
                )
            } else {
                DetailsOrigin.LocalMangaContent(ParcelableContent(content))
            }
        }
    }

    /** Private utils **/

    private fun startActivity(intent: Intent, options: Bundle? = null) {
        fragment?.also {
            if (it.isAdded) {
                it.startActivity(intent, options)
            }
        } ?: activity?.startActivity(intent, options)
    }

    private fun startActivitySafe(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private fun startActivity(activityClass: Class<out Activity>) {
        startActivity(Intent(contextOrNull() ?: return, activityClass))
    }

    private fun getFragmentManager(): FragmentManager? = runCatching {
        fragment?.childFragmentManager ?: activity?.supportFragmentManager
    }.onFailure { exception ->
        exception.printStackTraceDebug()
    }.getOrNull()

    fun shareLink(link: String, title: String) {
        val context = contextOrNull() ?: return
        ShareCompat.IntentBuilder(context)
            .setText(link)
            .setType(TYPE_TEXT)
            .setChooserTitle(context.getString(R.string.share_s, title.ellipsize(12)))
            .startChooser()
    }

    private fun shareFile(file: File) { // TODO directory sharing support
        val context = contextOrNull() ?: return
        val intentBuilder = ShareCompat.IntentBuilder(context)
            .setType(TYPE_CBZ)
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)
        intentBuilder.addStream(uri)
        intentBuilder.setChooserTitle(context.getString(R.string.share_s, file.name))
        intentBuilder.startChooser()
    }

    @UiContext
    private fun contextOrNull(): Context? = activity ?: fragment?.context

    private fun getLifecycleOwner(): LifecycleOwner? = activity ?: fragment?.viewLifecycleOwner

    private fun DialogFragment.showDistinct(): Boolean {
        val fm = this@AppRouter.getFragmentManager() ?: return false
        val tag = javaClass.fragmentTag()
        val existing = fm.findFragmentByTag(tag) as? DialogFragment?
        if (existing != null && existing.isVisible && existing.arguments == this.arguments) {
            return false
        }
        show(fm, tag)
        return true
    }

    private fun DialogFragment.show() {
        show(
            this@AppRouter.getFragmentManager() ?: return,
            javaClass.fragmentTag(),
        )
    }

    private fun Fragment.findFragmentByTagRecursive(fragmentTag: String): Fragment? {
        childFragmentManager.findFragmentByTag(fragmentTag)?.let {
            return it
        }
        val parent = parentFragment
        return if (parent != null) {
            parent.findFragmentByTagRecursive(fragmentTag)
        } else {
            parentFragmentManager.findFragmentByTag(fragmentTag)
        }
    }

    companion object {
        private const val WELCOME_MODAL_KEY = "welcome-sheet-modal"
        private const val FILTER_SHEET_MODAL_KEY = "filter-sheet-modal"

        fun from(view: View): AppRouter? = runCatching {
            AppRouter(view.findFragment())
        }.getOrElse {
            (view.context.findActivity() as? FragmentActivity)?.let(::AppRouter)
        }

        fun detailsIntent(context: Context, origin: DetailsOrigin) = Intent(context, DetailsActivity::class.java)
            .putExtra(KEY_DETAILS_ORIGIN, origin)

        fun detailsIntent(context: Context, content: com.mangaverse.app.parsers.model.Content) = detailsIntent(context, com.mangaverse.app.details.ui.model.DetailsOrigin.LocalMangaContent(com.mangaverse.app.core.model.parcelable.ParcelableContent(content)))

        fun listIntent(context: Context, source: ContentSource, filter: ContentListFilter?, sortOrder: SortOrder?): Intent =
            Intent(context, ContentListActivity::class.java)
                .setAction(ACTION_MANGA_EXPLORE)
                .putExtra(KEY_SOURCE, source.name)
                .apply {
                    if (!filter.isNullOrEmpty()) {
                        putExtra(KEY_FILTER, ParcelableContentListFilter(filter))
                    }
                    if (sortOrder != null) {
                        putExtra(KEY_SORT_ORDER, sortOrder)
                    }
                }

        fun cloudFlareResolveIntent(
            context: Context,
            exception: CloudFlareProtectedException,
            hidden: Boolean = false,
        ): Intent =
            Intent(context, CloudFlareActivity::class.java).apply {
                data = Uri.parse(exception.url)
                putExtra(KEY_SOURCE, exception.source.name)
                putExtra(CloudFlareActivity.EXTRA_METHOD, exception.method)
                exception.body?.let { putExtra(CloudFlareActivity.EXTRA_BODY, it) }
				exception.contentType?.let { putExtra(CloudFlareActivity.EXTRA_CONTENT_TYPE, it) }
                putExtra(CloudFlareActivity.EXTRA_HIDDEN, hidden)
                exception.headers[CommonHeaders.USER_AGENT]?.let {
                    putExtra(KEY_USER_AGENT, it)
                }
            }

        fun browserIntent(
            context: Context,
            url: String,
            source: ContentSource?,
            title: String?
        ): Intent = Intent(context, BrowserActivity::class.java)
            .setData(Uri.parse(url))
            .putExtra(KEY_TITLE, title)
            .putExtra(KEY_SOURCE, source?.name)

        fun homeIntent(context: Context) = Intent(context, MainActivity::class.java)

        fun historyIntent(context: Context, groupTab: BrowseGroupTab? = null) =
            Intent(context, HistoryActivity::class.java).apply {
                if (groupTab != null) {
                    putExtra(KEY_GROUP_TAB, groupTab.id)
                }
            }

        fun readerSettingsIntent(context: Context) =
            Intent(context, SettingsActivity::class.java)
                .setAction(ACTION_READER)

        fun translationSettingsIntent(context: Context) =
            Intent(context, SettingsActivity::class.java)
                .setAction(ACTION_TRANSLATION)

        fun suggestionsSettingsIntent(context: Context) =
            Intent(context, SettingsActivity::class.java)
                .setAction(ACTION_SUGGESTIONS)

        fun entityOrganizeSettingsIntent(
            context: Context,
            selectedContentIds: Set<Long> = emptySet(),
        ) = SettingsActivity.newEntityOrganizeIntent(
            context = context,
            selectedContentIds = selectedContentIds,
        )

        fun trackerSettingsIntent(context: Context) =
            Intent(context, SettingsActivity::class.java)
                .setAction(ACTION_TRACKER)

        fun periodicBackupSettingsIntent(context: Context) =
            Intent(context, SettingsActivity::class.java)
                .setAction(ACTION_PERIODIC_BACKUP)

        fun proxySettingsIntent(context: Context) =
            Intent(context, SettingsActivity::class.java)
                .setAction(ACTION_PROXY)

        fun historySettingsIntent(context: Context) =
            Intent(context, SettingsActivity::class.java)
                .setAction(ACTION_HISTORY)

        fun sourcesSettingsIntent(context: Context) =
            Intent(context, SettingsActivity::class.java)
                .setAction(ACTION_SOURCES)

        fun manageSourcesIntent(context: Context) =
            SettingsActivity.newUnifiedSourcesIntent(context)

        fun downloadsSettingsIntent(context: Context) =
            Intent(context, SettingsActivity::class.java)
                .setAction(ACTION_MANAGE_DOWNLOADS)

        fun sourceSettingsIntent(context: Context, source: ContentSource): Intent = when (source) {
            is ContentSourceInfo -> sourceSettingsIntent(context, source.mangaSource)
            is ExternalContentSource -> {
                val kind = inferUnifiedSourceKind(source.packageName)
                SettingsActivity.newUnifiedSourcesIntent(context, initialRepositoryKind = kind)
            }

            else -> Intent(context, SettingsActivity::class.java)
                .setAction(ACTION_SOURCE)
                .putExtra(KEY_SOURCE, source.name)
        }

        private fun inferUnifiedSourceKind(packageName: String): UnifiedSourceKind? {
            return when {
                packageName.startsWith("eu.kanade.tachiyomi") -> UnifiedSourceKind.MIHON
                else -> null
            }
        }

        fun sourceAuthIntent(context: Context, source: ContentSource): Intent {
            return Intent(context, SourceAuthActivity::class.java)
                .putExtra(KEY_SOURCE, source.name)
        }

        fun overrideEditIntent(context: Context, manga: Content): Intent =
            Intent(context, OverrideConfigActivity::class.java)
                .putExtra(KEY_MANGA, ParcelableContent(manga, withDescription = false))

        fun isShareSupported(manga: Content): Boolean = when {
            manga.isBroken -> false
            manga.isLocal -> manga.url.toUriOrNull()?.toFileOrNull() != null
            else -> true
        }

        fun shortContentUrl(mangaId: Long): Uri = Uri.Builder()
            .scheme("kototoro")
            .path("manga")
            .appendQueryParameter("id", mangaId.toString())
            .build()

        fun searchIntent(
            context: Context,
            query: String,
            kind: SearchKind = SearchKind.SIMPLE,
            sourceTypes: Set<com.mangaverse.app.core.jsonsource.SourceType>? = null,
            contentKinds: Set<SearchContentKind>? = null,
            pickMode: Boolean = false,
            advancedTitle: String? = null,
            advancedTags: String? = null,
            advancedAuthor: String? = null,
            pinnedOnly: Boolean = false,
            hideEmpty: Boolean = false,
        ): Intent {
            val intent = Intent(context, SearchActivity::class.java)
                .putExtra(KEY_QUERY, query)
                .putExtra(KEY_KIND, kind)
                .putExtra(KEY_PICK_MODE, pickMode)
                .putExtra(KEY_ADVANCED_TITLE, advancedTitle)
                .putExtra(KEY_ADVANCED_TAGS, advancedTags)
                .putExtra(KEY_ADVANCED_AUTHOR, advancedAuthor)
                .putExtra(KEY_PINNED_ONLY, pinnedOnly)
                .putExtra(KEY_HIDE_EMPTY, hideEmpty)
            if (!sourceTypes.isNullOrEmpty()) {
                intent.putExtra(KEY_SOURCE_TYPES, com.mangaverse.app.search.domain.sourceTypesToNames(sourceTypes))
            }
            if (!contentKinds.isNullOrEmpty()) {
                intent.putExtra(KEY_CONTENT_KINDS, com.mangaverse.app.search.domain.searchContentKindsToNames(contentKinds))
            }
            return intent
        }

        const val KEY_DATA = "data"
        const val KEY_ENTITY_ID = "entity_id"
        const val KEY_ENTRIES = "entries"
        const val KEY_ERROR = "error"
        const val KEY_EXCLUDE = "exclude"
        const val KEY_FILE = "file"
        const val KEY_FILTER = "filter"
        const val KEY_FORCE_LOAD = "force_load"
        const val KEY_ID = "id"
        const val KEY_INDEX = "index"
        const val KEY_IS_BOTTOMTAB = "is_btab"
        const val KEY_KIND = "kind"
        const val KEY_LIST_SECTION = "list_section"
        const val KEY_DETAILS_ORIGIN = "details_origin"
        internal const val EXTRA_HAS_IN_APP_CALLER =
            "com.mangaverse.app.extra.HAS_IN_APP_CALLER"
        const val KEY_MANGA = "manga"
        const val KEY_MANGA_LIST = "manga_list"
        const val KEY_TEMPORARY_DETAILS = "temporary_details"
        const val KEY_PAGES = "pages"
        const val KEY_PREVIEW = "preview"
        const val KEY_PICK_MODE = "pick_mode"
        const val KEY_PINNED_ONLY = "pinned_only"
        const val KEY_QUERY = "query"
        const val KEY_ADVANCED_TITLE = "advanced_title"
        const val KEY_ADVANCED_TAGS = "advanced_tags"
        const val KEY_ADVANCED_AUTHOR = "advanced_author"
        const val KEY_REMOTE_ID = "remote_id"
        const val KEY_READER_MODE = "reader_mode"
        const val KEY_SORT_ORDER = "sort_order"
        const val KEY_SOURCE = "source"
        const val KEY_SOURCE_TYPES = "source_types"
        const val KEY_CONTENT_KINDS = "content_kinds"
        const val KEY_HIDE_EMPTY = "hide_empty"
        const val KEY_GROUP_TAB = "group_tab"
        const val KEY_GROUP_TITLE = "group_title"
        const val KEY_TAB = "tab"
        const val KEY_TITLE = "title"
        const val KEY_URL = "url"
        const val KEY_USER_AGENT = "user_agent"
        const val KEY_SUCCESS_COOKIE_NAME = "success_cookie_name"
        const val KEY_SUCCESS_COOKIE_URL = "success_cookie_url"
        const val KEY_BROWSER_WAIT_TOKEN = "browser_wait_token"
        const val KEY_BROWSER_HTML = "browser_html"
        const val KEY_BROWSER_REFETCH_AFTER_SUCCESS = "browser_refetch_after_success"

        val ACTION_HISTORY = "${BuildConfig.APPLICATION_ID}.action.MANAGE_HISTORY"
        val ACTION_MANAGE_DOWNLOADS = "${BuildConfig.APPLICATION_ID}.action.MANAGE_DOWNLOADS"
        val ACTION_MANAGE_SOURCES = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCES_LIST"
        val ACTION_ENTITY_ORGANIZE = "${BuildConfig.APPLICATION_ID}.action.ENTITY_ORGANIZE"
        val ACTION_MANGA_EXPLORE = "${BuildConfig.APPLICATION_ID}.action.EXPLORE_MANGA"
        val ACTION_PROXY = "${BuildConfig.APPLICATION_ID}.action.MANAGE_PROXY"
        val ACTION_READER = "${BuildConfig.APPLICATION_ID}.action.MANAGE_READER_SETTINGS"
        val ACTION_SOURCE = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCE_SETTINGS"
        val ACTION_SOURCES = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SOURCES"
        val ACTION_SUGGESTIONS = "${BuildConfig.APPLICATION_ID}.action.MANAGE_SUGGESTIONS"
        val ACTION_TRACKER = "${BuildConfig.APPLICATION_ID}.action.MANAGE_TRACKER"
        val ACTION_TRANSLATION = "${BuildConfig.APPLICATION_ID}.action.MANAGE_TRANSLATION"
        val ACTION_PERIODIC_BACKUP = "${BuildConfig.APPLICATION_ID}.action.MANAGE_PERIODIC_BACKUP"

        private const val TYPE_TEXT = "text/plain"
        private const val TYPE_IMAGE = "image/*"
        private const val TYPE_CBZ = "application/x-cbz"

        private fun buildTagsCatalogModalKey(excludeMode: Boolean, groupTitle: String?): String {
            return buildString {
                append("tags-catalog-modal:")
                append(excludeMode)
                append(':')
                append(groupTitle.orEmpty())
            }
        }

        private fun Class<out Fragment>.fragmentTag() = name // TODO

        private inline fun <reified F : Fragment> fragmentTag() = F::class.java.fragmentTag()
    }
}

@Suppress("UNUSED_PARAMETER")
internal fun immersiveTaskFlags(enabled: Boolean): Int = 0
