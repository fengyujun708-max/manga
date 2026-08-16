package com.mangaverse.app.parsers.core

import com.mangaverse.app.parsers.InternalParsersApi
import com.mangaverse.app.parsers.ContentLoaderContext
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.SortOrder

@InternalParsersApi
public abstract class SinglePageContentParser(
	context: ContentLoaderContext,
	source: ContentSource,
) : AbstractContentParser(context, source) {

	final override suspend fun getList(offset: Int, order: SortOrder, filter: ContentListFilter): List<Content> {
		if (offset > 0) {
			return emptyList()
		}
		return getList(order, filter)
	}

	public abstract suspend fun getList(order: SortOrder, filter: ContentListFilter): List<Content>
}
