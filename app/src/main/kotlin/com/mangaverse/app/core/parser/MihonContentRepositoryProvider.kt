package com.mangaverse.app.core.parser

import com.mangaverse.app.core.cache.MemoryContentCache
import com.mangaverse.app.mihon.MihonMangaRepository
import com.mangaverse.app.mihon.model.MihonMangaSource
import com.mangaverse.app.parsers.model.ContentSource
import javax.inject.Inject

class MihonContentRepositoryProvider @Inject constructor(
	private val contentCache: MemoryContentCache,
) : ContentRepositoryProvider {

	override fun supports(source: ContentSource): Boolean = source is MihonMangaSource

	override fun create(source: ContentSource): ContentRepository? {
		android.util.Log.d("MihonProvider", "create() called with source: ${source.name}, type: ${source::class.simpleName}")
		if (source !is MihonMangaSource) {
			android.util.Log.d("MihonProvider", "Source is not MihonMangaSource, returning null")
			return null
		}
		android.util.Log.d("MihonProvider", "Creating MihonMangaRepository for source: ${source.name}")
		return MihonMangaRepository(
			source = source,
			cache = contentCache,
		)
	}
}
