package com.mangaverse.app.work.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.favourites.data.WorkFavouritesDao
import com.mangaverse.app.history.data.WorkHistoryDao
import com.mangaverse.app.list.domain.ListSortOrder
import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.space.domain.BuiltInSpaces
import com.mangaverse.app.space.domain.DefaultSpaceContentPolicy
import com.mangaverse.app.explore.data.SourceRuleResolver
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.space.data.TestSpaceCatalogRepository

class WorkAggregateSpaceQueryTest {

	private val favouritesDao = mockk<WorkFavouritesDao>()
	private val historyDao = mockk<WorkHistoryDao>()
	private val db = mockk<MangaDatabase> {
		every { getWorkFavouritesDao() } returns favouritesDao
		every { getWorkHistoryDao() } returns historyDao
	}
	private val repository = WorkAggregateRepository(
		db = db,
		workResolver = mockk(),
		spaceContentPolicy = DefaultSpaceContentPolicy(
			TestSpaceCatalogRepository(),
			mockk<SourceRuleResolver>(relaxed = true),
		),
		contentSourcesRepository = mockk<ContentSourcesRepository>(),
	)

	@Test
	fun `favourite query sends manga scope to dao before limiting`() = runTest {
		coEvery {
			favouritesDao.findActiveForSpace(
				categoryId = null,
				allowedTypes = any(),
				classifiedTypes = any(),
				oldestFirst = false,
				limit = 12,
			)
		} returns emptyList()

		assertEquals(
			emptyList<WorkAggregate>(),
			repository.findFavouriteAggregates(
				order = ListSortOrder.NEWEST,
				limit = 3,
				spaceId = BuiltInSpaces.Manga,
			),
		)
		coVerify {
			favouritesDao.findActiveForSpace(
				categoryId = null,
				allowedTypes = match { types ->
					ContentType.MANGA.name in types && ContentType.NOVEL.name !in types
				},
				classifiedTypes = match { types ->
					ContentType.MANGA.name in types && ContentType.NOVEL.name in types &&
						ContentType.VIDEO.name in types && ContentType.OTHER.name !in types
				},
				oldestFirst = false,
				limit = 12,
			)
		}
	}

	@Test
	fun `history query sends only target space types to dao`() = runTest {
		coEvery {
			historyDao.findRecentForSpace(any(), any(), 5)
		} returns emptyList()

		assertEquals(
			emptyList<WorkAggregate>(),
			repository.findRecentHistoryAggregates(limit = 5, spaceId = BuiltInSpaces.Anime),
		)
		coVerify {
			historyDao.findRecentForSpace(
				allowedTypes = match { it == setOf(ContentType.VIDEO.name, ContentType.HENTAI_VIDEO.name) },
				classifiedTypes = match { ContentType.OTHER.name !in it },
				limit = 5,
			)
		}
	}

	@Test
	fun `history query uses refreshed external source scope`() = runTest {
		coEvery {
			historyDao.findRecentForSpaceAndSources(any(), any(), setOf("MIHON_123"), 1)
		} returns emptyList()

		assertEquals(
			emptyList<WorkAggregate>(),
			repository.findRecentHistoryAggregates(
				limit = 1,
				spaceId = BuiltInSpaces.Manga,
				allowedSourceNames = setOf("MIHON_123"),
			),
		)
		coVerify {
			historyDao.findRecentForSpaceAndSources(
				allowedTypes = match { ContentType.MANGA.name in it },
				classifiedTypes = match { ContentType.OTHER.name !in it },
				allowedSources = setOf("MIHON_123"),
				limit = 1,
			)
		}
	}
}
