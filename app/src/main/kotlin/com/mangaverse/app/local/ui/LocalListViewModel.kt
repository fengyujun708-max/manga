package com.mangaverse.app.local.ui

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import com.mangaverse.app.R
import com.mangaverse.app.core.model.toChipModel
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.ListMode
import com.mangaverse.app.core.ui.widgets.ChipsView
import com.mangaverse.app.core.util.ext.MutableEventFlow
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.core.util.ext.toFileOrNull
import com.mangaverse.app.core.util.ext.toUriOrNull
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.explore.domain.ExploreRepository
import com.mangaverse.app.filter.ui.FilterCoordinator
import com.mangaverse.app.list.domain.ListFilterOption
import com.mangaverse.app.list.domain.ContentListMapper
import com.mangaverse.app.list.domain.QuickFilterListener
import com.mangaverse.app.list.ui.model.EmptyState
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.list.ui.model.ContentListModel
import com.mangaverse.app.list.ui.model.QuickFilter
import com.mangaverse.app.list.ui.model.TipModel
import com.mangaverse.app.local.data.LocalStorageChanges
import com.mangaverse.app.local.data.LocalStorageManager
import com.mangaverse.app.local.domain.DeleteLocalContentUseCase
import com.mangaverse.app.local.domain.model.LocalContent
import com.mangaverse.app.core.model.LocalMangaSource
import com.mangaverse.app.explore.data.SourceAvailabilityRepository
import com.mangaverse.app.explore.ui.model.BrowseGroupTab
import com.mangaverse.app.explore.ui.model.SourceTag
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentTag
import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.remotelist.ui.RemoteListViewModel
import javax.inject.Inject

