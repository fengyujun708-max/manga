package com.mangaverse.app.entitygraph.domain

import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentSource

interface EntityGraphSourceAdapter {

	suspend fun findContentForEntity(
		entity: Entity,
		allowedSourceNames: Set<String> = emptySet(),
		sourceLimit: Int = 8,
		resultLimitPerSource: Int = 5,
	): List<SourceResult>
}

data class SourceResult(
	val entity: Entity,
	val source: ContentSource,
	val content: Content,
	val confidence: Float,
)
