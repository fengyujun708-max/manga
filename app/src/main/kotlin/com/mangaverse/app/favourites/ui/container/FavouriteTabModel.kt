package com.mangaverse.app.favourites.ui.container

import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.list.domain.ListSortOrder

data class FavouriteTabModel(
	val id: Long,
	val title: String?,
	val order: ListSortOrder? = null,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is FavouriteTabModel && other.id == id
	}
}
