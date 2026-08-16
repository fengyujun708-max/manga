package com.mangaverse.app.history.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.entitygraph.data.EntityGraphRepository
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentRating
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.parsers.model.RATING_UNKNOWN
import com.mangaverse.app.scrobbling.common.domain.Scrobbler
import com.mangaverse.app.space.domain.BuiltInSpaces
import com.mangaverse.app.space.domain.SpaceContentPolicy
import com.mangaverse.app.tracker.domain.CheckNewChaptersUseCase
import com.mangaverse.app.work.domain.WorkAggregate
import com.mangaverse.app.work.domain.WorkAggregateRepository
import com.mangaverse.app.work.domain.WorkIdentity
import com.mangaverse.app.work.domain.WorkMigrationState
import com.mangaverse.app.work.domain.WorkResolver
import javax.inject.Provider

class HistoryRepositoryResumeFilterTest {

	private val workAggregateRepository = mockk<WorkAggregateRepository>()
	private val spaceContentPolicy = mockk<SpaceContentPolicy>()
	private val repository = HistoryRepository(
		db = mockk<MangaDatabase>(relaxed = true),
		settings = mockk<AppSettings>(relaxed = true),
		scrobblers = emptySet<Scrobbler>(),
		mangaRepository = mockk<ContentDataRepository>(relaxed = true),
		localObserver = mockk<HistoryLocalObserver>(relaxed = true),
		newChaptersUseCaseProvider = mockk<Provider<CheckNewChaptersUseCase>>(relaxed = true),
		entityGraphRepository = mockk<EntityGraphRepository>(relaxed = true),
		workResolver = mockk<WorkResolver>(relaxed = true),
		workAggregateRepository = workAggregateRepository,
		spaceContentPolicy = spaceContentPolicy,
	)

	@Test
	fun `adult history is skipped when selecting resume content`() = runTest {
		val adult = content(1L, ContentRating.ADULT)
		val safe = content(2L, ContentRating.SAFE)
		coEvery {
			workAggregateRepository.findRecentHistoryAggregates(any(), null, null)
		} answers {
			listOf(aggregate(adult), aggregate(safe)).take(firstArg())
		}

		assertEquals(safe, repository.getLastOrNull(excludeNsfw = true))
		assertEquals(adult, repository.getLastOrNull(excludeNsfw = false))
	}

	@Test
	fun `resume search continues past a full adult batch`() = runTest {
		val safe = content(100L, ContentRating.SAFE)
		val history = List(32) { index -> aggregate(content(index.toLong(), ContentRating.ADULT)) } + aggregate(safe)
		coEvery {
			workAggregateRepository.findRecentHistoryAggregates(any(), null, null)
		} answers {
			history.take(firstArg())
		}

		assertEquals(safe, repository.getLastOrNull(excludeNsfw = true))
		coVerify(exactly = 1) {
			workAggregateRepository.findRecentHistoryAggregates(32, null, null)
		}
		coVerify(exactly = 1) {
			workAggregateRepository.findRecentHistoryAggregates(64, null, null)
		}
	}

	@Test
	fun `space resume applies adult filtering inside the selected space`() = runTest {
		val safeAnime = content(2L, ContentRating.SAFE, ContentType.VIDEO)
		every { spaceContentPolicy.allowedSourceNames(BuiltInSpaces.Anime) } returns null
		coEvery {
			workAggregateRepository.findRecentHistoryAggregates(any(), BuiltInSpaces.Anime, null)
		} returns listOf(
			aggregate(content(1L, ContentRating.ADULT, ContentType.HENTAI_VIDEO)),
			aggregate(safeAnime),
		)

		assertEquals(
			safeAnime,
			repository.getLastOrNull(spaceId = BuiltInSpaces.Anime, excludeNsfw = true),
		)
	}

	private fun aggregate(content: Content) = WorkAggregate(
		identity = WorkIdentity(
			entityId = null,
			requestedMangaId = content.id,
			preferredMangaId = content.id,
			localMangaIds = setOf(content.id),
			migrationState = WorkMigrationState.VALID,
		),
		displayProjection = content,
		projections = listOf(content),
	)

	private fun content(
		id: Long,
		contentRating: ContentRating,
		contentType: ContentType = ContentType.MANGA,
	) = Content(
		id = id,
		title = "Work $id",
		altTitles = emptySet(),
		url = "/$id",
		publicUrl = "https://example.org/$id",
		rating = RATING_UNKNOWN,
		contentRating = contentRating,
		coverUrl = null,
		tags = emptySet(),
		state = null,
		authors = emptySet(),
		source = TestContentSource(contentType),
	).also { check(it.isNsfw() == (contentRating == ContentRating.ADULT)) }

	private data class TestContentSource(
		override val contentType: ContentType,
	) : ContentSource {
		override val name = "test-$contentType"
		override val locale = ""
	}
}
