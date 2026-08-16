package com.mangaverse.app.space.data

import kotlinx.serialization.json.Json
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.space.domain.SPACE_ROUTE_PREFERENCES_SCHEMA_VERSION
import com.mangaverse.app.space.domain.SpaceId
import com.mangaverse.app.space.domain.SpaceCatalogRepository
import com.mangaverse.app.space.domain.SpaceListPreferences
import com.mangaverse.app.space.domain.SpaceRoutePreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSpaceRoutePreferencesRepository internal constructor(
	private val dao: SpaceRoutePreferencesDao,
	private val json: Json,
	private val catalogRepository: SpaceCatalogRepository,
) : SpaceRoutePreferencesRepository {

	@Inject
	constructor(database: MangaDatabase, json: Json, catalogRepository: SpaceCatalogRepository) :
		this(database.getSpaceRoutePreferencesDao(), json, catalogRepository)

	override suspend fun load(spaceId: SpaceId, routeKey: String): SpaceListPreferences? {
		requireValidKey(spaceId, routeKey)
		val entity = dao.find(spaceId.value, routeKey) ?: return null
		if (entity.schemaVersion != SPACE_ROUTE_PREFERENCES_SCHEMA_VERSION) return null
		return runCatching {
			json.decodeFromString(SpaceListPreferences.serializer(), entity.payload)
		}.getOrNull()
	}

	override suspend fun save(
		spaceId: SpaceId,
		routeKey: String,
		preferences: SpaceListPreferences,
	) {
		requireValidKey(spaceId, routeKey)
		dao.upsert(
			SpaceRoutePreferencesEntity(
				spaceId = spaceId.value,
				routeKey = routeKey,
				payload = json.encodeToString(SpaceListPreferences.serializer(), preferences),
				schemaVersion = SPACE_ROUTE_PREFERENCES_SCHEMA_VERSION,
				updatedAt = System.currentTimeMillis(),
			),
		)
	}

	override suspend fun delete(spaceId: SpaceId, routeKey: String) {
		requireValidKey(spaceId, routeKey)
		dao.delete(spaceId.value, routeKey)
	}

	private fun requireValidKey(spaceId: SpaceId, routeKey: String) {
		require(catalogRepository.find(spaceId) != null) { "Unknown SpaceId: ${spaceId.value}" }
		require(routeKey.isNotBlank()) { "routeKey must not be blank" }
	}
}
