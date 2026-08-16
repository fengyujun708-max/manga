package com.mangaverse.app.details.ui.pager.pages

import com.mangaverse.app.list.ui.model.ListModel

data class PageThumbnailPlaceholder(
	val chapterId: Long,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is PageThumbnailPlaceholder && chapterId == other.chapterId
	}
}
