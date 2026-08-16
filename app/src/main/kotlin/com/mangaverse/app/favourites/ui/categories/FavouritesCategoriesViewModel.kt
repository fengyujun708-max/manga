package com.mangaverse.app.favourites.ui.categories

import androidx.collection.LongSet
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.mangaverse.app.R
import com.mangaverse.app.core.model.FavouriteCategory
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.observeAsFlow
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.util.ext.requireValue
import com.mangaverse.app.favourites.domain.FavouritesRepository
import com.mangaverse.app.favourites.domain.model.Cover
import com.mangaverse.app.favourites.ui.categories.adapter.AllCategoriesListModel
import com.mangaverse.app.favourites.ui.categories.adapter.CategoryListModel
import com.mangaverse.app.list.ui.model.EmptyState
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.list.ui.model.LoadingState
import javax.inject.Inject

@HiltViewModel
class FavouritesCategoriesViewModel @Inject constructor(
	private val repository: FavouritesRepository,
	private val settings: AppSettings,
	private val dataRepository: ContentDataRepository,
) : BaseViewModel() {

	private var commitJob: Job? = null
	private val isActionsEnabled = MutableStateFlow(true)
	private val displayChanges = dataRepository.observeDisplayPreferencesChanges().map { Unit }

	val content = combine(
		repository.observeCategoriesWithCovers(),
		observeAllCategories(),
		settings.observeAsFlow(AppSettings.KEY_ALL_FAVOURITES_VISIBLE) { isAllFavouritesVisible },
		isActionsEnabled,
		displayChanges,
	) { cats, all, showAll, hasActions, _ ->
		CategoriesUiPayload(
			categories = cats,
			allFavorites = all,
			showAll = showAll,
			hasActions = hasActions,
		)
	}.mapLatest { payload ->
		payload.categories.toUiList(
			allFavorites = payload.allFavorites,
			showAll = payload.showAll,
			hasActions = payload.hasActions,
		)
	}.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	fun deleteCategories(ids: Set<Long>) {
		launchJob(Dispatchers.Default) {
			repository.removeCategories(ids)
		}
	}

	fun setAllCategoriesVisible(isVisible: Boolean) {
		settings.isAllFavouritesVisible = isVisible
	}

	fun isEmpty(): Boolean = content.value.none { it is CategoryListModel }

	fun saveOrder(snapshot: List<ListModel>) {
		val prevJob = commitJob
		commitJob = launchJob {
			prevJob?.cancelAndJoin()
			val ids = snapshot.mapNotNullTo(ArrayList(snapshot.size)) {
				(it as? CategoryListModel)?.category?.id
			}
			if (ids.isNotEmpty()) {
				repository.reorderCategories(ids)
			}
		}
	}

	fun setIsVisible(ids: Set<Long>, isVisible: Boolean) {
		launchJob(Dispatchers.Default) {
			for (id in ids) {
				repository.updateCategory(id, isVisible)
			}
		}
	}

	fun setActionsEnabled(value: Boolean) {
		isActionsEnabled.value = value
	}

	fun getCategories(ids: LongSet): ArrayList<FavouriteCategory> {
		val items = content.requireValue()
		return items.mapNotNullTo(ArrayList(ids.size)) { item ->
			(item as? CategoryListModel)?.category?.takeIf { it.id in ids }
		}
	}

	private suspend fun Map<FavouriteCategory, List<Cover>>.toUiList(
		allFavorites: Pair<Int, List<Cover>>,
		showAll: Boolean,
		hasActions: Boolean,
	): List<ListModel> {
		if (isEmpty()) {
			return listOf(
				EmptyState(
					icon = R.drawable.ic_empty_favourites,
					textPrimary = R.string.text_empty_holder_primary,
					textSecondary = R.string.empty_favourite_categories,
					actionStringRes = 0,
				),
			)
		}
		val result = ArrayList<ListModel>(size + 1)
		val allCovers = resolveDisplayCovers(allFavorites.second)
		result.add(
			AllCategoriesListModel(
				mangaCount = allFavorites.first,
				covers = allCovers,
				isVisible = showAll,
				isActionsEnabled = hasActions,
			),
		)
		mapTo(result) { (category, covers) ->
			CategoryListModel(
				mangaCount = covers.size,
				covers = resolveDisplayCovers(covers.take(3)),
				category = category,
				isActionsEnabled = hasActions,
				isTrackerEnabled = settings.isTrackerEnabled && AppSettings.TRACK_FAVOURITES in settings.trackSources,
			)
		}
		return result
	}

	private fun observeAllCategories(): Flow<Pair<Int, List<Cover>>> {
		return settings.observeAsFlow(AppSettings.KEY_FAVORITES_ORDER) {
			allFavoritesSortOrder
		}.mapLatest { order ->
			repository.getAllFavoritesCovers(order, limit = 3)
		}.combine(repository.observeContentCount()) { covers, count ->
			count to covers
		}
	}

	private suspend fun resolveDisplayCovers(covers: List<Cover>): List<Cover> {
		return covers
	}

	private data class CategoriesUiPayload(
		val categories: Map<FavouriteCategory, List<Cover>>,
		val allFavorites: Pair<Int, List<Cover>>,
		val showAll: Boolean,
		val hasActions: Boolean,
	)
}
