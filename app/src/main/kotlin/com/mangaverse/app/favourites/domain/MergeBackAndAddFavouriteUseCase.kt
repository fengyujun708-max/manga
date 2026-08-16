package com.mangaverse.app.favourites.domain

import dagger.Reusable
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.entitygraph.data.EntityGraphRepository
import com.mangaverse.app.parsers.model.Content
import javax.inject.Inject

@Reusable
class MergeBackAndAddFavouriteUseCase @Inject constructor(
	private val entityGraphRepository: EntityGraphRepository,
	private val favouritesRepository: FavouritesRepository,
	private val contentDataRepository: ContentDataRepository,
) {

	suspend operator fun invoke(
		categoryId: Long,
		content: Content,
		targetEntityId: Long,
	): Boolean {
		val storedContent = contentDataRepository.storeContentAndReturn(content, replaceExisting = false)
		if (!entityGraphRepository.mergeDetachedProjectionBack(storedContent.id, targetEntityId)) {
			return false
		}
		favouritesRepository.addToCategory(categoryId, listOf(storedContent))
		return true
	}
}
