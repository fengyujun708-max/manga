package com.mangaverse.app.explore.domain

import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.core.model.GlobalTagBlacklist
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.util.ext.asArrayList
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.history.data.HistoryRepository
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.util.almostEquals
import com.mangaverse.app.parsers.util.runCatchingCancellable
import javax.inject.Inject

class ExploreRepository @Inject constructor(
	private val settings: AppSettings,
	private val sourcesRepository: ContentSourcesRepository,
	private val historyRepository: HistoryRepository,
	private val mangaRepositoryFactory: ContentRepository.Factory,
) {

	suspend fun findRandomContent(tagsLimit: Int): Content {
		val tagsBlacklist = TagsBlacklist(settings.suggestionsTagsBlacklist, 0.4f)
		val globalTagBlacklist = GlobalTagBlacklist(settings.globalTagBlacklist)
		val tagsWhitelist = settings.suggestionsTagsWhitelist.toList()
		val tags = (tagsWhitelist + historyRepository.getPopularTags(tagsLimit).mapNotNull {
			if (it in tagsBlacklist) null else it.title
		}).distinct()
		val sources = sourcesRepository.getEnabledSources()
		check(sources.isNotEmpty()) { "No sources available" }
		for (i in 0..4) {
			val list = getList(sources.random(), tags, tagsBlacklist)
			val manga = list.randomOrNull() ?: continue
			val details = runCatchingCancellable {
				mangaRepositoryFactory.create(manga.source).getDetails(manga)
			}.getOrNull() ?: continue
			if (
				(settings.isSuggestionsExcludeNsfw && details.isNsfw()) ||
				details in tagsBlacklist ||
				details in globalTagBlacklist
			) {
				continue
			}
			return details
		}
		throw NoSuchElementException()
	}

	suspend fun findRandomContent(source: ContentSource, tagsLimit: Int): Content {
		val tagsBlacklist = TagsBlacklist(settings.suggestionsTagsBlacklist, 0.4f)
		val globalTagBlacklist = GlobalTagBlacklist(settings.globalTagBlacklist)
		val skipNsfw = settings.isSuggestionsExcludeNsfw && !source.isNsfw()
		val tagsWhitelist = settings.suggestionsTagsWhitelist.toList()
		val tags = (tagsWhitelist + historyRepository.getPopularTags(tagsLimit).mapNotNull {
			if (it in tagsBlacklist) null else it.title
		}).distinct()
		for (i in 0..4) {
			val list = getList(source, tags, tagsBlacklist)
			val manga = list.randomOrNull() ?: continue
			val details = runCatchingCancellable {
				mangaRepositoryFactory.create(manga.source).getDetails(manga)
			}.getOrNull() ?: continue
			if ((skipNsfw && details.isNsfw()) || details in tagsBlacklist || details in globalTagBlacklist) {
				continue
			}
			return details
		}
		throw NoSuchElementException()
	}

	private suspend fun getList(
		source: ContentSource,
		tags: List<String>,
		blacklist: TagsBlacklist,
	): List<Content> = runCatchingCancellable {
		val repository = mangaRepositoryFactory.create(source)
		val order = repository.sortOrders.random()
		val availableTags = repository.getFilterOptions().availableTags
		val tag = tags.firstNotNullOfOrNull { title ->
			availableTags.find { x -> x.title.almostEquals(title, 0.4f) }
		}
		val list = repository.getList(
			offset = 0,
			order = order,
			filter = ContentListFilter(tags = setOfNotNull(tag)),
		).asArrayList()
		if (settings.isSuggestionsExcludeNsfw) {
			list.removeAll { it.isNsfw() }
		}
		if (blacklist.isNotEmpty()) {
			list.removeAll { manga -> manga in blacklist }
		}
		val globalTagBlacklist = GlobalTagBlacklist(settings.globalTagBlacklist)
		if (!globalTagBlacklist.isEmpty) {
			list.removeAll { manga -> manga in globalTagBlacklist }
		}
		list.shuffle()
		list
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrDefault(emptyList())
}
