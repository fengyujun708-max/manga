package com.mangaverse.app.details.ui.compose

import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mangaverse.app.R
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.os.AppShortcutManager
import com.mangaverse.app.details.ui.DetailsViewModel
import com.mangaverse.app.details.ui.openDetailsReader
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.SortOrder
import com.mangaverse.app.search.domain.SearchKind

internal fun handleDetailsAction(
    action: DetailsAction,
    appRouter: AppRouter,
    viewModel: DetailsViewModel,
    appShortcutManager: AppShortcutManager,
    coroutineScope: CoroutineScope,
    snackbarHost: View,
    overrideEditLauncher: ActivityResultLauncher<Intent>,
    onOpenSourceList: ((source: ContentSource, filter: ContentListFilter?, sortOrder: SortOrder?) -> Unit)? = null,
    onFinish: () -> Unit,
) {
    when (action) {
        DetailsAction.OpenCover -> {
            viewModel.getContentOrNull()?.let { content ->
                content.coverUrl?.let { url ->
                    appRouter.openImage(url = url, source = content.source, anchor = null)
                }
            }
        }

        DetailsAction.Resume -> openDetailsReader(
            context = snackbarHost.context,
            viewModel = viewModel,
            router = appRouter,
            isIncognitoMode = false,
            snackbarHost = snackbarHost,
        )

        DetailsAction.ResumeIncognito -> openDetailsReader(
            context = snackbarHost.context,
            viewModel = viewModel,
            router = appRouter,
            isIncognitoMode = true,
            snackbarHost = snackbarHost,
        )

        DetailsAction.ManageDownloads -> appRouter.openDownloads()
        is DetailsAction.OpenContent -> appRouter.openDetails(action.content)
        DetailsAction.Favorite -> viewModel.getContentOrNull()?.let(appRouter::showFavoriteDialog)
        DetailsAction.Share -> viewModel.getContentOrNull()?.let(appRouter::showShareDialog)
        DetailsAction.ForgetHistory -> viewModel.removeFromHistory()
        DetailsAction.ManageCategories -> appRouter.openFavoriteCategories()
        is DetailsAction.OpenSource -> onOpenSourceList?.invoke(action.source, null, null)
            ?: appRouter.openList(action.source, null, null)
        is DetailsAction.SearchAuthorOnSource -> appRouter.openSearch(action.source, action.author)
        is DetailsAction.SearchAuthorEverywhere -> appRouter.openSearch(action.author, SearchKind.AUTHOR)
        is DetailsAction.SearchTagOnSource -> appRouter.openSearch(action.tag.source, action.tag.title)
        is DetailsAction.SearchTagEverywhere -> appRouter.openSearch(action.tagTitle, SearchKind.TAG)
        is DetailsAction.OpenWebUrl -> appRouter.openBrowser(action.url, null, null)
        is DetailsAction.SelectBranch -> viewModel.setSelectedBranch(action.branch)
        is DetailsAction.ShareLink -> appRouter.shareLink(action.link, action.title)

        DetailsAction.Translate -> {
            val hasCache = viewModel.hasTranslationCache.value
            viewModel.translateTitleAndDescription(forceRefresh = hasCache)
            com.google.android.material.snackbar.Snackbar.make(
                snackbarHost,
                if (hasCache) R.string.reader_translation_retranslate_started else R.string.translating,
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT,
            ).show()
        }

        DetailsAction.ToggleTranslation -> viewModel.toggleTranslationDisplay()
        DetailsAction.FindSimilar -> viewModel.getContentOrNull()?.let { appRouter.openSearch(it.title) }
        DetailsAction.OpenAlternatives -> viewModel.getContentOrNull()?.let(appRouter::openAlternatives)
        DetailsAction.OpenOnlineVariant -> viewModel.remoteContent.value?.let(appRouter::openDetails)
        is DetailsAction.OpenBrowserPage -> appRouter.openBrowser(action.url, action.source, action.title)
        DetailsAction.OpenMetadataInBrowser, DetailsAction.OpenLocalSourceInBrowser -> Unit

        DetailsAction.Download -> Unit
        DetailsAction.OpenStatistics -> Unit
        DetailsAction.OpenReadingRecord -> Unit

        DetailsAction.ToggleList,
        DetailsAction.ToggleGrid,
        DetailsAction.ToggleBookmarkView,
        -> Unit

        DetailsAction.ToggleSafe -> viewModel.toggleMarkSafe()
        DetailsAction.DeleteLocal -> {
            viewModel.deleteLocal()
        }

        DetailsAction.EditOverride -> {
            viewModel.getContentOrNull()?.let {
                overrideEditLauncher.launch(AppRouter.overrideEditIntent(snackbarHost.context, it))
            }
        }

        DetailsAction.CreateShortcut -> {
            viewModel.getContentOrNull()?.let { manga ->
                coroutineScope.launch {
                    if (!appShortcutManager.requestPinShortcut(manga)) {
                        com.google.android.material.snackbar.Snackbar.make(
                            snackbarHost,
                            R.string.operation_not_supported,
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
        }
    }
}
