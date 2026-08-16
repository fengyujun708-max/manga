package com.mangaverse.app.remotelist.ui

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import com.mangaverse.app.R
import com.mangaverse.app.core.model.ContentSource
import com.mangaverse.app.core.model.GlobalTagBlacklist
import com.mangaverse.app.parsers.model.ContentSource as ParserContentSource
import com.mangaverse.app.core.model.distinctById
import com.mangaverse.app.core.model.isLocal
import com.mangaverse.app.core.parser.ContentActionRepository
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.ListMode
import com.mangaverse.app.core.prefs.observeAsFlow
import com.mangaverse.app.core.util.ext.MutableEventFlow
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.core.util.ext.getCauseUrl
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.explore.data.SourceAvailabilityRepository
import com.mangaverse.app.explore.domain.ExploreRepository
import com.mangaverse.app.filter.ui.FilterCoordinator
import com.mangaverse.app.list.domain.ContentListMapper
import com.mangaverse.app.list.ui.ContentListViewModel
import com.mangaverse.app.list.ui.ContentActionHostRequest
import com.mangaverse.app.list.ui.model.ButtonFooter
import com.mangaverse.app.list.ui.model.EmptyState
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.list.ui.model.LoadingFooter
import com.mangaverse.app.list.ui.model.LoadingState
import com.mangaverse.app.list.ui.model.toErrorFooter
import com.mangaverse.app.list.ui.model.toErrorState
import com.mangaverse.app.local.data.LocalStorageChanges
import com.mangaverse.app.local.domain.model.LocalContent
import com.mangaverse.app.parsers.model.Content

import com.mangaverse.app.parsers.util.sizeOrZero
import javax.inject.Inject

private const val FILTER_MIN_INTERVAL = 250L
private const val RemoteListPaginationLogTag = "RemoteListPagination"

