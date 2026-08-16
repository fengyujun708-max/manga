package com.mangaverse.app.tracker.ui.feed.model

import com.mangaverse.app.list.ui.ListModelDiffCallback
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.list.ui.model.ContentListModel

data class UpdatedContentHeaderItem(
	val model: ContentListModel,
	val groupKey: Long,
	val entityId: Long?,
	val preferredLocalMangaId: Long?,
	val totalNewChapters: Int,
)

data class UpdatedContentHeader(
	val list: List<UpdatedContentHeaderItem>,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is UpdatedContentHeader
	}

	override fun getChangePayload(previousState: ListModel): Any {
		return ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
	}
}
