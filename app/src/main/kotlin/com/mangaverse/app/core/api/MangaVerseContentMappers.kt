package com.mangaverse.app.core.api

import com.mangaverse.app.core.api.model.ApiMangaWithRoutes
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentTag
import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.parsers.model.RATING_UNKNOWN
import java.util.Locale

/**
 * MangaVerse API DTO → 客户端 [Content] 映射（顶层扩展函数）。
 *
 * 供 [MangaVerseContentRepository] 与 UI 层（服务器详情页等）复用。
 * 详情反查键使用第一条路线的 source_id + source_manga_id 编码进 [Content.sourceData]，
 * 保证 getDetails / getChapters / getPages 端到端正确（去重后 manga.id 不再可靠）。
 */
fun ApiMangaWithRoutes.toMangaVerseContent(): Content? {
    val title = manga.title.ifBlank { return null }

    // 优先使用第一条路线的 source_id + source_manga_id（去重后最可靠的反查键）
    val (sourceId, sourceMangaId) = routes.firstOrNull()?.let { route ->
        route.sourceId to route.sourceMangaId
    } ?: (manga.matchSources.firstOrNull() ?: "api") to (manga.id ?: title.lowercase(Locale.ROOT))

    val stableId = stableIdFromString(sourceMangaId)
    val source = MangaVerseBoundSource("MANGAVERSE:$sourceId")
    return Content(
        id = stableId,
        title = title,
        altTitles = manga.titleAlt?.let { setOf(it) } ?: emptySet(),
        url = sourceMangaId,
        publicUrl = manga.coverUrl ?: "",
        rating = RATING_UNKNOWN,
        contentRating = null,
        coverUrl = manga.coverUrl,
        tags = emptySet<ContentTag>(),
        state = null,
        authors = emptySet(),
        description = manga.description,
        source = source,
        sourceData = "$sourceId:$sourceMangaId",
    )
}

internal fun stableIdFromString(value: String): Long {
    var hash = 1125899906842597L
    for (element in value) {
        hash = 31 * hash + element.code
    }
    return hash and Long.MAX_VALUE
}

internal data class MangaVerseBoundSource(override val name: String) : ContentSource {
    override val locale: String = ""
    override val contentType: ContentType = ContentType.MANGA
}
