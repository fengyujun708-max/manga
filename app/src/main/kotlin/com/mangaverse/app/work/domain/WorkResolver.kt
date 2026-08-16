package com.mangaverse.app.work.domain

import com.mangaverse.app.entitygraph.domain.EntityBinding
import com.mangaverse.app.parsers.model.Content

interface WorkResolver {

	suspend fun resolveByMangaId(mangaId: Long): WorkIdentity

	suspend fun resolveByEntityId(entityId: Long): WorkIdentity?

	suspend fun resolveBindingsByEntityId(entityId: Long): List<EntityBinding>

	suspend fun resolveManyByMangaIds(mangaIds: Collection<Long>): Map<Long, WorkIdentity>

	suspend fun ensureForProjection(
		content: Content,
		provenance: WorkIdentityProvenance = WorkIdentityProvenance.USER,
	): WorkIdentity

	suspend fun bindProjectionToEntity(
		targetEntityId: Long,
		projection: Content,
	): WorkProjectionBindingResult

	suspend fun selectPreferredProjection(entityId: Long): Long?
}

enum class WorkIdentityProvenance {
	USER,
	IMPORT,
	MIGRATION,
	RESTORE,
}
