package com.mangaverse.app.favourites.domain

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.db.dao.MangaDao
import com.mangaverse.app.core.db.entity.MangaEntity
import com.mangaverse.app.core.db.entity.MangaWithTags
import com.mangaverse.app.entitygraph.data.EntityBindingRecord
import com.mangaverse.app.entitygraph.data.EntityGraphDao
import com.mangaverse.app.entitygraph.data.EntityPrefsRecord
import com.mangaverse.app.entitygraph.data.EntityRecord
import com.mangaverse.app.entitygraph.domain.EntityBindingCreatedBy
import com.mangaverse.app.entitygraph.domain.EntityBindingState
import com.mangaverse.app.entitygraph.domain.EntityType
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.favourites.data.FavouriteCategoriesDao
import com.mangaverse.app.favourites.data.FavouriteCategoryEntity
import com.mangaverse.app.favourites.data.FavouritesDao
import com.mangaverse.app.favourites.data.WorkFavouriteEntity
import com.mangaverse.app.favourites.data.WorkFavouritesDao
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentType

class EntityOrganizeRepositoryTest {

    private val workFavouritesDao = mockk<WorkFavouritesDao>()
    private val favouritesDao = mockk<FavouritesDao>(relaxed = true)
    private val mangaDao = mockk<MangaDao>()
    private val categoriesDao = mockk<FavouriteCategoriesDao>()
    private val entityGraphDao = mockk<EntityGraphDao>()
    private val sourcesRepository = mockk<ContentSourcesRepository>()
    private val db = mockk<MangaDatabase> {
        every { getWorkFavouritesDao() } returns workFavouritesDao
        every { getFavouritesDao() } returns favouritesDao
        every { getMangaDao() } returns mangaDao
        every { getFavouriteCategoriesDao() } returns categoriesDao
        every { getEntityGraphDao() } returns entityGraphDao
    }
    private val repository = EntityOrganizeRepository(db, sourcesRepository)

    @Test
    fun `old favourites table is not an entity organize input`() = runTest {
        coEvery { workFavouritesDao.findActive() } returns emptyList()

        assertTrue(repository.listFavouriteContents().isEmpty())
        assertTrue(repository.listOrganizableWorks().isEmpty())

        verify(exactly = 0) { db.getFavouritesDao() }
    }

    @Test
    fun `work favourites with anchors are exposed as organizable works`() = runTest {
        coEvery { workFavouritesDao.findActive() } returns listOf(
            workFavourite(entityId = 7L, categoryId = 2L, anchorMangaId = 100L),
        )
        coEvery { entityGraphDao.findEntitiesByIds(listOf(7L)) } returns listOf(entity(7L, "Work A"))
        coEvery { entityGraphDao.findEntityPrefsByIds(listOf(7L)) } returns listOf(
            prefs(entityId = 7L, preferredLocalMangaId = 101L),
        )
        coEvery { entityGraphDao.findActiveLocalBindingsByEntities(listOf(7L)) } returns listOf(
            binding(entityId = 7L, mangaId = 100L, state = EntityBindingState.CONFIRMED),
            binding(entityId = 7L, mangaId = 101L, state = EntityBindingState.MANUAL),
        )
        coEvery { mangaDao.findWithTagsByIds(any<Collection<Long>>()) } answers {
            val ids = firstArg<Collection<Long>>()
            listOfNotNull(
                if (100L in ids) mangaWithTags(100L, "Source A", "Anchor") else null,
                if (101L in ids) mangaWithTags(101L, "Source B", "Preferred") else null,
            )
        }
        coEvery { categoriesDao.findByIds(listOf(2L)) } returns listOf(category(2))

        val works = repository.listOrganizableWorks()
        val contents = repository.listFavouriteContents()

        assertEquals(1, works.size)
        assertEquals(7L, works.single().entityId)
        assertEquals(101L, works.single().preferredMangaId)
        assertEquals(setOf(2L), works.single().favouriteCategoryIds)
        assertEquals(setOf(100L, 101L), works.single().projections.mapTo(LinkedHashSet()) { it.mangaId })
        assertEquals(EntityBindingState.MANUAL, works.single().projections.first { it.mangaId == 101L }.bindingState)
        assertEquals(EntityBindingCreatedBy.USER, works.single().projections.first { it.mangaId == 101L }.bindingCreatedBy)
        assertTrue(works.single().projections.first { it.mangaId == 100L }.isFavouriteAnchor)
        assertTrue(works.single().projections.first { it.mangaId == 101L }.isPreferred)
        assertEquals(listOf(100L), contents.map { it.manga.id })

        verify(exactly = 0) { db.getFavouritesDao() }
    }

    private fun workFavourite(
        entityId: Long,
        categoryId: Long,
        anchorMangaId: Long?,
    ): WorkFavouriteEntity {
        return WorkFavouriteEntity(
            entityId = entityId,
            categoryId = categoryId,
            anchorMangaId = anchorMangaId,
            sortKey = 0,
            isPinned = false,
            createdAt = 1L,
            deletedAt = 0L,
            updatedAt = 1L,
        )
    }

    private fun entity(id: Long, title: String): EntityRecord {
        return EntityRecord(
            id = id,
            type = EntityType.WORK.name,
            primaryName = title,
            aliases = null,
            createdAt = 1L,
            lastAccessed = 1L,
            accessCount = 0,
        )
    }

    private fun prefs(entityId: Long, preferredLocalMangaId: Long?): EntityPrefsRecord {
        return EntityPrefsRecord(
            entityId = entityId,
            preferredLocalMangaId = preferredLocalMangaId,
            titleOverride = null,
            coverUrlOverride = null,
            contentRatingOverride = null,
            readingStatus = null,
            metadataSourceKind = null,
            metadataBindingSource = null,
            metadataBindingExternalId = null,
            metadataSourceService = null,
            metadataSourceRemoteId = null,
            updatedAt = 1L,
        )
    }

    private fun binding(
        entityId: Long,
        mangaId: Long,
        state: EntityBindingState,
    ): EntityBindingRecord {
        return EntityBindingRecord(
            entityId = entityId,
            source = "local_manga",
            externalId = mangaId.toString(),
            confidence = 1f,
            isPrimary = true,
            state = state.name,
            createdBy = EntityBindingCreatedBy.USER.name,
            updatedAt = 1L,
        )
    }

    private fun mangaWithTags(id: Long, source: String, title: String): MangaWithTags {
        return MangaWithTags(
            manga = MangaEntity(
                id = id,
                title = title,
                altTitles = null,
                url = "/$id",
                publicUrl = "https://example.org/$id",
                rating = -1f,
                isNsfw = false,
                contentRating = null,
                coverUrl = "",
                largeCoverUrl = null,
                state = null,
                authors = null,
                source = source,
            ),
            tags = emptyList(),
        )
    }

    private fun category(id: Int): FavouriteCategoryEntity {
        return FavouriteCategoryEntity(
            categoryId = id,
            createdAt = 1L,
            sortKey = id,
            title = "Category $id",
            order = "",
            track = false,
            isVisibleInLibrary = true,
            deletedAt = 0L,
        )
    }

    private class TestContentSource(
        override val name: String,
        override val locale: String = "en",
        override val contentType: ContentType = ContentType.MANGA,
    ) : ContentSource
}
