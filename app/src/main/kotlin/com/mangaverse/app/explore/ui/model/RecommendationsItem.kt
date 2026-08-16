package com.mangaverse.app.explore.ui.model

import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.list.ui.model.ContentCompactListModel

data class RecommendationsItem(
	val manga: List<ContentCompactListModel>
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is RecommendationsItem
	}
}
