package com.mangaverse.app.details.ui.pager.pages

import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.reader.ui.pager.ReaderPage

data class PageThumbnail(
	val isCurrent: Boolean,
	val page: ReaderPage,
) : ListModel {

	val number
		get() = page.index + 1

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is PageThumbnail && page == other.page
	}
}
