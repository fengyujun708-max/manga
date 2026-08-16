package com.mangaverse.app.bookmarks.data

import com.mangaverse.app.bookmarks.domain.Bookmark
import com.mangaverse.app.parsers.model.Content
import java.time.Instant

fun BookmarkEntity.toBookmark(manga: Content) = Bookmark(
	manga = manga,
	pageId = pageId,
	chapterId = chapterId,
	chapterTitle = null,
	page = page,
	scroll = scroll,
	imageUrl = imageUrl,
	createdAt = Instant.ofEpochMilli(createdAt),
	percent = percent,
)

fun Bookmark.toEntity() = BookmarkEntity(
	mangaId = manga.id,
	pageId = pageId,
	chapterId = chapterId,
	page = page,
	scroll = scroll,
	imageUrl = imageUrl,
	createdAt = createdAt.toEpochMilli(),
	percent = percent,
)

fun Collection<BookmarkEntity>.toBookmarks(manga: Content) = map {
	it.toBookmark(manga)
}

@JvmName("bookmarksIds")
fun Collection<Bookmark>.ids() = map { it.pageId }
