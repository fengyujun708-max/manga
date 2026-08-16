package com.mangaverse.app.core.parser

import com.mangaverse.app.core.model.LocalMangaSource
import com.mangaverse.app.core.model.UnknownContentSource
import com.mangaverse.app.local.data.LocalMangaRepository
import com.mangaverse.app.parsers.model.ContentSource
import javax.inject.Inject

class BuiltinContentRepositoryProvider @Inject constructor(
	private val localMangaRepository: LocalMangaRepository,
) : ContentRepositoryProvider {

	override fun supports(source: ContentSource): Boolean {
		return source == LocalMangaSource || source == UnknownContentSource
	}

	override fun create(source: ContentSource): ContentRepository? {
		return when (source) {
			LocalMangaSource -> localMangaRepository
			UnknownContentSource -> EmptyContentRepository(source)
			else -> null
		}
	}
}
