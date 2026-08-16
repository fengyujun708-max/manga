package com.mangaverse.app.settings.nav

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.mangaverse.app.R
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.MAX_MAIN_NAV_ITEM_COUNT
import com.mangaverse.app.core.prefs.NavItem
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.ui.util.ActivityRecreationHandle
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.main.ui.MainActivity
import com.mangaverse.app.parsers.util.move
import com.mangaverse.app.settings.nav.model.NavItemAddModel
import com.mangaverse.app.settings.nav.model.NavItemConfigModel
import javax.inject.Inject

@HiltViewModel
class NavConfigViewModel @Inject constructor(
	private val settings: AppSettings,
	private val activityRecreationHandle: ActivityRecreationHandle,
) : BaseViewModel() {

	private val items = MutableStateFlow(settings.mainNavItems)

	val configuredItems: StateFlow<List<NavItemConfigModel>> = items.map { snapshot ->
		snapshot.map {
			NavItemConfigModel(it, getUnavailabilityHint(it))
		}
	}.stateIn(
		viewModelScope + Dispatchers.Default,
		SharingStarted.WhileSubscribed(5000),
		emptyList(),
	)

	val availableItems: StateFlow<List<NavItem>> = items.map { snapshot ->
		NavItem.entries.filterNot { item -> item in snapshot || item == NavItem.DISCOVER }
	}.stateIn(
		viewModelScope + Dispatchers.Default,
		SharingStarted.WhileSubscribed(5000),
		emptyList(),
	)

	val canShowAddAction: StateFlow<Boolean> = items.map { snapshot ->
		snapshot.size < NavItem.entries.size
	}.stateIn(
		viewModelScope + Dispatchers.Default,
		SharingStarted.WhileSubscribed(5000),
		false,
	)

	val canAddAction: StateFlow<Boolean> = items.map { snapshot ->
		snapshot.size < MAX_MAIN_NAV_ITEM_COUNT
	}.stateIn(
		viewModelScope + Dispatchers.Default,
		SharingStarted.WhileSubscribed(5000),
		false,
	)

	val content: StateFlow<List<ListModel>> = items.map { snapshot ->
		buildList(snapshot.size + 1) {
			snapshot.mapTo(this) {
				NavItemConfigModel(it, getUnavailabilityHint(it))
			}
			if (size < NavItem.entries.size) {
				add(NavItemAddModel(size < MAX_MAIN_NAV_ITEM_COUNT))
			}
		}
	}.stateIn(
		viewModelScope + Dispatchers.Default,
		SharingStarted.WhileSubscribed(5000),
		emptyList(),
	)

	private var commitJob: Job? = null

	fun reorder(fromPos: Int, toPos: Int) {
		items.value = items.value.toMutableList().apply {
			move(fromPos, toPos)
			commit(this)
		}
	}

	fun moveUp(index: Int) {
		if (index <= 0) return
		reorder(index, index - 1)
	}

	fun moveDown(index: Int) {
		if (index >= items.value.lastIndex) return
		reorder(index, index + 1)
	}

	fun addItem(item: NavItem) {
		if (items.value.size >= MAX_MAIN_NAV_ITEM_COUNT || item in items.value) return
		items.value = items.value.plus(item).also {
			commit(it)
		}
	}

	fun removeItem(item: NavItem) {
		val newList = items.value.toMutableList()
		newList.remove(item)
		if (newList.isEmpty()) {
			newList.add(NavItem.HOME)
		}
		items.value = newList
		commit(newList)
	}

	private fun commit(value: List<NavItem>) {
		val prevJob = commitJob
		commitJob = launchJob {
			prevJob?.cancelAndJoin()
			delay(500)
			settings.mainNavItems = value
			activityRecreationHandle.recreate(MainActivity::class.java)
		}
	}

	private fun getUnavailabilityHint(item: NavItem) = if (item.isAvailable(settings)) {
		0
	} else when (item) {
		NavItem.FEED -> R.string.check_for_new_chapters_disabled
		NavItem.SUGGESTIONS -> R.string.suggestions_unavailable_text
		else -> 0
	}
}
