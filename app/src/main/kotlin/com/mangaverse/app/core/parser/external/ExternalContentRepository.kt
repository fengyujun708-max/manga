package com.mangaverse.app.core.parser.external

import android.content.ContentResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import com.mangaverse.app.core.cache.MemoryContentCache
import com.mangaverse.app.core.parser.CachingContentRepository
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentListFilterCapabilities
import com.mangaverse.app.parsers.model.ContentListFilterOptions
import com.mangaverse.app.parsers.model.ContentPage
import com.mangaverse.app.parsers.model.SortOrder
import com.mangaverse.app.parsers.util.suspendlazy.suspendLazy
import java.util.EnumSet

class ExternalContentRepository(
	contentResolver: ContentResolver,
	override val source: ExternalContentSource,
	cache: MemoryContentCache,
) : CachingContentRepository(cache) {

	private val contentSource = ExternalPluginContentSource(contentResolver, source)

	private val capabilities by lazy {
		runCatching {
			contentSource.getCapabilities()
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrNull()
	}

	private val filterOptions = suspendLazy(initializer = contentSource::getListFilterOptions)

	override val sortOrders: Set<SortOrder>
		get() = capabilities?.availableSortOrders ?: EnumSet.of(SortOrder.POPULARITY)

	override val filterCapabilities: ContentListFilterCapabilities
		get() = capabilities?.listFilterCapabilities ?: ContentListFilterCapabilities()

	override var defaultSortOrder: SortOrder
		get() = capabilities?.availableSortOrders?.firstOrNull() ?: SortOrder.ALPHABETICAL
		set(value) = Unit

	override suspend fun getFilterOptions(): ContentListFilterOptions = filterOptions.get()

	override suspend fun getList(offset: Int, order: SortOrder?, filter: ContentListFilter?): List<Content> =
		runInterruptible(Dispatchers.IO) {
			contentSource.getList(offset, order ?: defaultSortOrder, filter ?: ContentListFilter.EMPTY)
		}

	override suspend fun getDetailsImpl(manga: Content): Content = runInterruptible(Dispatchers.IO) {
		contentSource.getDetails(manga)
	}

	override suspend fun getPagesImpl(chapter: ContentChapter, nextChapterUrl: String?): List<ContentPage> = runInterruptible(Dispatchers.IO) {
		contentSource.getPages(chapter)
	}

override suspend fun getPageUrl(page: ContentPage): String = runInterruptible(Dispatchers.IO) {
	contentSource.getPageUrl(page.url)
}

	override suspend fun getRelatedContentImpl(seed: Content): List<Content> = emptyList() // TODO
}
