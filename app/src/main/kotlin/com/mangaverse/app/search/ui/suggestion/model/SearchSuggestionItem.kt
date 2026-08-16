package com.mangaverse.app.search.ui.suggestion.model

import androidx.annotation.StringRes
import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.core.ui.widgets.ChipsView
import com.mangaverse.app.list.ui.ListModelDiffCallback
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.search.domain.LocalEntitySuggestion

sealed interface SearchSuggestionItem : ListModel {

	data class ContentList(
		val items: List<Content>,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is ContentList
		}
	}

	data class LocalEntityList(
		val items: List<LocalEntitySuggestion>,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is LocalEntityList
		}

		override fun getChangePayload(previousState: ListModel): Any {
			return ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
		}
	}

	data class RecentQuery(
		val query: String,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is RecentQuery && query == other.query
		}
	}

	data class Hint(
		val query: String,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Hint && query == other.query
		}
	}

	data class Author(
		val name: String,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Author && name == other.name
		}
	}

	data class Source(
		val source: ContentSource,
		val isEnabled: Boolean,
	) : SearchSuggestionItem {

		val isNsfw: Boolean
			get() = source.isNsfw()

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Source && other.source.name == source.name
		}

		override fun getChangePayload(previousState: ListModel): Any? {
			if (previousState !is Source) {
				return super.getChangePayload(previousState)
			}
			return if (isEnabled != previousState.isEnabled) {
				ListModelDiffCallback.PAYLOAD_CHECKED_CHANGED
			} else {
				null
			}
		}
	}

	data class SourceTip(
		val source: ContentSource,
	) : SearchSuggestionItem {

		val isNsfw: Boolean
			get() = source.isNsfw()

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is SourceTip && other.source.name == source.name
		}
	}

	data class Tags(
		val tags: List<ChipsView.ChipModel>,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Tags
		}

		override fun getChangePayload(previousState: ListModel): Any {
			return ListModelDiffCallback.PAYLOAD_NESTED_LIST_CHANGED
		}
	}

	data class Text(
		@StringRes val textResId: Int,
		val error: Throwable?,
	) : SearchSuggestionItem {

		override fun areItemsTheSame(other: ListModel): Boolean = other is Text
			&& textResId == other.textResId
			&& error?.javaClass == other.error?.javaClass
			&& error?.message == other.error?.message
	}
}

