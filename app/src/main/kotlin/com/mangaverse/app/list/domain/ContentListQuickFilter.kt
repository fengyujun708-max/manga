package com.mangaverse.app.list.domain

import androidx.collection.ArraySet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.mangaverse.app.core.model.toChipModel
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.list.ui.model.QuickFilter
import com.mangaverse.app.parsers.util.suspendlazy.getOrNull
import com.mangaverse.app.parsers.util.suspendlazy.suspendLazy

abstract class ContentListQuickFilter(
	private val settings: AppSettings,
) : QuickFilterListener {

	private val appliedFilter = MutableStateFlow<Set<ListFilterOption>>(emptySet())
	private val availableFilterOptions = suspendLazy {
		getAvailableFilterOptions()
	}

	open val appliedOptions
		get() = appliedFilter.asStateFlow()

	override fun setFilterOption(option: ListFilterOption, isApplied: Boolean) {
		appliedFilter.value = ArraySet(appliedFilter.value).also {
			if (isApplied) {
				it.addNoConflicts(option)
			} else {
				it.remove(option)
			}
		}
	}

	override fun toggleFilterOption(option: ListFilterOption) {
		appliedFilter.value = ArraySet(appliedFilter.value).also {
			if (option in it) {
				it.remove(option)
			} else {
				it.addNoConflicts(option)
			}
		}
	}

	override fun clearFilter() {
		appliedFilter.value = emptySet()
	}

	suspend fun filterItem(
		selectedOptions: Set<ListFilterOption>,
	): QuickFilter? {
		if (!settings.isQuickFilterEnabled) {
			return null
		}
		val availableOptions = availableFilterOptions.getOrNull().orEmpty()
		val selectedOptionsOnly = selectedOptions.filterNot { it in availableOptions }
		val chips = buildList {
			selectedOptionsOnly.mapTo(this) { option ->
				option.toChipModel(isChecked = true)
			}
			availableOptions.mapTo(this) { option ->
				option.toChipModel(isChecked = option in selectedOptions)
			}
		}
		return if (chips.isNotEmpty()) {
			createFilterModel(chips)
		} else {
			null
		}
	}

	protected open fun createFilterModel(chips: List<com.mangaverse.app.core.ui.widgets.ChipsView.ChipModel>) =
		QuickFilter(chips)

	protected abstract suspend fun getAvailableFilterOptions(): List<ListFilterOption>

	private fun ArraySet<ListFilterOption>.addNoConflicts(option: ListFilterOption) {
		add(option)
		if (option is ListFilterOption.Inverted) {
			remove(option.option)
		} else {
			removeIf { it is ListFilterOption.Inverted && it.option == option }
		}
	}
}
