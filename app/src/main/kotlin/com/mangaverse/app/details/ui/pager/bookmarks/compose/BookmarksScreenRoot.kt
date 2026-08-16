package com.mangaverse.app.details.ui.pager.bookmarks.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangaverse.app.R
import com.mangaverse.app.bookmarks.domain.Bookmark
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.nav.ReaderIntent
import com.mangaverse.app.core.util.ext.findActivity
import com.mangaverse.app.details.ui.DetailsViewModel
import com.mangaverse.app.details.ui.compose.state.DetailsPaneState
import com.mangaverse.app.details.ui.pager.ChaptersPagesViewModel
import com.mangaverse.app.details.ui.pager.bookmarks.BookmarksViewModel
import com.mangaverse.app.reader.ui.ReaderNavigationCallback

@Composable
fun BookmarksScreenRoot(
	activityViewModel: ChaptersPagesViewModel,
	router: AppRouter,
	context: Context,
	viewModel: BookmarksViewModel,
	detailsPaneState: DetailsPaneState? = null,
) {
	val contentItems by viewModel.content.collectAsStateWithLifecycle(initialValue = emptyList())
	val gridScale by viewModel.gridScale.collectAsStateWithLifecycle(initialValue = 1f)
	val selectedItemIds = remember { mutableStateListOf<Long>() }
	val selectedIds = remember(selectedItemIds.toList()) {
		selectedItemIds.toSet()
	}

	val mangaDetails by activityViewModel.mangaDetails.collectAsStateWithLifecycle(initialValue = null)
	LaunchedEffect(mangaDetails) {
		viewModel.emit(mangaDetails)
	}

	BookmarksScreen(
		items = contentItems,
		gridMinSize = (120.dp / gridScale.coerceIn(0.5f, 1.5f)),
		selectedItemIds = selectedIds,
		detailsPaneState = detailsPaneState,
		onItemClick = { item ->
			val bookmark = item as Bookmark
			if (selectedItemIds.isNotEmpty()) {
				if (selectedItemIds.contains(bookmark.pageId)) {
					selectedItemIds.remove(bookmark.pageId)
				} else {
					selectedItemIds.add(bookmark.pageId)
				}
			} else {
				val navigationCallback = (context as? ReaderNavigationCallback)
					?: (context.findActivity() as? ReaderNavigationCallback)
				if (navigationCallback?.onBookmarkSelected(bookmark) == true) {
					return@BookmarksScreen
				}
				val targetState = com.mangaverse.app.reader.ui.ReaderState(bookmark.chapterId, bookmark.page, bookmark.scroll)
				(activityViewModel as? DetailsViewModel)?.recordDetailsJump(targetState, "detail_bookmark")
				router.openReader(
					ReaderIntent.Builder(context)
						.manga(bookmark.manga)
						.state(targetState)
						.build(),
				)
			}
		},
		onItemLongClick = { item ->
			val bookmark = item as Bookmark
			if (selectedItemIds.contains(bookmark.pageId)) {
				selectedItemIds.remove(bookmark.pageId)
			} else {
				selectedItemIds.add(bookmark.pageId)
			}
		},
		onSelectionActionClick = { actionId ->
			if (actionId == R.id.action_delete) {
				viewModel.removeBookmarks(selectedIds)
			}
			selectedItemIds.clear()
		},
		onClearSelection = { selectedItemIds.clear() },
	)
}
