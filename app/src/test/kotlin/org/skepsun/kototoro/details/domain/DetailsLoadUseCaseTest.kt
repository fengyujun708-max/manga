package com.mangaverse.app.details.domain

import android.text.Html
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.mangaverse.app.core.model.TestContentSource
import com.mangaverse.app.core.nav.ContentIntent
import com.mangaverse.app.core.os.NetworkState
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.details.data.ContentDetails
import com.mangaverse.app.explore.domain.RecoverContentUseCase
import com.mangaverse.app.local.data.LocalMangaRepository
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter

class DetailsLoadUseCaseTest {

	private val dataRepository = mockk<ContentDataRepository>()
	private val localContentRepository = mockk<LocalMangaRepository>()
	private val repositoryFactory = mockk<ContentRepository.Factory>()
	private val recoverUseCase = mockk<RecoverContentUseCase>()
	private val networkState = mockk<NetworkState>()
	private val database = mockk<MangaDatabase>(relaxed = true)
	private val useCase = DetailsLoadUseCase(
		mangaDataRepository = dataRepository,
		localContentRepository = localContentRepository,
		mangaRepositoryFactory = repositoryFactory,
		recoverUseCase = recoverUseCase,
		imageGetter = mockk<Html.ImageGetter>(relaxed = true),
		networkState = networkState,
		mangaDatabase = database,
	)

	@Test
	fun `initial seed keeps reading source before network details arrive`() = runTest {
		val seed = content(id = 1L, title = "Seed")
		val remote = content(id = 1L, title = "Remote")
		val repository = mockk<ContentRepository>()
		coEvery { dataRepository.resolveIntent(any(), withChapters = true) } returns seed
		coEvery { dataRepository.resolveStoredProjection(seed) } returns seed
		coEvery { dataRepository.getOverride(seed.id) } returns null
		coEvery { localContentRepository.findSavedContent(seed, withDetails = true) } returns null
		coEvery { dataRepository.findContentById(seed.id, withChapters = true) } returns null
		every { networkState.isOfflineOrRestricted() } returns false
		every { repositoryFactory.create(TestContentSource) } returns repository
		coEvery { repository.getDetails(seed) } returns remote
		coEvery { dataRepository.updateProjectionSnapshot(remote) } returns remote

		val emissions = useCase(ContentIntent.of(seed), force = false).toList()

		assertEquals(listOf("Seed", "Remote"), emissions.map { it.toContent().title })
		assertEquals(listOf("TEST", "TEST"), emissions.map { it.toContent().source.name })
	}

	@Test
	fun `complete cached snapshot retains its source identity`() {
		val cached = content(id = 3L, title = "Cached", description = "cached description")

		assertTrue(cached.hasCompleteDetailsSnapshot())
		assertEquals(TestContentSource.name, cached.source.name)
	}

	@Test
	fun `refresh stores and emits the network source`() = runTest {
		val seed = content(id = 2L, title = "Seed")
		val remote = content(id = 2L, title = "Remote")
		val repository = mockk<ContentRepository>()
		coEvery { dataRepository.resolveIntent(any(), withChapters = true) } returns seed
		coEvery { dataRepository.resolveStoredProjection(seed) } returns seed
		coEvery { dataRepository.getOverride(seed.id) } returns null
		coEvery { localContentRepository.findSavedContent(seed, withDetails = true) } returns null
		every { repositoryFactory.create(TestContentSource) } returns repository
		coEvery { repository.getDetails(seed) } returns remote
		coEvery { dataRepository.updateProjectionSnapshot(remote) } returns remote

		val emissions = useCase(ContentIntent.of(seed), force = true).toList()

		assertSame(remote.source, emissions.last().toContent().source)
		coVerify(exactly = 1) { dataRepository.updateProjectionSnapshot(remote) }
	}

	private fun content(id: Long, title: String, description: String? = null): Content {
		return Content(
			id = id,
			title = title,
			altTitles = emptySet(),
			url = "/$id",
			publicUrl = "https://example.org/$id",
			rating = 0f,
			contentRating = null,
			coverUrl = null,
			largeCoverUrl = null,
			tags = emptySet(),
			state = null,
			authors = emptySet(),
			description = description,
			chapters = listOf(
				ContentChapter(
					id = id * 10,
					title = "Chapter 1",
					number = 1f,
					volume = 0,
					url = "/chapter/$id",
					scanlator = null,
					uploadDate = 0L,
					branch = null,
					source = TestContentSource,
				),
			),
			source = TestContentSource,
		)
	}
}
