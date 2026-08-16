package com.mangaverse.app.alternatives.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.util.ext.toLocale
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.core.model.getLocale
import com.mangaverse.app.core.model.getContentType
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.util.runCatchingCancellable
import com.mangaverse.app.search.domain.SearchKind
import com.mangaverse.app.search.domain.SearchV2Helper
import java.util.Locale
import javax.inject.Inject

private const val MAX_PARALLELISM = 4

class AlternativesUseCase @Inject constructor(
	private val sourcesRepository: ContentSourcesRepository,
	private val searchHelperFactory: SearchV2Helper.Factory,
	private val mangaRepositoryFactory: ContentRepository.Factory,
) {

	suspend operator fun invoke(manga: Content, throughDisabledSources: Boolean, pinnedOnly: Boolean = false): Flow<Content> {
		val sources = getSources(manga.source, throughDisabledSources).let { list ->
			if (pinnedOnly) {
				val pinned = sourcesRepository.getPinnedSources()
				val pinnedNames = pinned.map { it.name }.toSet()
				list.filter { it.name in pinnedNames }
			} else list
		}
		if (sources.isEmpty()) {
			return emptyFlow()
		}
		val semaphore = Semaphore(MAX_PARALLELISM)
		return channelFlow {
			for (source in sources) {
				launch {
					val searchHelper = searchHelperFactory.create(source)
					val list = runCatchingCancellable {
						semaphore.withPermit {
							searchHelper(manga.title, SearchKind.TITLE)?.manga
						}
					}.getOrNull()
					list?.forEach { m ->
						if (m.id != manga.id) {
							launch {
								val details = runCatchingCancellable {
									mangaRepositoryFactory.create(m.source).getDetails(m)
								}.getOrDefault(m)
								send(details)
							}
						}
					}
				}
			}
		}
	}

	private suspend fun getSources(ref: ContentSource, disabled: Boolean): List<ContentSource> = if (disabled) {
		sourcesRepository.getDisabledSources()
	} else {
		sourcesRepository.getEnabledSources()
	}.sortedByDescending { it.priority(ref) }

	private fun ContentSource.priority(ref: ContentSource): Int {
		var res = 0
		if (this.getLocale() == ref.getLocale()) {
			res += 4
		} else if (this.getLocale() == Locale.getDefault()) {
			res += 2
		}
		if (this.getContentType() == ref.getContentType()) {
			res++
		}
		return res
	}
}
