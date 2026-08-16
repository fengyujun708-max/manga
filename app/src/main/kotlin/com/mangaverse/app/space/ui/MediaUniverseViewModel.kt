package com.mangaverse.app.space.ui

import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.work.domain.WorkAggregate

data class MediaUniverseItem(
	val content: Content,
	val inHistory: Boolean,
	val inFavorites: Boolean,
)

data class MediaUniverseUiState(
	val visible: Boolean = false,
	val loading: Boolean = false,
	val items: List<MediaUniverseItem> = emptyList(),
)

internal fun mergeMediaUniverseItems(
	history: List<WorkAggregate>,
	favorites: List<WorkAggregate>,
): List<MediaUniverseItem> {
	val merged = LinkedHashMap<Any, MediaUniverseItem>()
	fun add(aggregate: WorkAggregate, inHistory: Boolean, inFavorites: Boolean) {
		val content = aggregate.displayProjection ?: return
		val key = aggregate.identity.entityId?.let { "entity:$it" } ?: "content:${content.id}"
		val existing = merged[key]
		merged[key] = MediaUniverseItem(
			content = existing?.content ?: content,
			inHistory = existing?.inHistory == true || inHistory,
			inFavorites = existing?.inFavorites == true || inFavorites,
		)
	}
	history.forEach { add(it, inHistory = true, inFavorites = false) }
	favorites.forEach { add(it, inHistory = false, inFavorites = true) }
	return merged.values.toList()
}
