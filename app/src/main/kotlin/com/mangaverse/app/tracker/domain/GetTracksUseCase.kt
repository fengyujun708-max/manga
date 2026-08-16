package com.mangaverse.app.tracker.domain

import com.mangaverse.app.tracker.domain.model.ContentTracking
import javax.inject.Inject

class GetTracksUseCase @Inject constructor(
	private val repository: TrackingRepository,
) {

	suspend operator fun invoke(limit: Int): List<ContentTracking> {
		repository.updateTracks()
		return repository.getTracks(offset = 0, limit = limit)
	}
}
