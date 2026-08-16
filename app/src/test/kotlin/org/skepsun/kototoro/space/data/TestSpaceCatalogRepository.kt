package com.mangaverse.app.space.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.mangaverse.app.core.jsonsource.SourceType
import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.space.domain.BuiltInSpaces
import com.mangaverse.app.space.domain.SpaceCatalogRepository
import com.mangaverse.app.space.domain.SpaceContext
import com.mangaverse.app.space.domain.SpaceId

internal class TestSpaceCatalogRepository(
    initial: List<SpaceContext> = BuiltInSpaces.contexts,
) : SpaceCatalogRepository {
    private val state = MutableStateFlow(initial)
    override val spaces: StateFlow<List<SpaceContext>> = state
    override val allSpaces: StateFlow<List<SpaceContext>> = state

    override suspend fun create(
        title: String,
        contentTypes: Set<ContentType>,
        sourceLanguages: Set<String>,
        sourceKinds: Set<SourceType>,
    ): SpaceContext = error("Not used")

    override suspend fun update(space: SpaceContext) = Unit
    override suspend fun delete(spaceId: SpaceId) = Unit
}
