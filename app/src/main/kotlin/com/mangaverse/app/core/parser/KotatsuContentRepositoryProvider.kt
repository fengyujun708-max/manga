package com.mangaverse.app.core.parser

import com.mangaverse.app.core.cache.MemoryContentCache
import com.mangaverse.app.core.parser.kotatsu.KotatsuParserRepository
import com.mangaverse.app.core.parser.kotatsu.KotatsuParserSource
import com.mangaverse.app.core.parser.kotatsu.KotatsuLoaderContextAdapter
import com.mangaverse.app.core.extensions.GlobalExtensionManager
import com.mangaverse.app.parsers.ContentLoaderContext
import com.mangaverse.app.parsers.model.ContentSource
import javax.inject.Inject

class KotatsuContentRepositoryProvider @Inject constructor(
	private val loaderContext: ContentLoaderContext,
	private val contentCache: MemoryContentCache,
) : ContentRepositoryProvider {

	override fun supports(source: ContentSource): Boolean = source is KotatsuParserSource
	override fun create(source: ContentSource): ContentRepository? {
		if (source !is KotatsuParserSource) return null
		val mangaContext = KotatsuLoaderContextAdapter(loaderContext)
		return KotatsuParserRepository(
			parser = GlobalExtensionManager.getMangaParser(source.delegate, mangaContext),
			kotatsuSource = source,
			loaderContext = loaderContext,
			cache = contentCache,
		)
	}
}
