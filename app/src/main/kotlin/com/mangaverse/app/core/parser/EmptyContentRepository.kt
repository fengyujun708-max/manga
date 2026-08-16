package com.mangaverse.app.core.parser

import com.mangaverse.app.core.exceptions.UnsupportedSourceException
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentListFilterCapabilities
import com.mangaverse.app.parsers.model.ContentListFilterOptions
import com.mangaverse.app.parsers.model.ContentPage
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.SortOrder
import java.util.EnumSet

open class EmptyContentRepository(override val source: ContentSource) : ContentRepository {

	override val sortOrders: Set<SortOrder>
		get() = EnumSet.allOf(SortOrder::class.java)

	override var defaultSortOrder: SortOrder
		get() = SortOrder.NEWEST
		set(value) = Unit

	override val filterCapabilities: ContentListFilterCapabilities
		get() = ContentListFilterCapabilities()

	override suspend fun getList(offset: Int, order: SortOrder?, filter: ContentListFilter?): List<Content> = stub(null)

	override suspend fun getDetails(manga: Content): Content = stub(manga)

	override suspend fun getPages(chapter: ContentChapter, nextChapterUrl: String?): List<ContentPage> = stub(null)

	override suspend fun getPageUrl(page: ContentPage): String = stub(null)

	override suspend fun getChapterContent(chapter: ContentChapter, nextChapterUrl: String?) = stub(null)

	override suspend fun getFilterOptions(): ContentListFilterOptions = stub(null)

	override suspend fun getRelated(seed: Content): List<Content> = stub(seed)

	private fun stub(manga: Content?): Nothing {
		throw UnsupportedSourceException("This manga source is not supported", manga)
	}
}
