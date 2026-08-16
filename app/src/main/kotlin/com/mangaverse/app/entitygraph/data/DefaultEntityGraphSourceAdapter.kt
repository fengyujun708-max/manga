package com.mangaverse.app.entitygraph.data

import dagger.Reusable
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.entitygraph.domain.Entity
import com.mangaverse.app.entitygraph.domain.EntityGraphSourceAdapter
import com.mangaverse.app.entitygraph.domain.EntityType
import com.mangaverse.app.entitygraph.domain.SourceResult
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.SortOrder
import com.mangaverse.app.parsers.util.levenshteinDistance
import javax.inject.Inject

private const val MAX_QUERY_COUNT = 3

@Reusable
class DefaultEntityGraphSourceAdapter @Inject constructor(
	private val contentSourcesRepository: ContentSourcesRepository,
	private val repositoryFactory: ContentRepository.Factory,
) : EntityGraphSourceAdapter {

	override suspend fun findContentForEntity(
		entity: Entity,
		allowedSourceNames: Set<String>,
		sourceLimit: Int,
		resultLimitPerSource: Int,
	): List<SourceResult> {
		if (entity.type != EntityType.WORK) {
			return emptyList()
		}
		val availableSources = contentSourcesRepository.getEnabledSources()
			.filter { allowedSourceNames.isEmpty() || it.name in allowedSourceNames }
			.take(sourceLimit.coerceAtLeast(0))
		if (availableSources.isEmpty()) {
			return emptyList()
		}
		val queries = mergeAliases(entity.primaryName, entity.aliases).take(MAX_QUERY_COUNT)
		val aggregated = LinkedHashMap<String, SourceResult>()
		for (source in availableSources) {
			val repository = runCatching { repositoryFactory.create(source) }.getOrNull() ?: continue
			if (!repository.filterCapabilities.isSearchSupported) {
				continue
			}
			for (query in queries) {
				val items = runCatching {
					repository.getList(
						offset = 0,
						order = SortOrder.RELEVANCE,
						filter = ContentListFilter(query = query),
					)
				}.getOrDefault(emptyList())
				for (content in items.take(resultLimitPerSource)) {
					val confidence = scoreContent(entity, content)
					if (confidence < 0.6f) {
						continue
					}
					val key = "${source.name}:${content.id}"
					val current = aggregated[key]
					if (current == null || confidence > current.confidence) {
						aggregated[key] = SourceResult(
							entity = entity,
							source = source,
							content = content,
							confidence = confidence,
						)
					}
				}
			}
		}
		return aggregated.values
			.sortedWith(
				compareByDescending<SourceResult> { it.confidence }
					.thenBy { it.source.name }
					.thenBy { it.content.title },
			)
	}

	private fun scoreContent(entity: Entity, content: Content): Float {
		val entityNames = mergeAliases(entity.primaryName, entity.aliases)
		val contentNames = buildList {
			add(content.title)
			addAll(content.altTitles)
		}.map { it.trim() }
			.filter { it.isNotEmpty() }
			.distinct()
		var best = 0f
		for (entityName in entityNames) {
			for (contentName in contentNames) {
				val score = scoreName(entityName, contentName)
				if (score > best) {
					best = score
				}
			}
		}
		return best
	}

	private fun scoreName(left: String, right: String): Float {
		if (left == right) {
			return 1f
		}
		if (left.equals(right, ignoreCase = true)) {
			return 0.9f
		}
		val normalizedLeft = normalizeName(left)
		val normalizedRight = normalizeName(right)
		if (normalizedLeft.isEmpty() || normalizedRight.isEmpty()) {
			return 0f
		}
		if (normalizedLeft == normalizedRight) {
			return 0.9f
		}
		val maxLength = maxOf(normalizedLeft.length, normalizedRight.length).coerceAtLeast(1)
		val similarity = 1f - normalizedLeft.levenshteinDistance(normalizedRight).toFloat() / maxLength.toFloat()
		if (similarity < 0.72f) {
			return 0f
		}
		return (0.7f + ((similarity - 0.72f) / 0.28f) * 0.18f).coerceIn(0.7f, 0.88f)
	}
}
