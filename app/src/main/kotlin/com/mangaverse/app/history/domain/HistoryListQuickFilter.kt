package com.mangaverse.app.history.domain

import com.mangaverse.app.core.os.NetworkState
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.model.toChipModel
import com.mangaverse.app.history.data.HistoryRepository
import com.mangaverse.app.list.domain.ListFilterOption
import com.mangaverse.app.list.domain.ContentListQuickFilter
import com.mangaverse.app.list.ui.model.QuickFilter
import javax.inject.Inject

class HistoryListQuickFilter @Inject constructor(
	private val settings: AppSettings,
	private val repository: HistoryRepository,
	networkState: NetworkState,
) : ContentListQuickFilter(settings) {

	init {
		setFilterOption(ListFilterOption.Downloaded, !networkState.value)
	}

	override suspend fun getAvailableFilterOptions(): List<ListFilterOption> = buildList {
		add(ListFilterOption.Downloaded)
		if (settings.isTrackerEnabled) {
			add(ListFilterOption.Macro.NEW_CHAPTERS)
		}
		add(ListFilterOption.Macro.COMPLETED)
		add(ListFilterOption.Macro.FAVORITE)
		add(ListFilterOption.NOT_FAVORITE)
		if (!settings.isHistoryExcludeNsfw) {
			add(ListFilterOption.Macro.NSFW)
		}
		repository.getPopularTags(Int.MAX_VALUE).mapTo(this) {
			ListFilterOption.Tag(it)
		}
		repository.getPopularSources(Int.MAX_VALUE).mapTo(this) {
			ListFilterOption.Source(it)
		}
	}

	fun previewFilterItem(
		selectedOptions: Set<ListFilterOption>,
	): QuickFilter? {
		if (!settings.isQuickFilterEnabled) {
			return null
		}
		val chips = buildList {
			selectedOptions.mapTo(this) { option ->
				option.toChipModel(isChecked = true)
			}
			add(ListFilterOption.Downloaded.toChipModel(isChecked = ListFilterOption.Downloaded in selectedOptions))
			if (settings.isTrackerEnabled) {
				add(
					ListFilterOption.Macro.NEW_CHAPTERS.toChipModel(
						isChecked = ListFilterOption.Macro.NEW_CHAPTERS in selectedOptions,
					),
				)
			}
			add(ListFilterOption.Macro.COMPLETED.toChipModel(isChecked = ListFilterOption.Macro.COMPLETED in selectedOptions))
			add(ListFilterOption.Macro.FAVORITE.toChipModel(isChecked = ListFilterOption.Macro.FAVORITE in selectedOptions))
			add(ListFilterOption.NOT_FAVORITE.toChipModel(isChecked = ListFilterOption.NOT_FAVORITE in selectedOptions))
			if (!settings.isHistoryExcludeNsfw) {
				add(ListFilterOption.Macro.NSFW.toChipModel(isChecked = ListFilterOption.Macro.NSFW in selectedOptions))
			}
		}.distinctBy { chip ->
			chip.data ?: "${chip.titleResId}:${chip.title}"
		}
		return if (chips.isNotEmpty()) {
			QuickFilter(chips)
		} else {
			null
		}
	}
}
