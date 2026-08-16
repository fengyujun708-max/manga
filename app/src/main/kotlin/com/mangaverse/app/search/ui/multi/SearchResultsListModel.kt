package com.mangaverse.app.search.ui.multi

import android.content.Context
import androidx.annotation.StringRes
import com.mangaverse.app.core.model.getTitle
import com.mangaverse.app.list.ui.ListModelDiffCallback
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.list.ui.model.ContentListModel
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.SortOrder

data class SearchResultsListModel(
	@StringRes val titleResId: Int,
	val source: ContentSource,
	val listFilter: ContentListFilter?,
	val sortOrder: SortOrder?,
	val list: List<ContentListModel>,
	val error: Throwable?,
) : ListModel {

	fun getTitle(context: Context): String = if (titleResId != 0) {
		context.getString(titleResId)
	} else {
		source.getTitle(context)
	}

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is SearchResultsListModel && source == other.source && titleResId == other.titleResId
	}

	override fun getChangePayload(previousState: ListModel): Any? {
		return if (previousState is SearchResultsListModel && previousState.list != list) {
			ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
		} else {
			super.getChangePayload(previousState)
		}
	}
}
