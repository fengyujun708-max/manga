package com.mangaverse.app.tracker.domain

import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.favourites.domain.FavouritesRepository
import com.mangaverse.app.list.domain.ListFilterOption
import com.mangaverse.app.list.domain.ContentListQuickFilter
import javax.inject.Inject

class UpdatesListQuickFilter @Inject constructor(
	private val favouritesRepository: FavouritesRepository,
	settings: AppSettings,
) : ContentListQuickFilter(settings) {

	override suspend fun getAvailableFilterOptions(): List<ListFilterOption> =
		favouritesRepository.getMostUpdatedCategories(
			limit = 4,
		).map {
			ListFilterOption.Favorite(it)
		}
}
