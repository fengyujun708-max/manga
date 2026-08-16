package com.mangaverse.app.explore.ui.model

import com.mangaverse.app.core.model.ContentSourceInfo
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.parsers.util.longHashCode

data class ContentSourceItem(
	val source: ContentSourceInfo,
	val isGrid: Boolean,
) : ListModel {

	val id: Long = source.name.longHashCode()

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ContentSourceItem && other.source.name == source.name
	}
}
