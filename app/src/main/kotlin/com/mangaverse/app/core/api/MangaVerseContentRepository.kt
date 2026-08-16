package com.mangaverse.app.core.api

import com.mangaverse.app.core.api.data.MangaVerseRepository
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentListFilterCapabilities
import com.mangaverse.app.parsers.model.ContentListFilterOptions
import com.mangaverse.app.parsers.model.ContentPage
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.RATING_UNKNOWN
import com.mangaverse.app.parsers.model.SortOrder
import java.util.Locale

/**
 * 基于 MangaVerse API 的 [ContentRepository] 实现。
 *
 * 将 MangaVerse 后端的漫画/章节/图片数据映射为客户端 [Content]/[ContentChapter]/[ContentPage]。
 * API 使用字符串 ID，这里用稳定的字符串哈希映射为客户端 Long ID，
 * 并在 [Content.sourceData] 中编码 `sourceId:sourceMangaId` 用于详情/章节反查。
 */
class MangaVerseContentRepository(
    override val source: ContentSource,
    private val repository: MangaVerseRepository,
) : ContentRepository {

    override val sortOrders: Set<SortOrder> = setOf(SortOrder.POPULARITY, SortOrder.NEWEST, SortOrder.RELEVANCE)

    override var defaultSortOrder: SortOrder = SortOrder.POPULARITY

    override val filterCapabilities: ContentListFilterCapabilities
        get() = ContentListFilterCapabilities(isSearchSupported = true)

    override suspend fun getList(offset: Int, order: SortOrder?, filter: ContentListFilter?): List<Content> {
        val query = filter?.query?.takeIf { it.isNotBlank() }
        if (query != null) {
            val page = offset.coerceAtLeast(0) + 1
            val response = repository.search(query, category = null, page = page)
                .getOrElse { return emptyList() }
            return response.results.mapNotNull { it.manga.toContent() }
        }
        val hot = repository.getHotList("all", 20).getOrElse { return emptyList() }
        return hot.mapNotNull { it.toContent() }
    }

    override suspend fun getDetails(manga: Content): Content {
        val (sourceId, sourceMangaId) = manga.decodeSourceData() ?: return manga
        val detail = repository.getMangaDetail(sourceId, sourceMangaId).getOrElse { return manga }
        return detail.toContent() ?: manga
    }

    override suspend fun getPages(chapter: ContentChapter, nextChapterUrl: String?): List<ContentPage> {
        val (sourceId, sourceMangaId) = chapter.decodeSourceData() ?: return emptyList()
        val chapterId = chapter.url ?: return emptyList()
        val pages = repository.getChapterPages(sourceId, sourceMangaId, chapterId)
            .getOrElse { return emptyList() }
        return pages.mapIndexed { index, pageUrl ->
            ContentPage(
                id = index.toLong(),
                url = pageUrl,
                preview = null,
                source = chapter.source,
            )
        }
    }

    override suspend fun getPageUrl(page: ContentPage): String = page.url

    override suspend fun getFilterOptions(): ContentListFilterOptions = ContentListFilterOptions()
    override suspend fun getRelated(seed: Content): List<Content> = emptyList()

    private fun com.mangaverse.app.core.api.model.ApiMangaWithRoutes.toContent(): Content? =
        toMangaVerseContent()

    private fun com.mangaverse.app.core.api.model.ApiManga.toContent(): Content? {
        val title = title.ifBlank { return null }
        val sourceName = "MANGAVERSE:${(matchSources.firstOrNull() ?: "api")}"
        val sourceId = matchSources.firstOrNull() ?: "api"
        val sourceMangaId = id ?: title.lowercase(Locale.ROOT)
        val stableId = stableIdFromString(sourceMangaId)
        val source = MangaVerseBoundSource(sourceName)
        return Content(
            id = stableId,
            title = title,
            altTitles = titleAlt?.let { setOf(it) } ?: emptySet(),
            url = sourceMangaId,
            publicUrl = coverUrl ?: "",
            rating = RATING_UNKNOWN,
            contentRating = null,
            coverUrl = coverUrl,
            tags = emptySet<com.mangaverse.app.parsers.model.ContentTag>(),
            state = null,
            authors = emptySet(),
            description = description,
            source = source,
            sourceData = "$sourceId:$sourceMangaId",
        )
    }

    private fun Content.decodeSourceData(): Pair<String, String>? {
        val data = sourceData ?: return null
        val idx = data.indexOf(':')
        if (idx <= 0) return null
        return data.substring(0, idx) to data.substring(idx + 1)
    }

    private fun ContentChapter.decodeSourceData(): Pair<String, String>? {
        val data = sourceData ?: return null
        val idx = data.indexOf(':')
        if (idx <= 0) return null
        return data.substring(0, idx) to data.substring(idx + 1)
    }
}
