package com.mangaverse.app.details.ui.pager.pages.compose

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.snackbar.Snackbar
import com.mangaverse.app.R
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.util.ext.findActivity
import com.mangaverse.app.core.util.ext.observeEvent
import com.mangaverse.app.details.ui.DetailsViewModel
import com.mangaverse.app.details.ui.compose.state.DetailsPaneState
import com.mangaverse.app.details.ui.pager.ChaptersPagesViewModel
import com.mangaverse.app.details.ui.pager.pages.PageThumbnail
import com.mangaverse.app.details.ui.pager.pages.PagesViewModel
import com.mangaverse.app.reader.ui.PageSaveHelper
import com.mangaverse.app.reader.ui.ReaderNavigationCallback

@Composable
fun PagesScreenRoot(
	activityViewModel: ChaptersPagesViewModel,
	router: AppRouter,
	context: Context,
	pageSaveHelper: PageSaveHelper,
	viewForSnackbar: View,
	lifecycleOwner: LifecycleOwner,
	viewModel: PagesViewModel,
	detailsPaneState: DetailsPaneState? = null,
	thumbnailAspectRatio: Float = 0.7f,
) {
	val thumbnails by viewModel.thumbnails.collectAsStateWithLifecycle(initialValue = emptyList())
	val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(initialValue = false)
	val gridScale by viewModel.gridScale.collectAsStateWithLifecycle(initialValue = 1f)
	val fitPreview by viewModel.fitPreview.collectAsStateWithLifecycle(initialValue = false)
	val selectedItemIds = remember { mutableStateListOf<Long>() }
	val selectedIds = remember(selectedItemIds.toList()) {
		selectedItemIds.toSet()
	}

	val mangaDetails by activityViewModel.mangaDetails.collectAsStateWithLifecycle(initialValue = null)
	val readingState by activityViewModel.readingState.collectAsStateWithLifecycle(initialValue = null)
	val selectedBranch by activityViewModel.selectedBranch.collectAsStateWithLifecycle(initialValue = null)

	LaunchedEffect(mangaDetails, readingState, selectedBranch) {
		if (mangaDetails != null) {
			viewModel.updateState(PagesViewModel.State(mangaDetails!!, readingState, selectedBranch))
		}
	}

	DisposableEffect(Unit) {
		viewModel.onPageSaved.observeEvent(lifecycleOwner) { uris ->
			if (uris.isEmpty()) return@observeEvent
			if (uris.size == 1) {
				Snackbar.make(viewForSnackbar, R.string.page_saved, Snackbar.LENGTH_LONG).show()
			} else {
				Snackbar.make(
					viewForSnackbar,
					context.getString(R.string.pages_saved),
					Snackbar.LENGTH_LONG,
				).show()
			}
		}
		onDispose {}
	}

	PagesScreen(
		items = thumbnails,
		gridColumns = pagePreviewGridColumns(gridScale),
		selectedItemIds = selectedIds,
		fitPreview = fitPreview,
		thumbnailAspectRatio = thumbnailAspectRatio,
		emptyMessageResId = null,
		isLoading = isLoading,
		detailsPaneState = detailsPaneState,
		onLoadPrevious = viewModel::loadPrevChapter,
		onLoadNext = viewModel::loadNextChapter,
		onVisiblePlaceholder = viewModel::loadTowardsChapter,
		onItemClick = { item ->
			val thumbnail = item as PageThumbnail
			if (selectedItemIds.isNotEmpty()) {
				if (selectedItemIds.contains(thumbnail.page.id)) {
					selectedItemIds.remove(thumbnail.page.id)
				} else {
					selectedItemIds.add(thumbnail.page.id)
				}
			} else {
				val navigationCallback = (context as? ReaderNavigationCallback)
					?: (context.findActivity() as? ReaderNavigationCallback)
				if (navigationCallback?.onPageSelected(thumbnail.page) == true) {
					return@PagesScreen
				}
				val manga = activityViewModel.getContentOrNull() ?: return@PagesScreen
				val targetState = com.mangaverse.app.reader.ui.ReaderState(thumbnail.page.chapterId, thumbnail.page.index, 0)
				(activityViewModel as? DetailsViewModel)?.recordDetailsJump(targetState, "detail_page")
				router.openReader(
					com.mangaverse.app.core.nav.ReaderIntent.Builder(context)
						.manga(manga)
						.state(targetState)
						.build(),
				)
			}
		},
		onItemLongClick = { item ->
			val thumbnail = item as PageThumbnail
			if (selectedItemIds.contains(thumbnail.page.id)) {
				selectedItemIds.remove(thumbnail.page.id)
			} else {
				selectedItemIds.add(thumbnail.page.id)
			}
		},
		onSelectionActionClick = { actionId ->
			if (selectedIds.isEmpty()) return@PagesScreen
			when (actionId) {
				R.id.action_save -> {
					val snapshot = thumbnails.filterIsInstance<PageThumbnail>()
						.filter { it.page.id in selectedIds }
						.map { it.page }
						.toSet()
					viewModel.savePages(pageSaveHelper, snapshot)
				}
			}
			selectedItemIds.clear()
		},
		onClearSelection = { selectedItemIds.clear() },
	)
}
