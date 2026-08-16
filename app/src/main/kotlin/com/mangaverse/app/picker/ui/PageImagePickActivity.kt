package com.mangaverse.app.picker.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.mangaverse.app.BuildConfig
import com.mangaverse.app.R
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.nav.router
import com.mangaverse.app.core.ui.BaseComposeActivity
import com.mangaverse.app.core.util.ext.getDisplayMessage
import com.mangaverse.app.core.util.ext.observeEvent
import com.mangaverse.app.details.ui.pager.pages.compose.PagesScreen
import com.mangaverse.app.details.ui.pager.pages.compose.pagePreviewGridColumns
import com.mangaverse.app.list.ui.compose.AppContentListRoute
import com.mangaverse.app.picker.ui.manga.ContentPickerViewModel
import com.mangaverse.app.picker.ui.page.PagePickerViewModel
import com.mangaverse.app.reader.ui.PageSaveHelper
import com.mangaverse.app.reader.ui.pager.ReaderPage
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class PageImagePickActivity : BaseComposeActivity() {
    @Inject lateinit var pageSaveHelperFactory: PageSaveHelper.Factory

    private val saveViewModel by viewModels<PageImagePickViewModel>()
    private val contentViewModel by viewModels<ContentPickerViewModel>()
    private val pageViewModel by viewModels<PagePickerViewModel>()
    private lateinit var pageSaveHelper: PageSaveHelper

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageSaveHelper = pageSaveHelperFactory.create(this)
        saveViewModel.onFileReady.observeEvent(this, ::finishWithResult)
        saveViewModel.onError.observeEvent(this) { error ->
            lifecycleScope.launch { snackbarHostState.showSnackbar(error.getDisplayMessage(resources)) }
        }
        val pagePhase = intent.hasExtra(AppRouter.KEY_MANGA) || intent.hasExtra(AppRouter.KEY_ID)
        setComposeContent {
            val saving by saveViewModel.isLoading.collectAsStateWithLifecycle()
            val pageDetails = if (pagePhase) {
                pageViewModel.manga.collectAsStateWithLifecycle().value
            } else {
                null
            }
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(pageDetails?.toContent()?.title ?: getString(R.string.pick_manga_page))
                        },
                        navigationIcon = {
                            IconButton(onClick = ::finishAfterTransition) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                            }
                        },
                    )
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    if (pagePhase) {
                        val thumbnails by pageViewModel.thumbnails.collectAsStateWithLifecycle()
                        val loading by pageViewModel.isLoading.collectAsStateWithLifecycle()
                        val noChapters by pageViewModel.isNoChapters.collectAsStateWithLifecycle(false)
                        val gridScale by pageViewModel.gridScale.collectAsStateWithLifecycle()
                        PagesScreen(
                            items = thumbnails,
                            gridColumns = pagePreviewGridColumns(gridScale),
                            selectedItemIds = remember { emptySet() },
                            emptyMessageResId = if (noChapters) R.string.no_chapters else null,
                            isLoading = loading,
                            onLoadNext = pageViewModel::loadNextChapter,
                            onVisiblePlaceholder = pageViewModel::loadTowardsChapter,
                            onItemClick = { item ->
                                pageViewModel.manga.value?.toContent()?.let { onPagePicked(it, item.page) }
                            },
                            onItemLongClick = {},
                            onSelectionActionClick = {},
                            onClearSelection = {},
                        )
                    } else {
                        AppContentListRoute(
                            viewModel = contentViewModel,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                            appRouter = router,
                            registerFilterCallback = false,
                            pullRefreshEnabled = false,
                            onNavigateToDetails = { _, content, _ -> openPagePhase(content.id) },
                        )
                    }
                    if (saving) CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
    }

    private fun openPagePhase(contentId: Long) {
        intent.putExtra(AppRouter.KEY_ID, contentId)
        recreate()
    }

    private fun onPagePicked(content: com.mangaverse.app.parsers.model.Content, page: ReaderPage) {
        saveViewModel.savePageToTempFile(
            pageSaveHelper,
            PageSaveHelper.Task(content, page.chapterId, page.index + 1, page.toContentPage()),
        )
    }

    private fun finishWithResult(file: File) {
        val uri = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.files", file)
        setResult(RESULT_OK, Intent().setData(uri).setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        finish()
    }
}
