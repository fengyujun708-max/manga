package com.mangaverse.app.core.parser

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.mangaverse.app.core.cache.MemoryContentCache
import com.mangaverse.app.core.parser.external.ExternalContentRepository
import com.mangaverse.app.core.parser.external.ExternalContentSource
import com.mangaverse.app.parsers.model.ContentSource
import javax.inject.Inject

class ExternalContentRepositoryProvider @Inject constructor(
	@ApplicationContext private val context: Context,
	private val contentCache: MemoryContentCache,
) : ContentRepositoryProvider {

	override fun supports(source: ContentSource): Boolean = source is ExternalContentSource

	override fun create(source: ContentSource): ContentRepository? {
		if (source !is ExternalContentSource) return null
		return if (source.isAvailable(context)) {
			ExternalContentRepository(
				contentResolver = context.contentResolver,
				source = source,
				cache = contentCache,
			)
		} else {
			EmptyContentRepository(source)
		}
	}
}
