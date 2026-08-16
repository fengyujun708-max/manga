package com.mangaverse.app.entitygraph.domain

interface EntityBindingMatcher {

	suspend fun tryBindEntities(
		entityA: Entity,
		entityB: Entity,
	): Float

	fun classify(confidence: Float): EntityBindingStrength
}
