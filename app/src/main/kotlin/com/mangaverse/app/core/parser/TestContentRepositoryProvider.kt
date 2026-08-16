package com.mangaverse.app.core.parser

import com.mangaverse.app.core.cache.MemoryContentCache
import com.mangaverse.app.core.model.TestContentSource
import com.mangaverse.app.parsers.ContentLoaderContext
import com.mangaverse.app.parsers.model.ContentSource
import javax.inject.Inject

class TestContentRepositoryProvider @Inject constructor(
	private val loaderContext: ContentLoaderContext,
	private val contentCache: MemoryContentCache,
) : ContentRepositoryProvider {

	override fun supports(source: ContentSource): Boolean = source == TestContentSource

	override fun create(source: ContentSource): ContentRepository? {
		if (source != TestContentSource) return null
		return TestContentRepository(
			loaderContext = loaderContext,
			cache = contentCache,
		)
	}
}
