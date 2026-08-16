package com.mangaverse.app.core.parser

import com.mangaverse.app.core.cache.MemoryContentCache
import com.mangaverse.app.core.model.TestContentSource
import com.mangaverse.app.parsers.ContentLoaderContext

@Suppress("unused")
class TestContentRepository(
	private val loaderContext: ContentLoaderContext,
	cache: MemoryContentCache
) : EmptyContentRepository(TestContentSource)
