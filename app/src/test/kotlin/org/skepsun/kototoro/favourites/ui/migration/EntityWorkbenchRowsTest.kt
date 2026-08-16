package com.mangaverse.app.favourites.ui.migration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.mangaverse.app.entitygraph.domain.EntityBindingCreatedBy
import com.mangaverse.app.entitygraph.domain.EntityBindingState
import com.mangaverse.app.favourites.domain.MergeCandidateGroup
import com.mangaverse.app.favourites.domain.MergeCandidateItem
import com.mangaverse.app.favourites.domain.OrganizableWork
import com.mangaverse.app.favourites.domain.WorkProjection
import com.mangaverse.app.favourites.ui.migration.compose.buildEntityWorkbenchRows
import com.mangaverse.app.parsers.model.ContentType

class EntityWorkbenchRowsTest {

    @Test
    fun `merge preview does not hide new work entities outside preview scope`() {
        val rows = buildEntityWorkbenchRows(
            MigrationUiState(
                mergeCandidateGroups = listOf(
                    mergeGroup(id = "preview", mangaIds = setOf(1L, 2L)),
                ),
                organizableWorks = listOf(
                    work(entityId = 10L, mangaIds = listOf(1L, 2L)),
                    work(entityId = 20L, mangaIds = listOf(3L)),
                ),
            ),
        )

        assertEquals(listOf("preview", "work:20"), rows.map { it.group.id })
    }

    private fun mergeGroup(
        id: String,
        mangaIds: Set<Long>,
    ): MergeCandidateGroup {
        return MergeCandidateGroup(
            id = id,
            title = id,
            normalizedTitle = id,
            contentType = ContentType.MANGA,
            mangaIds = mangaIds,
            items = mangaIds.map { mangaId ->
                MergeCandidateItem(
                    mangaId = mangaId,
                    title = "Title $mangaId",
                    normalizedTitle = "title $mangaId",
                    sourceName = "source$mangaId",
                    coverUrl = null,
                    score = 1f,
                )
            },
            matchScore = 1f,
            isExactMatch = true,
        )
    }

    private fun work(
        entityId: Long,
        mangaIds: List<Long>,
    ): OrganizableWork {
        return OrganizableWork(
            entityId = entityId,
            title = "Work $entityId",
            preferredMangaId = mangaIds.firstOrNull(),
            favouriteCategoryIds = emptySet(),
            projections = mangaIds.map { mangaId ->
                WorkProjection(
                    mangaId = mangaId,
                    source = "source$mangaId",
                    title = "Title $mangaId",
                    bindingState = EntityBindingState.CONFIRMED,
                    bindingCreatedBy = EntityBindingCreatedBy.USER,
                    isPreferred = mangaId == mangaIds.first(),
                    isFavouriteAnchor = false,
                )
            },
        )
    }
}
