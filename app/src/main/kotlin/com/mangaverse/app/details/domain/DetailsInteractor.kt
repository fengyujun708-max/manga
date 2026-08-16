package com.mangaverse.app.details.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.mangaverse.app.core.model.FavouriteCategory
import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.TriStateOption
import com.mangaverse.app.core.prefs.observeAsFlow
import com.mangaverse.app.details.data.ContentDetails
import com.mangaverse.app.favourites.domain.FavouritesRepository
import com.mangaverse.app.history.data.HistoryRepository
import com.mangaverse.app.local.data.LocalMangaRepository
import com.mangaverse.app.local.domain.model.LocalContent
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.util.runCatchingCancellable
import com.mangaverse.app.tracker.domain.TrackingRepository
import javax.inject.Inject

/* TODO: remove */
class DetailsInteractor @Inject constructor(
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val localContentRepository: LocalMangaRepository,
	private val trackingRepository: TrackingRepository,
	private val settings: AppSettings,
) {

	fun observeFavourite(mangaId: Long): Flow<Set<FavouriteCategory>> {
		return favouritesRepository.observeCategoriesByWork(mangaId)
	}

	fun observeNewChapters(mangaId: Long): Flow<Int> {
		return settings.observeAsFlow(AppSettings.KEY_TRACKER_ENABLED) { isTrackerEnabled }
			.flatMapLatest { isEnabled ->
				if (isEnabled) {
					trackingRepository.observeNewChaptersCount(mangaId)
				} else {
					flowOf(0)
				}
			}
	}

	fun observeIncognitoMode(mangaFlow: Flow<Content?>): Flow<TriStateOption> {
		return mangaFlow
			.filterNotNull()
			.distinctUntilChangedBy { it.isNsfw() }
			.combine(observeIncognitoMode()) { manga, globalIncognito ->
				when {
					globalIncognito -> TriStateOption.ENABLED
					manga.isNsfw() -> settings.incognitoModeForNsfw
					else -> TriStateOption.DISABLED
				}
			}
	}

	suspend fun updateLocal(subject: ContentDetails?, localContent: LocalContent): ContentDetails? {
		subject ?: return null
		return if (subject.id == localContent.manga.id) {
			if (subject.isLocal) {
				subject.copy(
					manga = localContent.manga,
				)
			} else {
				subject.copy(
					localContent = runCatchingCancellable {
						localContent.copy(
							manga = localContentRepository.getDetails(localContent.manga),
						)
					}.getOrNull() ?: subject.local,
				)
			}
		} else {
			subject
		}
	}

	suspend fun findRemote(seed: Content) = localContentRepository.getRemoteContent(seed)

	private fun observeIncognitoMode() = settings.observeAsFlow(AppSettings.KEY_INCOGNITO_MODE) {
		isIncognitoModeEnabled
	}
}
