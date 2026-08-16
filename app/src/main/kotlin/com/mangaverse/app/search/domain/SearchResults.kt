package com.mangaverse.app.search.domain

import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.SortOrder

data class SearchResults(
	val listFilter: ContentListFilter,
	val sortOrder: SortOrder,
	val manga: List<Content>,
)
