package com.mangaverse.app.list.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.plus
import com.mangaverse.app.core.model.FavouriteCategory
import com.mangaverse.app.explore.ui.model.BrowseGroupTab
import com.mangaverse.app.explore.ui.model.SourceTag
import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.ListMode
import com.mangaverse.app.core.prefs.observeAsFlow
import com.mangaverse.app.core.prefs.observeAsStateFlow
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.ui.util.ReversibleAction
import com.mangaverse.app.core.util.ext.MutableEventFlow
import com.mangaverse.app.list.domain.ListFilterOption
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.local.data.LocalStorageChanges
import com.mangaverse.app.local.domain.model.LocalContent

abstract class ContentListViewModel(
	protected val settings: AppSettings,
	private val mangaDataRepository: ContentDataRepository,
	@param:LocalStorageChanges private val localStorageChanges: SharedFlow<LocalContent?>,
) : BaseViewModel() {

	abstract val content: StateFlow<List<ListModel>>
	open val hasMoreItems: StateFlow<Boolean> = flowOf(true)
		.stateIn(viewModelScope, SharingStarted.Eagerly, true)
	open val listMode = settings.observeAsFlow(AppSettings.KEY_LIST_MODE) { listMode }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, settings.listMode)
	val onActionDone = MutableEventFlow<ReversibleAction>()
	val onContentMessage = MutableEventFlow<String>()
	val onContentActionHostRequest = MutableEventFlow<ContentActionHostRequest>()
	val gridScale = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_GRID_SIZE,
		valueProducer = { gridSize / 100f },
	)

	/**
	 * Currently selected browse group tab (Content Type)
	 */
	protected val selectedGroupTab = MutableStateFlow<BrowseGroupTab>(BrowseGroupTab.All)
	open val currentGroupTab: StateFlow<BrowseGroupTab> get() = selectedGroupTab

	/**
	 * Currently selected source tags (Source Origin)
	 */
	protected val selectedSourceTags = MutableStateFlow<Set<SourceTag>>(emptySet())
	open val currentSourceTags: StateFlow<Set<SourceTag>> get() = selectedSourceTags

	/**
	 * Currently selected category IDs
	 */
	protected val selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
	val currentCategoryIds: StateFlow<Set<Long>> = selectedCategoryIds

	/**
	 * Available categories for filtering
	 */
	open val availableCategories: StateFlow<List<FavouriteCategory>> = flowOf(emptyList<FavouriteCategory>())
		.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

	/**
	 * Whether the filter bar should be shown
	 */
	open val isFilterBarVisible: StateFlow<Boolean> = flowOf(false).stateIn(viewModelScope, SharingStarted.Eagerly, false)

	open fun setSelectedGroupTab(tab: BrowseGroupTab) {
		selectedGroupTab.value = tab
	}

	open fun setSelectedSourceTags(tags: Set<SourceTag>) {
		selectedSourceTags.value = tags
	}

	open fun setSelectedCategoryIds(ids: Set<Long>) {
		selectedCategoryIds.value = ids
	}

	open fun resolveEntityIdForUiItemId(id: Long): Long? = null

	open fun resolvePreferredLocalMangaIdForUiItemId(id: Long): Long? = null

	val isIncognitoModeEnabled: Boolean
		get() = settings.isIncognitoModeEnabled

	abstract fun onRefresh()

	abstract fun onRetry()

	open fun onContentClick(content: Content): Boolean = false

	protected fun List<Content>.skipNsfwIfNeeded() = if (settings.isNsfwContentDisabled) {
		filterNot { it.isNsfw() }
	} else {
		this
	}

	protected fun Flow<Set<ListFilterOption>>.combineWithSettings(): Flow<Set<ListFilterOption>> = combine(
		settings.observeAsFlow(AppSettings.KEY_DISABLE_NSFW) { isNsfwContentDisabled },
	) { filters, skipNsfw ->
		if (skipNsfw) {
			filters + ListFilterOption.SFW
		} else {
			filters
		}
	}

	protected fun observeListModeWithTriggers(): Flow<ListMode> = combine(
		listMode,
		merge(
			mangaDataRepository.observeOverridesTrigger(emitInitialState = true).map { Unit },
			mangaDataRepository.observeFavoritesTrigger(emitInitialState = true).map { Unit },
			localStorageChanges.onStart { emit(null) }.map { Unit },
		),
		settings.observeChanges().filter { key ->
			key == AppSettings.KEY_PROGRESS_INDICATORS
				|| key == AppSettings.KEY_TRACKER_ENABLED
				|| key == AppSettings.KEY_QUICK_FILTER
				|| key == AppSettings.KEY_MANGA_LIST_BADGES
		}.onStart { emit("") },
	) { mode, _, _ ->
		mode
	}
}

fun interface ContentActionHostRequest {

	fun execute(onComplete: () -> Unit)
}