@HiltViewModel
class LocalListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: ContentRepository.Factory,
	filterCoordinator: FilterCoordinator,
	settings: AppSettings,
	mangaListMapper: ContentListMapper,
	private val deleteLocalContentUseCase: DeleteLocalContentUseCase,
	exploreRepository: ExploreRepository,
	@param:LocalStorageChanges private val localStorageChanges: SharedFlow<LocalContent?>,
	private val localStorageManager: LocalStorageManager,
	sourcesRepository: ContentSourcesRepository,
	sourceAvailabilityRepository: SourceAvailabilityRepository,
	mangaDataRepository: ContentDataRepository,
	private val globalFavoritesState: com.mangaverse.app.favourites.domain.GlobalFavoritesState,
) : RemoteListViewModel(
	savedStateHandle = savedStateHandle,
	mangaRepositoryFactory = mangaRepositoryFactory,
	filterCoordinator = filterCoordinator,
	settings = settings,
	mangaListMapper = mangaListMapper,
	exploreRepository = exploreRepository,
	sourcesRepository = sourcesRepository,
	sourceAvailabilityRepository = sourceAvailabilityRepository,
	mangaDataRepository = mangaDataRepository,
	localStorageChanges = localStorageChanges,
), SharedPreferences.OnSharedPreferenceChangeListener, QuickFilterListener {

	val onContentRemoved = MutableEventFlow<Unit>()

	override val currentGroupTab: StateFlow<BrowseGroupTab> = globalFavoritesState.selectedGroupTab

	init {
		launchJob(Dispatchers.Default) {
			localStorageChanges
				.collect {
					loadList(filterCoordinator.snapshot(), append = false).join()
				}
		}
		settings.subscribe(this)
	}

	override suspend fun onBuildList(list: MutableList<ListModel>) {
		super.onBuildList(list)
		createFilterHeader()?.let {
			list.add(0, it)
		}
		if (!localStorageManager.hasExternalStoragePermission(isReadOnly = true)) {
			for (item in list) {
				if (item !is ContentListModel) {
					continue
				}
				val file = item.manga.url.toUriOrNull()?.toFileOrNull() ?: continue
				if (localStorageManager.isOnExternalStorage(file)) {
					val tip = TipModel(
						key = "permission",
						title = R.string.external_storage,
						text = R.string.missing_storage_permission,
						icon = R.drawable.ic_storage,
						primaryButtonText = R.string.fix,
						secondaryButtonText = R.string.settings,
					)
					list.add(0, tip)
					return
				}
			}
		}
	}

	override fun setFilterOption(option: ListFilterOption, isApplied: Boolean) {
		if (option is ListFilterOption.Tag) {
			filterCoordinator.toggleTag(option.tag, isApplied)
		}
	}

	override fun toggleFilterOption(option: ListFilterOption) {
		if (option is ListFilterOption.Tag) {
			val isSelected = option.tag in filterCoordinator.snapshot().listFilter.tags
			filterCoordinator.toggleTag(option.tag, !isSelected)
		}
	}

	override fun clearFilter() = filterCoordinator.reset()

	/**
	 * 将 BrowseGroupTab（内容类型胶囊）映射到 filterCoordinator 的 ContentType 过滤。
	 * 本地内容支持漫画/小说/视频三种类型，通过 LocalMangaRepository.getList() 的 filter.types 过滤。
	 */
	override fun setSelectedGroupTab(tab: BrowseGroupTab) {
		globalFavoritesState.setSelectedGroupTab(tab)
		val types = when (tab) {
			BrowseGroupTab.Content -> setOf(ContentType.MANGA, ContentType.HENTAI_MANGA)
			BrowseGroupTab.Novel -> setOf(ContentType.NOVEL, ContentType.HENTAI_NOVEL)
			BrowseGroupTab.Video -> setOf(ContentType.VIDEO, ContentType.HENTAI_VIDEO)
			BrowseGroupTab.All -> emptySet()
		}
		// 清除旧的内容类型过滤，再设置新的
		val currentFilter = filterCoordinator.snapshot().listFilter
		val nonTypeFilter = currentFilter.copy(types = emptySet())
		filterCoordinator.set(nonTypeFilter)
		types.forEach { type -> filterCoordinator.toggleContentType(type, isSelected = true) }
	}

	/**
	 * 本地内容全部来自 BUILTIN 来源，来源标签过滤对本地页无实际意义，忽略即可。
	 */
	override fun setSelectedSourceTags(tags: Set<SourceTag>) {
		super.setSelectedSourceTags(tags)
		// 本地内容不按来源标签过滤，不需要桥接到 filterCoordinator
	}

	override fun onCleared() {
		settings.unsubscribe(this)
		super.onCleared()
	}

	override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
		if (key == AppSettings.KEY_LOCAL_MANGA_DIRS) {
			onRefresh()
		}
	}

	fun delete(ids: Set<Long>) {
		launchLoadingJob(Dispatchers.Default) {
			deleteLocalContentUseCase(ids)
			onContentRemoved.call(Unit)
		}
	}

	override suspend fun mapContentList(
		destination: MutableCollection<in ListModel>,
		manga: Collection<Content>,
		mode: ListMode
	) = mangaListMapper.toListModelList(destination, manga, mode, ContentListMapper.NO_SAVED)

	override fun createEmptyState(canResetFilter: Boolean): EmptyState = if (canResetFilter) {
		super.createEmptyState(true)
	} else {
		EmptyState(
			icon = R.drawable.ic_empty_local,
			textPrimary = R.string.text_local_holder_primary,
			textSecondary = R.string.text_local_holder_secondary,
			actionStringRes = R.string._import,
		)
	}

	override fun resolveInitialSource(savedStateHandle: SavedStateHandle): ContentSource {
		return LocalMangaSource
	}

	private suspend fun createFilterHeader(): QuickFilter? {
		val appliedTags = filterCoordinator.snapshot().listFilter.tags
			.sortedBy(ContentTag::title)
		val availableTags = repository.getFilterOptions().availableTags
			.sortedBy(ContentTag::title)
		if (appliedTags.isEmpty() && availableTags.isEmpty()) {
			return null
		}
		val result = ArrayList<ChipsView.ChipModel>(appliedTags.size + availableTags.size)
		appliedTags.mapTo(result) { tag ->
			ListFilterOption.Tag(tag).toChipModel(isChecked = true)
		}
		for (tag in availableTags) {
			if (tag in appliedTags) {
				continue
			}
			result.add(ListFilterOption.Tag(tag).toChipModel(isChecked = false))
		}
		return QuickFilter(result)
	}
}