@HiltViewModel
open class RemoteListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	mangaRepositoryFactory: ContentRepository.Factory,
	final override val filterCoordinator: FilterCoordinator,
	settings: AppSettings,
	protected val mangaListMapper: ContentListMapper,
	private val exploreRepository: ExploreRepository,
	sourcesRepository: ContentSourcesRepository,
	private val sourceAvailabilityRepository: SourceAvailabilityRepository,
	mangaDataRepository: ContentDataRepository,
	@LocalStorageChanges localStorageChanges: SharedFlow<LocalContent?>,
) : ContentListViewModel(settings, mangaDataRepository, localStorageChanges), FilterCoordinator.Owner {

	private val initialSource = resolveInitialSource(savedStateHandle)
	val isRandomLoading = MutableStateFlow(false)
	val onOpenContent = MutableEventFlow<Content>()
    val onSourceBroken = MutableEventFlow<Unit>()

	protected val repository = mangaRepositoryFactory.create(initialSource)
	val source: com.mangaverse.app.parsers.model.ContentSource = repository.source
	private val mangaList = MutableStateFlow<List<Content>?>(null)
	private val hasNextPage = MutableStateFlow(false)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null
	private var randomJob: Job? = null
	private var contentActionJob: Job? = null
	private var lastLoadedPageIndex: Int = -1
	private var lastRequestedPageIndex: Int = 0

	override val content = combine(
		combine(
			mangaList,
			settings.observeAsFlow(AppSettings.KEY_GLOBAL_TAG_BLACKLIST) { globalTagBlacklist },
		) { list, blacklistedTags ->
			list?.skipNsfwIfNeeded()?.let(GlobalTagBlacklist(blacklistedTags)::filter)
		},
		observeListModeWithTriggers(),
		listError,
		hasNextPage,
		selectedGroupTab, // Adding these to match the base class requirement if creating a new combine
		selectedSourceTags, // but RemoteListViewModel might not use them.
		// Wait, RemoteListViewModel overrides `content`. The error was:
		// Cannot infer type for this parameter. Specify it explicitly.
	) { values: Array<Any?> ->
		val list = values[0] as List<Content>?
		val mode = values[1] as ListMode
		val error = values[2] as Throwable?
		val hasNext = values[3] as Boolean
		
		buildList(list?.size?.plus(2) ?: 2) {
			when {
				list.isNullOrEmpty() && error != null -> add(
					error.toErrorState(
						canRetry = true,
						secondaryAction = if (error.getCauseUrl().isNullOrEmpty()) 0 else R.string.open_in_browser,
					),
				)

				list == null -> add(LoadingState)
				list.isEmpty() -> add(createEmptyState(canResetFilter = filterCoordinator.isFilterApplied))
				else -> {
					mapContentList(this, list, mode)
					when {
						error != null -> add(error.toErrorFooter())
						hasNext -> add(LoadingFooter())
						else -> getFooter()?.let(::add)
					}
				}
			}
			onBuildList(this)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, listOf(LoadingState))

	init {
		filterCoordinator.observe()
			.debounce(FILTER_MIN_INTERVAL)
			.onEach { filterState ->
				Log.d(
					RemoteListPaginationLogTag,
					"filterChanged source=${source.name} cancelActive=${loadingJob?.isActive == true}",
				)
				loadingJob?.cancelAndJoin()
				mangaList.value = null
				lastLoadedPageIndex = -1
				loadList(filterState, false)
			}.catch { error ->
				listError.value = error
			}.launchIn(viewModelScope)

		launchJob(Dispatchers.Default) {
			sourcesRepository.trackUsage(source)
		}

	}

	override fun onRefresh() {
		Log.d(
			RemoteListPaginationLogTag,
			"refresh source=${source.name} current=${mangaList.value.sizeOrZero()} hasNext=${hasNextPage.value}",
		)
		loadingJob?.cancel()
		lastLoadedPageIndex = -1
		loadList(filterCoordinator.snapshot(), append = false)
	}

	override fun onRetry() {
		Log.d(
			RemoteListPaginationLogTag,
			"retry source=${source.name} append=${!mangaList.value.isNullOrEmpty()} " +
				"current=${mangaList.value.sizeOrZero()} hasNext=${hasNextPage.value} error=${listError.value != null}",
		)
		loadList(filterCoordinator.snapshot(), append = !mangaList.value.isNullOrEmpty())
	}

	override fun onContentClick(content: Content): Boolean {
		val actionRepository = repository as? ContentActionRepository
		val isAction = actionRepository?.isAction(content) == true
		Log.d(
			RemoteListPaginationLogTag,
			"contentClick source=${source.name} repository=${repository::class.simpleName} " +
				"sourceDataLength=${content.sourceData?.length ?: 0} isAction=$isAction",
		)
		if (actionRepository == null || !isAction) return false
		val requiresActivityHost = actionRepository.requiresActivityHost(content)
		Log.d(
			RemoteListPaginationLogTag,
			"contentClick source=${source.name} requiresActivityHost=$requiresActivityHost",
		)
		if (requiresActivityHost) {
			onContentActionHostRequest.call(
				ContentActionHostRequest { onComplete ->
					executeContentAction(actionRepository, content, onComplete)
				},
			)
			return true
		}
		executeContentAction(actionRepository, content)
		return true
	}

	private fun executeContentAction(
		actionRepository: ContentActionRepository,
		content: Content,
		onComplete: () -> Unit = {},
	) {
		if (contentActionJob?.isActive == true) return
		contentActionJob = viewModelScope.launch(Dispatchers.IO) {
			runCatching { actionRepository.executeAction(content) }
				.onSuccess { result ->
					val message = result?.message?.takeIf { it.isNotBlank() }
					if (message != null) onContentMessage.call(message)
					if (result != null) onRefresh()
				}
				.onFailure(errorEvent::call)
			onComplete()
		}
	}

	fun loadNextPage() {
		val hasNext = hasNextPage.value
		val error = listError.value
		Log.d(
			RemoteListPaginationLogTag,
			"loadNextPage source=${source.name} hasNext=$hasNext error=${error?.javaClass?.simpleName} " +
				"current=${mangaList.value.sizeOrZero()} loadingActive=${loadingJob?.isActive == true}",
		)
		if (hasNext && error == null) {
			loadList(filterCoordinator.snapshot(), append = true)
		}
	}

	protected fun loadList(filterState: FilterCoordinator.Snapshot, append: Boolean): Job {
		loadingJob?.let {
			if (it.isActive) {
				Log.d(
					RemoteListPaginationLogTag,
					"loadList skipActive source=${source.name} append=$append current=${mangaList.value.sizeOrZero()}",
				)
				return it
			}
		}
		return launchLoadingJob(Dispatchers.Default) {
			try {
				listError.value = null
				val offsetOrPageIndex = when (repository.listPagingMode) {
					ContentRepository.ListPagingMode.OFFSET -> if (append) mangaList.value.sizeOrZero() else 0
					ContentRepository.ListPagingMode.PAGE_INDEX -> {
						val pageIndex = if (append) lastLoadedPageIndex + 1 else 0
						lastRequestedPageIndex = pageIndex
							pageIndex
						}
					}
					Log.d(
						RemoteListPaginationLogTag,
						"loadList start source=${source.name} append=$append mode=${repository.listPagingMode} " +
							"offsetOrPage=$offsetOrPageIndex current=${mangaList.value.sizeOrZero()} " +
							"lastLoadedPage=$lastLoadedPageIndex",
					)
					val list = repository.getList(
						offset = offsetOrPageIndex,
						order = filterState.sortOrder,
						filter = filterState.listFilter,
					)
					if (shouldUpdateSourceAvailability(append)) {
						if (list.isEmpty()) {
							sourceAvailabilityRepository.markEmpty(source)
						} else {
							sourceAvailabilityRepository.markAvailable(source)
						}
					}
					val prevList = mangaList.value.orEmpty()
					Log.d(
						RemoteListPaginationLogTag,
						"loadList result source=${source.name} append=$append fetched=${list.size} prev=${prevList.size}",
					)
					if (!append) {
						mangaList.value = list.distinctById()
						if (repository.listPagingMode == ContentRepository.ListPagingMode.PAGE_INDEX && list.isNotEmpty()) {
						lastLoadedPageIndex = 0
					}
				} else if (list.isNotEmpty()) {
					mangaList.value = (prevList + list).distinctById()
					if (repository.listPagingMode == ContentRepository.ListPagingMode.PAGE_INDEX) {
						lastLoadedPageIndex = lastRequestedPageIndex
					}
				}
					hasNextPage.value = when (repository.listPagingMode) {
						ContentRepository.ListPagingMode.OFFSET -> if (append) {
							prevList != mangaList.value
					} else {
						list.size > prevList.size || hasNextPage.value
					}
						ContentRepository.ListPagingMode.PAGE_INDEX -> list.isNotEmpty()
					}
					Log.d(
						RemoteListPaginationLogTag,
						"loadList applied source=${source.name} append=$append total=${mangaList.value.sizeOrZero()} " +
							"hasNext=${hasNextPage.value} lastLoadedPage=$lastLoadedPageIndex",
					)
				} catch (e: CancellationException) {
					throw e
				} catch (e: Throwable) {
					Log.d(
						RemoteListPaginationLogTag,
						"loadList error source=${source.name} append=$append current=${mangaList.value.sizeOrZero()} " +
							"error=${e.javaClass.simpleName}: ${e.message}",
					)
					e.printStackTraceDebug()
					listError.value = e
				if (shouldUpdateSourceAvailability(append)) {
					sourceAvailabilityRepository.markEmpty(source)
				}
				if (!mangaList.value.isNullOrEmpty()) {
					errorEvent.call(e)
				}
				hasNextPage.value = false
			}
		}.also { loadingJob = it }
	}

	private fun shouldUpdateSourceAvailability(append: Boolean): Boolean {
		return !append && !filterCoordinator.isFilterApplied && !source.isLocal
	}

	protected open fun createEmptyState(canResetFilter: Boolean) = EmptyState(
		icon = R.drawable.ic_empty_common,
		textPrimary = R.string.nothing_found,
		textSecondary = 0,
		actionStringRes = if (canResetFilter) R.string.reset_filter else 0,
	)

	protected open fun resolveInitialSource(savedStateHandle: SavedStateHandle): ParserContentSource {
		val sourceName = savedStateHandle.get<String>(com.mangaverse.app.core.nav.AppRouter.KEY_SOURCE)
			?: savedStateHandle.get<String>("sourceName")
		return ContentSource(sourceName)
	}

	protected open suspend fun onBuildList(list: MutableList<ListModel>) = Unit

	protected open suspend fun mapContentList(
		destination: MutableCollection<in ListModel>,
		manga: Collection<Content>,
		mode: ListMode
	) = mangaListMapper.toListModelList(destination, manga, mode)

	protected open fun getFooter(): ButtonFooter? {
		val filter = filterCoordinator.snapshot().listFilter
		val hasQuery = !filter.query.isNullOrEmpty()
		val hasAuthor = !filter.author.isNullOrEmpty()
		val isOneTag = filter.tags.size == 1
		return if ((hasQuery xor isOneTag xor hasAuthor) && !(hasQuery && isOneTag && hasAuthor)) {
			ButtonFooter(R.string.global_search)
		} else {
			null
		}
	}

	fun openRandom() {
		if (randomJob?.isActive == true) {
			return
		}
		randomJob = launchLoadingJob(Dispatchers.Default) {
			isRandomLoading.value = true
			val manga = exploreRepository.findRandomContent(source, 16)
			onOpenContent.call(manga)
			isRandomLoading.value = false
		}
	}
}
