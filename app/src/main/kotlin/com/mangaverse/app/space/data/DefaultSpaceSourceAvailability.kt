package com.mangaverse.app.space.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.space.domain.SpaceSourceAvailability
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSpaceSourceAvailability @Inject constructor(
	private val contentSourcesRepository: ContentSourcesRepository,
) : SpaceSourceAvailability {

	override suspend fun isAvailable(sourceName: String): Boolean = withContext(Dispatchers.IO) {
		contentSourcesRepository.isSourceAvailable(sourceName)
	}
}
