package com.mangaverse.app.reader.ui

import com.mangaverse.app.bookmarks.domain.Bookmark
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.reader.ui.pager.ReaderPage

interface ReaderNavigationCallback {

	fun onPageSelected(page: ReaderPage): Boolean

	fun onChapterSelected(chapter: ContentChapter): Boolean

	fun onBookmarkSelected(bookmark: Bookmark): Boolean = onPageSelected(
		ReaderPage(bookmark.toContentPage(), bookmark.page, bookmark.chapterId),
	)
}
