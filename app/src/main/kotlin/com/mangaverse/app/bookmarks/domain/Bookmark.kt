package com.mangaverse.app.bookmarks.domain

import com.mangaverse.app.core.util.MimeTypes
import com.mangaverse.app.core.util.ext.isImage
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentPage
import java.time.Instant

data class Bookmark(
	val manga: Content,
	val pageId: Long,
	val chapterId: Long,
	val chapterTitle: String? = null,
	val page: Int,
	val scroll: Int,
	val imageUrl: String,
	val createdAt: Instant,
	val percent: Float,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is Bookmark &&
			manga.id == other.manga.id &&
			chapterId == other.chapterId &&
			page == other.page
	}

	fun toContentPage() = ContentPage(
		id = pageId,
		url = imageUrl,
		preview = imageUrl.takeIf {
			MimeTypes.getMimeTypeFromUrl(it)?.isImage == true
		},
		source = manga.source,
	)
}
