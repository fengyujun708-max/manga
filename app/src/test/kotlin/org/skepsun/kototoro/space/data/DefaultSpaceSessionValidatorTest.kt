package com.mangaverse.app.space.data

import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import com.mangaverse.app.entitygraph.domain.EntityBinding
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.space.domain.BuiltInSpaces
import com.mangaverse.app.space.domain.SpaceRouteSnapshot
import com.mangaverse.app.space.domain.SpaceSessionSnapshot
import com.mangaverse.app.work.domain.WorkIdentity
import com.mangaverse.app.work.domain.WorkIdentityProvenance
import com.mangaverse.app.work.domain.WorkMigrationState
import com.mangaverse.app.work.domain.WorkProjectionBindingResult
import com.mangaverse.app.work.domain.WorkResolver

class DefaultSpaceSessionValidatorTest {

	@Test
	fun `missing work truncates it and all dependent routes`() = runTest {
		val validator = DefaultSpaceSessionValidator(FakeWorkResolver())
		val snapshot = snapshot(
			routes = listOf(
				SpaceRouteSnapshot.TopLevel("home"),
				SpaceRouteSnapshot.WorkDetails(404L, null),
				SpaceRouteSnapshot.ContentList("AVAILABLE"),
			),
		)

		val validated = validator.validate(snapshot)

		validated.stacks shouldContainExactly mapOf(
			"home" to listOf(SpaceRouteSnapshot.TopLevel("home")),
		)
	}

	@Test
	fun `invalid projection falls back to entity details`() = runTest {
		val validator = DefaultSpaceSessionValidator(
			FakeWorkResolver(entityIds = setOf(42L), projections = setOf(7L)),
		)
		val snapshot = snapshot(
			routes = listOf(
				SpaceRouteSnapshot.TopLevel("home"),
				SpaceRouteSnapshot.WorkDetails(42L, 99L),
			),
		)

		val validated = validator.validate(snapshot)

		validated.stacks.getValue("home").last() shouldBe SpaceRouteSnapshot.WorkDetails(42L, null)
	}

	@Test
	fun `temporarily unavailable source keeps content list route for cold start restoration`() = runTest {
		val validator = DefaultSpaceSessionValidator(
			FakeWorkResolver(),
		)
		val snapshot = snapshot(
			routes = listOf(
				SpaceRouteSnapshot.TopLevel("home"),
				SpaceRouteSnapshot.ContentList("REMOVED"),
			),
		)

		val validated = validator.validate(snapshot)

		validated.stacks.getValue("home") shouldBe listOf(
			SpaceRouteSnapshot.TopLevel("home"),
			SpaceRouteSnapshot.ContentList("REMOVED"),
		)
	}

	private fun snapshot(routes: List<SpaceRouteSnapshot>) = SpaceSessionSnapshot(
		spaceId = BuiltInSpaces.Manga,
		selectedTopLevel = "home",
		resumeRoute = routes.lastOrNull(),
		stacks = mapOf("home" to routes),
		lastAccessed = 1L,
		updatedAt = 1L,
	)
}

private class FakeWorkResolver(
	private val entityIds: Set<Long> = emptySet(),
	private val projections: Set<Long> = emptySet(),
) : WorkResolver {
	override suspend fun resolveByMangaId(mangaId: Long): WorkIdentity = error("Not used")

	override suspend fun resolveByEntityId(entityId: Long): WorkIdentity? {
		if (entityId !in entityIds) return null
		return WorkIdentity(
			entityId = entityId,
			requestedMangaId = null,
			preferredMangaId = projections.firstOrNull(),
			localMangaIds = projections,
			migrationState = WorkMigrationState.VALID,
		)
	}

	override suspend fun resolveManyByMangaIds(mangaIds: Collection<Long>): Map<Long, WorkIdentity> = error("Not used")

	override suspend fun resolveBindingsByEntityId(entityId: Long): List<EntityBinding> = error("Not used")

	override suspend fun ensureForProjection(content: Content, provenance: WorkIdentityProvenance): WorkIdentity =
		error("Not used")

	override suspend fun bindProjectionToEntity(
		targetEntityId: Long,
		projection: Content,
	): WorkProjectionBindingResult = error("Not used")

	override suspend fun selectPreferredProjection(entityId: Long): Long? = error("Not used")
}
