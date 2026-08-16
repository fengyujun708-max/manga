package com.mangaverse.app.settings.sources.catalog

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.parsers.model.ContentSource

sealed interface SourceCatalogItem : ListModel {

	data class Source(
		val source: ContentSource,
	) : SourceCatalogItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Source && other.source == source
		}
	}

	data class Hint(
		@DrawableRes val icon: Int,
		@StringRes val title: Int,
		@StringRes val text: Int,
	) : SourceCatalogItem {

		override fun areItemsTheSame(other: ListModel): Boolean {
			return other is Hint && other.title == title
		}
	}
}
