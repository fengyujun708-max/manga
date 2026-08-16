package com.mangaverse.app.space.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.db.dao.MangaDao
import com.mangaverse.app.core.model.LocalMangaSource
import com.mangaverse.app.core.model.TestContentSource
import com.mangaverse.app.core.model.UnknownContentSource
import com.mangaverse.app.parsers.model.ContentType

class ProjectionContentTypeBackfillTest {

	private val dao = mockk<MangaDao>()
	private val db = mockk<MangaDatabase> {
		every { getMangaDao() } returns dao
	}
	private val backfill = ProjectionContentTypeBackfill(db)

	@Test
	fun `backfill writes resolved content types`() = runTest {
		coEvery {
			dao.findMissingContentTypes(any(), 10)
		} returns listOf(
			MangaDao.MissingContentTypeProjection(1L, LocalMangaSource.name),
			MangaDao.MissingContentTypeProjection(2L, TestContentSource.name),
		)
		coEvery { dao.setContentTypeIfMissing(any(), any()) } returns 1

		val updated = backfill.backfill(
			resolvedSources = listOf(LocalMangaSource, TestContentSource),
			limit = 10,
		)

		assertEquals(2, updated)
		coVerify { dao.setContentTypeIfMissing(1L, ContentType.MANGA.name) }
		coVerify { dao.setContentTypeIfMissing(2L, ContentType.OTHER.name) }
	}

	@Test
	fun `unresolved sources are not queried or classified`() = runTest {
		val updated = backfill.backfill(listOf(UnknownContentSource), limit = 10)

		assertEquals(0, updated)
		coVerify(exactly = 0) { dao.findMissingContentTypes(any(), any()) }
		coVerify(exactly = 0) { dao.setContentTypeIfMissing(any(), any()) }
	}

	@Test
	fun `conditional update result makes repeated backfill idempotent`() = runTest {
		coEvery {
			dao.findMissingContentTypes(any(), 10)
		} returns listOf(MangaDao.MissingContentTypeProjection(1L, LocalMangaSource.name))
		coEvery { dao.setContentTypeIfMissing(1L, ContentType.MANGA.name) } returns 0

		assertEquals(0, backfill.backfill(listOf(LocalMangaSource), limit = 10))
	}

	@Test
	fun `bulk backfill updates each content type without per projection writes`() = runTest {
		coEvery { dao.setContentTypeIfMissingForSources(any(), any()) } returns 2

		val updated = backfill.backfillAll(
			listOf(LocalMangaSource, TestContentSource),
		)

		assertEquals(4, updated)
		coVerify {
			dao.setContentTypeIfMissingForSources(listOf(LocalMangaSource.name), ContentType.MANGA.name)
			dao.setContentTypeIfMissingForSources(listOf(TestContentSource.name), ContentType.OTHER.name)
		}
		coVerify(exactly = 0) { dao.setContentTypeIfMissing(any(), any()) }
	}
}
