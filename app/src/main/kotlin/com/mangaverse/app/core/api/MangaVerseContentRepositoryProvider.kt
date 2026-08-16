package com.mangaverse.app.core.api

import com.mangaverse.app.core.api.data.MangaVerseRepository
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.parser.ContentRepositoryProvider
import com.mangaverse.app.parsers.model.ContentSource
import javax.inject.Inject

/**
 * 为 MangaVerse API 内容源创建 [MangaVerseContentRepository]。
 */
class MangaVerseContentRepositoryProvider @Inject constructor(
    private val repository: MangaVerseRepository,
) : ContentRepositoryProvider {

    override fun supports(source: ContentSource): Boolean =
        source.name == MangaVerseContentSource.name || source.name.startsWith("MANGAVERSE:")

    override fun create(source: ContentSource): ContentRepository? {
        if (!supports(source)) return null
        return MangaVerseContentRepository(
            source = source,
            repository = repository,
        )
    }
}
