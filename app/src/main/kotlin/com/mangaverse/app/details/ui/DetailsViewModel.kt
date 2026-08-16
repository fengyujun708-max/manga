package com.mangaverse.app.details.ui

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.core.text.parseAsHtml
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import com.mangaverse.app.core.util.ext.combine as extCombine
import com.mangaverse.app.core.util.ext.sanitize
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import com.mangaverse.app.R
import com.mangaverse.app.favourites.domain.AttachReadingSourceToEntityUseCase
import com.mangaverse.app.details.ui.model.ActiveLocalSourceOption
import com.mangaverse.app.details.ui.model.EntityChapterSourceInfo
import com.mangaverse.app.details.ui.model.toListItem
import com.mangaverse.app.bookmarks.domain.BookmarksRepository
import com.mangaverse.app.core.model.ContentSource
import com.mangaverse.app.core.model.ContentSourceInfo
import com.mangaverse.app.core.model.getContentType
import com.mangaverse.app.core.model.resolvedContentTypeForSnapshot
import com.mangaverse.app.core.model.isLocal
import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.core.model.getPreferredBranch
import com.mangaverse.app.core.db.entity.toContent
import com.mangaverse.app.core.jsonsource.SourceType
import com.mangaverse.app.core.jsonsource.SourceTypeIdentifier
import com.mangaverse.app.core.nav.ContentIntent
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.ListMode
import com.mangaverse.app.core.prefs.TriStateOption
import com.mangaverse.app.core.prefs.observeAsFlow
import com.mangaverse.app.core.prefs.observeAsStateFlow
import com.mangaverse.app.core.ui.util.ReversibleAction
import com.mangaverse.app.core.util.ext.awaitCancellable
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.core.util.ext.computeSize
import com.mangaverse.app.core.util.ext.onEachWhile
import com.mangaverse.app.details.data.CachedTranslationEntry
import com.mangaverse.app.details.data.ContentDetails
import com.mangaverse.app.details.data.DetailsTranslationCache
import com.mangaverse.app.details.domain.BranchComparator
import com.mangaverse.app.details.domain.DetailsInteractor
import com.mangaverse.app.details.domain.DetailsLoadUseCase
import com.mangaverse.app.details.domain.isDetailsProjectionAllowed
import com.mangaverse.app.details.domain.ProgressUpdateUseCase
import com.mangaverse.app.work.domain.WorkProjectionBindingResult
import com.mangaverse.app.details.domain.ReadingTimeUseCase
import com.mangaverse.app.details.domain.RelatedContentUseCase
import com.mangaverse.app.details.ui.model.HistoryInfo
import com.mangaverse.app.details.ui.model.DetailsOrigin
import com.mangaverse.app.details.ui.model.ContentBranch
import com.mangaverse.app.details.ui.model.DetailsSourceOption
import com.mangaverse.app.details.ui.model.DetailsChapterSourceTab
import com.mangaverse.app.details.ui.model.ChapterListItem.Companion.FLAG_DOWNLOADED
import com.mangaverse.app.details.ui.model.findChapterByHistory
import com.mangaverse.app.details.ui.pager.ChaptersPagesViewModel
import com.mangaverse.app.details.ui.pager.EmptyContentReason
import com.mangaverse.app.details.ui.model.LocalSearchState
import com.mangaverse.app.download.ui.worker.DownloadWorker
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.explore.data.SourcePreset
import com.mangaverse.app.explore.data.SourcePresetsRepository
import com.mangaverse.app.core.parser.ContentSourceResolutionPipeline
import com.mangaverse.app.mihon.MihonExtensionManager
import com.mangaverse.app.history.data.HistoryRepository
import com.mangaverse.app.list.domain.ContentListMapper
import com.mangaverse.app.list.ui.model.ContentListModel
import com.mangaverse.app.local.data.LocalStorageChanges
import com.mangaverse.app.local.domain.DeleteLocalContentUseCase
import com.mangaverse.app.local.domain.model.LocalContent
import com.mangaverse.app.favourites.domain.FavouritesRepository
import com.mangaverse.app.favourites.domain.MergeBackAndAddFavouriteUseCase
import com.mangaverse.app.favourites.ui.categories.select.FavoriteDuplicatePrompt
import com.mangaverse.app.core.model.FavouriteCategory
import com.mangaverse.app.core.model.ids
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentState
import com.mangaverse.app.parsers.model.SortOrder
import com.mangaverse.app.parsers.util.ifNullOrEmpty
import com.mangaverse.app.parsers.util.runCatchingCancellable
import com.mangaverse.app.readingrecord.data.ReadingRecordRepository
import com.mangaverse.app.readingrecord.data.ReadingRecordSnapshot
import com.mangaverse.app.reader.ui.FULLY_READ_CHAPTER_ID
import com.mangaverse.app.reader.ui.ReaderState
import com.mangaverse.app.core.domain.model.ScrobblingStatus
import com.mangaverse.app.core.parser.ContentDataRepository.MetadataSourceSelection as PersistedMetadataSourceSelection
import javax.inject.Inject
import kotlin.experimental.or
import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.entitygraph.domain.Entity
import com.mangaverse.app.entitygraph.domain.EntityBinding
import com.mangaverse.app.entitygraph.domain.EntityType
import com.mangaverse.app.entitygraph.domain.Relation
import com.mangaverse.app.entitygraph.domain.RelationType
import com.mangaverse.app.entitygraph.domain.TrackingCharacterDto
import com.mangaverse.app.entitygraph.domain.TrackingPersonDto
import com.mangaverse.app.entitygraph.domain.TrackingStaffDto
import com.mangaverse.app.entitygraph.domain.TrackingWorkDto
import com.mangaverse.app.entitygraph.domain.isLocalReadingSource
import com.mangaverse.app.entitygraph.domain.stripEntityDisambiguationTitleSuffix
import com.mangaverse.app.entitygraph.ui.details.EntityRelationSection
import com.mangaverse.app.entitygraph.ui.details.EntityRelationItem
import com.mangaverse.app.details.ui.model.DetailsSupplementAction
import com.mangaverse.app.filter.ui.model.UiTagGroup
import com.mangaverse.app.search.domain.ALL_SEARCH_CONTENT_KINDS
import com.mangaverse.app.search.domain.ALL_SOURCE_TYPES
import com.mangaverse.app.search.domain.SearchContentKind
import com.mangaverse.app.search.domain.matches
import com.mangaverse.app.work.domain.WorkDuplicateCandidateRepository
import kotlinx.coroutines.channels.BufferOverflow
import java.io.File
import java.util.Locale
import com.mangaverse.app.space.domain.SpaceContentPolicy
import com.mangaverse.app.stats.data.StatsRepository
import com.mangaverse.app.space.domain.SpaceId

private const val SYNTHETIC_ENTITY_GRAPH_SOURCE = "Entity Graph"
private const val DETAILS_TRACE_TAG = "DetailsTrace"

private fun Content?.detailsTraceSummary(): String {
	return this?.let {
		"id=${it.id} source=${it.source.name} locale=${it.source.locale} chapters=${it.chapters?.size ?: 0}"
	} ?: "null"
}

private fun DetailsOrigin?.detailsTraceSummary(): String = when (this) {
	null -> "null"
	is DetailsOrigin.EntityGraph ->
		"EntityGraph(entityId=$entityId, preferred=$preferredLocalMangaId, initial=$initialProjectionLocalMangaId)"
	is DetailsOrigin.LocalMangaId -> "LocalMangaId(mangaId=$mangaId)"
	is DetailsOrigin.LocalMangaContent -> "LocalMangaContent(${manga.detailsTraceSummary()})"
	is DetailsOrigin.TrackingEntity -> "TrackingEntity(serviceId=$serviceId, remoteId=$remoteId)"
	is DetailsOrigin.TrackingItem -> "TrackingItem(serviceId=$serviceId, remoteId=$remoteId)"
}

internal fun List<EntityRelationSection>.deduplicateRelationItems(): List<EntityRelationSection> =
	map { section ->
		section.copy(items = section.items.distinctBy(EntityRelationItem::stableKey))
	}

internal fun DetailsOrigin.initialProjectionLocalMangaIdOrNull(): Long? = when (this) {
	is DetailsOrigin.EntityGraph -> initialProjectionLocalMangaId
	is DetailsOrigin.LocalMangaId -> mangaId
	is DetailsOrigin.LocalMangaContent -> manga.id
	else -> null
}

internal fun DetailsOrigin.initialProjectionIntentOrNull(): ContentIntent? = when (this) {
	is DetailsOrigin.LocalMangaContent -> ContentIntent.of(manga)
	else -> initialProjectionLocalMangaIdOrNull()?.let(ContentIntent::of)
}

internal fun Content.isSyntheticEntityGraphContent(): Boolean = source.name == SYNTHETIC_ENTITY_GRAPH_SOURCE

private const val ENTITY_RELATION_SECTIONS_DEBOUNCE_MS = 120L
private const val TRACKING_SUGGESTION_THRESHOLD = 0.9f
private const val TRACKING_SUGGESTION_GAP_THRESHOLD = 0.03f
private const val TRACKING_SUGGESTION_RESULT_LIMIT = 3
private const val SOURCE_SEARCH_TIMEOUT_MS = 12_000L
private const val READING_SEARCH_MAX_PARALLELISM = 4
private const val READING_SEARCH_LOG_TAG = "ReadingSourceSearch"
private const val MAX_DUPLICATE_PROMPT_CANDIDATES = 3
private fun <T> Flow<T>?.orEmptyFlow(fallback: T): Flow<T> = this ?: flowOf(fallback)

private inline fun <T> StateFlow<T>?.safeValueOrNull(): T? = runCatching {
	this?.value
}.getOrNull()

private inline fun <T> flowOrFallback(
	fallback: T,
	block: () -> Flow<T>?,
): Flow<T> = runCatching {
	block()
}.getOrNull().orEmptyFlow(fallback)

private data class WorkProjectionContext(
	val entityId: Long?,
	val requestedMangaId: Long,
	val preferredLocalMangaId: Long?,
	val persistedLocalMangaId: Long,
	val candidateMangaIds: List<Long>,
)

private data class CurrentWorkProjectionSnapshot(
	val activeLocalMangaId: Long?,
	val currentReadingProjectionMangaId: Long?,
)

data class DetailsSupplementUiState(
	val metadataProperties: List<Pair<String, String>> = emptyList(),
	val sections: List<EntityRelationSection> = emptyList(),
	val actions: List<DetailsSupplementAction> = emptyList(),
)

data class ReadingSearchUiState(
	val sources: List<ContentSourceInfo> = emptyList(),
	val selectedSource: String? = null,
	val query: String = "",
	val sections: List<ReadingSearchSectionUiState> = emptyList(),
	val isLoading: Boolean = false,
	val hasSearched: Boolean = false,
	val state: LocalSearchState? = null,
	val filterUiState: ReadingSearchFilterUiState = ReadingSearchFilterUiState(),
	val scopeFilterUiState: ReadingSearchScopeFilterUiState = ReadingSearchScopeFilterUiState(),
)

data class ReadingSearchSectionUiState(
	val source: ContentSourceInfo,
	val items: List<Content> = emptyList(),
	val isPending: Boolean = false,
	val isLoading: Boolean = false,
	val errorMessage: String? = null,
)

data class ReadingSearchFilterUiState(
	val hasSelectedSource: Boolean = false,
	val isLoading: Boolean = false,
	val errorMessage: String? = null,
	val sortOrders: List<SortOrder> = emptyList(),
	val selectedSortOrder: SortOrder? = null,
	val tagGroups: List<UiTagGroup> = emptyList(),
	val excludedTagGroups: List<UiTagGroup> = emptyList(),
	val contentTypes: List<ContentType> = emptyList(),
	val selectedContentTypes: Set<ContentType> = emptySet(),
	val states: List<ContentState> = emptyList(),
	val selectedStates: Set<ContentState> = emptySet(),
	val locales: List<Locale?> = emptyList(),
	val selectedLocale: Locale? = null,
	val author: String? = null,
	val canSearchByAuthor: Boolean = false,
	val supportsTagExclusion: Boolean = false,
	val appliedFilterCount: Int = 0,
)

data class ReadingSearchScopeFilterUiState(
	val sourceTypes: Set<SourceType> = ALL_SOURCE_TYPES,
	val contentKinds: Set<SearchContentKind> = ALL_SEARCH_CONTENT_KINDS,
	val pinnedOnly: Boolean = false,
	val hideEmpty: Boolean = false,
) {
	val appliedFilterCount: Int
		get() {
			var count = 0
			if (sourceTypes != ALL_SOURCE_TYPES) count++
			if (contentKinds != ALL_SEARCH_CONTENT_KINDS) count++
			if (pinnedOnly) count++
			if (hideEmpty) count++
			return count
		}
}

data class SourceBindingUiState(
	val activeLocalSourceOptions: List<ActiveLocalSourceOption> = emptyList(),
	val entityChapterSourceInfo: EntityChapterSourceInfo? = null,
	val metadataSourceOptions: List<DetailsSourceOption> = emptyList(),
	val readingSourceOptions: List<DetailsSourceOption> = emptyList(),
	val metadataChapterTabs: List<DetailsChapterSourceTab> = emptyList(),
	val readingChapterTabs: List<DetailsChapterSourceTab> = emptyList(),
	val resolvedMetadataContentType: ContentType? = null,
	val resolvedMetadataLanguage: String? = null,
	val resolvedReadingLanguage: String? = null,
)

data class TranslationUiState(
	val translatedTitle: String? = null,
	val translatedDescription: String? = null,
	val isShowingTranslation: Boolean = false,
	val hasTranslationCache: Boolean = false,
	val isTranslating: Boolean = false,
	val showTranslateAction: Boolean = false,
)

data class DetailsPrimaryUiState(
	val mangaDetails: ContentDetails? = null,
	val remoteContent: Content? = null,
	val relatedContent: List<ContentListModel> = emptyList(),
	val favouriteCategories: Set<FavouriteCategory> = emptySet(),
	val historyInfo: HistoryInfo = HistoryInfo(null, null, null, false, null),
	val branches: List<ContentBranch> = emptyList(),
	val isStatsAvailable: Boolean = false,
	val readingStatus: ScrobblingStatus = ScrobblingStatus.PLANNED,
	val unifiedRating: Float = 0f,
	val canEditUnifiedRating: Boolean = false,
	val isLoading: Boolean = false,
	val entityRelationSections: List<EntityRelationSection> = emptyList(),
	val activeLocalBrowserContent: Content? = null,
	val isWorkDetails: Boolean = true,
)

data class ChaptersPaneControlsUiState(
	val isChaptersReversed: Boolean = false,
	val isChaptersInGridView: Boolean = false,
	val isHideReadChapters: Boolean = false,
	val isMergeRepeatedChapters: Boolean = false,
	val showMergeRepeatedChapters: Boolean = false,
	val isDownloadedOnly: Boolean = false,
	val emptyReason: EmptyContentReason? = null,
)

private data class ReadingSearchPrimaryUiState(
	val sources: List<ContentSourceInfo> = emptyList(),
	val selectedSource: String? = null,
	val query: String = "",
	val sections: List<ReadingSearchSectionUiState> = emptyList(),
)

private data class ReadingSearchFilterState(
	val source: ContentSourceInfo? = null,
	val capabilities: com.mangaverse.app.parsers.model.ContentListFilterCapabilities =
		com.mangaverse.app.parsers.model.ContentListFilterCapabilities(),
	val filterOptions: com.mangaverse.app.parsers.model.ContentListFilterOptions =
		com.mangaverse.app.parsers.model.ContentListFilterOptions(),
	val sortOrders: List<SortOrder> = emptyList(),
	val selectedSortOrder: SortOrder? = null,
	val listFilter: ContentListFilter = ContentListFilter.EMPTY,
	val isLoading: Boolean = false,
	val errorMessage: String? = null,
)

private data class SourceOptionsUiState(
	val activeLocalSourceOptions: List<ActiveLocalSourceOption> = emptyList(),
	val entityChapterSourceInfo: EntityChapterSourceInfo? = null,
	val metadataSourceOptions: List<DetailsSourceOption> = emptyList(),
	val readingSourceOptions: List<DetailsSourceOption> = emptyList(),
)

private data class SourceChapterTabsUiState(
	val metadataChapterTabs: List<DetailsChapterSourceTab> = emptyList(),
	val readingChapterTabs: List<DetailsChapterSourceTab> = emptyList(),
)

private data class SourceResolutionUiState(
	val resolvedMetadataContentType: ContentType? = null,
	val resolvedMetadataLanguage: String? = null,
	val resolvedReadingLanguage: String? = null,
)

private data class TranslationTextUiState(
	val translatedTitle: String? = null,
	val translatedDescription: String? = null,
	val isShowingTranslation: Boolean = false,
	val hasTranslationCache: Boolean = false,
)

private data class DetailsHeaderUiState(
	val mangaDetails: ContentDetails? = null,
	val favouriteCategories: Set<FavouriteCategory> = emptySet(),
	val historyInfo: HistoryInfo = HistoryInfo(null, null, null, false, null),
	val readingStatus: ScrobblingStatus = ScrobblingStatus.PLANNED,
	val unifiedRating: Float = 0f,
	val canEditUnifiedRating: Boolean = false,
)

private data class DetailsPaneSummaryUiState(
	val remoteContent: Content? = null,
	val branches: List<ContentBranch> = emptyList(),
	val isStatsAvailable: Boolean = false,
	val isLoading: Boolean = false,
	val activeLocalBrowserContent: Content? = null,
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
	@ApplicationContext private val context: Context,
	private val historyRepository: HistoryRepository,
	private val readingRecordRepository: ReadingRecordRepository,
	bookmarksRepository: BookmarksRepository,
	settings: AppSettings,
	@LocalStorageChanges localStorageChanges: SharedFlow<LocalContent?>,
	downloadScheduler: DownloadWorker.Scheduler,
	interactor: DetailsInteractor,
	savedStateHandle: SavedStateHandle,
	deleteLocalContentUseCase: DeleteLocalContentUseCase,
	private val relatedContentUseCase: RelatedContentUseCase,
	private val mangaListMapper: ContentListMapper,
	private val detailsLoadUseCase: DetailsLoadUseCase,
	private val progressUpdateUseCase: ProgressUpdateUseCase,
	private val readingTimeUseCase: ReadingTimeUseCase,
	private val attachReadingSourceToEntityUseCase: AttachReadingSourceToEntityUseCase,
	statsRepository: StatsRepository,
	private val epubChapterMappingDao: com.mangaverse.app.core.db.dao.EpubChapterMappingDao,
	private val favouritesRepository: FavouritesRepository,
	private val duplicateCandidateRepository: WorkDuplicateCandidateRepository,
	private val mergeBackAndAddFavouriteUseCase: MergeBackAndAddFavouriteUseCase,
	mangaRepositoryFactory: com.mangaverse.app.core.parser.ContentRepository.Factory,
	private val contentSourcesRepository: ContentSourcesRepository,
	private val mihonExtensionManager: MihonExtensionManager,
	private val contentSourceResolutionPipeline: ContentSourceResolutionPipeline,
	private val sourcePresetsRepository: SourcePresetsRepository,
	private val dataRepository: com.mangaverse.app.core.parser.ContentDataRepository,
	private val detailsTranslationCache: DetailsTranslationCache,
	private val db: com.mangaverse.app.core.db.MangaDatabase,
	private val entityGraphRepository: com.mangaverse.app.entitygraph.data.EntityGraphRepository,
	private val sourceTypeIdentifier: SourceTypeIdentifier,
	private val workResolver: com.mangaverse.app.work.domain.WorkResolver,
	private val spaceContentPolicy: SpaceContentPolicy,
) : ChaptersPagesViewModel(
	settings = settings,
	interactor = interactor,
	bookmarksRepository = bookmarksRepository,
	historyRepository = historyRepository,
	downloadScheduler = downloadScheduler,
	deleteLocalContentUseCase = deleteLocalContentUseCase,
	mangaRepositoryFactory = mangaRepositoryFactory,
	localStorageChanges = localStorageChanges,
) {

	private val intent = ContentIntent(savedStateHandle)
	val activeExternalOrigin = savedStateHandle.get<com.mangaverse.app.details.ui.model.DetailsOrigin>(
		com.mangaverse.app.core.nav.AppRouter.KEY_DETAILS_ORIGIN,
	) ?: com.mangaverse.app.core.nav.PendingDetailsNavigation.consume()
	private val isTemporaryReadOnly = savedStateHandle.get<Boolean>(
		com.mangaverse.app.core.nav.AppRouter.KEY_TEMPORARY_DETAILS,
	) == true
	private val originContent = (activeExternalOrigin as? com.mangaverse.app.details.ui.model.DetailsOrigin.LocalMangaContent)?.manga
	private val initialProjectionIntentOverride = activeExternalOrigin?.initialProjectionIntentOrNull()
	private var loadingJob: Job = Job()
	private var translateAvailabilityJob: Job? = null
	private var readingSearchJob: Job? = null
	private var sourceBindingsRefreshJob: Job? = null
	private var readingSearchGeneration: Int = 0
	private var allEnabledSourcesLoaded = false
	private var currentLoadIntentOverride: ContentIntent? = initialProjectionIntentOverride
	private var translationCacheSourceLang: String? = null
	private var translationCacheTargetLang: String? = null
	private val activeMangaIdFlow = kotlinx.coroutines.flow.MutableStateFlow(
		activeExternalOrigin?.initialProjectionLocalMangaIdOrNull()
			?: intent.mangaId.takeIf { it != 0L },
	)
	val mangaId: Long get() = activeMangaIdFlow.value ?: intent.mangaId

	private val pendingEntityRelationSections = MutableSharedFlow<List<EntityRelationSection>>(
		replay = 1,
		extraBufferCapacity = 1,
		onBufferOverflow = BufferOverflow.DROP_OLDEST,
	)
	val entityRelationSections: StateFlow<List<EntityRelationSection>> = pendingEntityRelationSections
		.debounce(ENTITY_RELATION_SECTIONS_DEBOUNCE_MS)
		.distinctUntilChanged()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())
	val activeLocalSourceOptions = MutableStateFlow<List<ActiveLocalSourceOption>>(emptyList())
	val entityChapterSourceInfo = MutableStateFlow<EntityChapterSourceInfo?>(null)
	val metadataSourceOptions = MutableStateFlow<List<DetailsSourceOption>>(emptyList())
	val readingSourceOptions = MutableStateFlow<List<DetailsSourceOption>>(emptyList())
	val metadataChapterTabs = MutableStateFlow<List<DetailsChapterSourceTab>>(emptyList())
	val readingChapterTabs = MutableStateFlow<List<DetailsChapterSourceTab>>(emptyList())
	private var detailsSpaceId: SpaceId? = null
	private var activeEntityContextId: Long? = null
	private var activeEntityContextBindings: List<EntityBinding> = emptyList()
	private var activeEntityContextBoundLocalId: Long? = null
	private var activeProjectionStoredContentType: ContentType? = null
	private val sessionReadingProjectionLocalMangaId = MutableStateFlow<Long?>(null)
	val supplementalMetadataProperties = MutableStateFlow<List<Pair<String, String>>>(emptyList())
	val supplementalSections = MutableStateFlow<List<EntityRelationSection>>(emptyList())
	val supplementalActions = MutableStateFlow<List<DetailsSupplementAction>>(emptyList())
	val detailsSupplementUiState: StateFlow<DetailsSupplementUiState> = combine(
		supplementalMetadataProperties,
		supplementalSections,
		supplementalActions,
	) { metadataProperties, sections, actions ->
		DetailsSupplementUiState(
			metadataProperties = metadataProperties,
			sections = sections,
			actions = actions,
		)
	}.stateIn(viewModelScope, SharingStarted.Eagerly, DetailsSupplementUiState())
	val readingSearchSources = MutableStateFlow<List<ContentSourceInfo>>(emptyList())
	val selectedReadingSearchSource = MutableStateFlow<String?>(null)
	val readingSearchQuery = MutableStateFlow("")
	val readingSearchSections = MutableStateFlow<List<ReadingSearchSectionUiState>>(emptyList())
	val readingSearchLoading = MutableStateFlow(false)
	val readingSearchHasSearched = MutableStateFlow(false)
	val readingSearchState = MutableStateFlow<LocalSearchState?>(null)
	private val readingSearchFilterState = MutableStateFlow(ReadingSearchFilterState())
	private val readingSearchScopeFilters = MutableStateFlow(ReadingSearchScopeFilterUiState())
	private val readingSearchPrimaryUiState = combine(
		readingSearchSources,
		selectedReadingSearchSource,
		readingSearchQuery,
		readingSearchSections,
	) { sources, selectedSource, query, sections ->
		ReadingSearchPrimaryUiState(
			sources = sources,
			selectedSource = selectedSource,
			query = query,
			sections = sections,
		)
	}
	val readingSearchUiState: StateFlow<ReadingSearchUiState> = combine(
		readingSearchPrimaryUiState,
		combine(
			readingSearchLoading,
			readingSearchHasSearched,
			readingSearchState,
		) { isLoading, hasSearched, state ->
			Triple(isLoading, hasSearched, state)
		},
		readingSearchFilterState,
		readingSearchScopeFilters,
	) { primary, status, filterState, scopeFilterState ->
		ReadingSearchUiState(
			sources = primary.sources,
			selectedSource = primary.selectedSource,
			query = primary.query,
			sections = primary.sections,
			isLoading = status.first,
			hasSearched = status.second,
			state = status.third,
			filterUiState = filterState.toUiState(),
			scopeFilterUiState = scopeFilterState,
		)
	}.stateIn(viewModelScope, SharingStarted.Eagerly, ReadingSearchUiState())
	val languagePresets: StateFlow<List<SourcePreset>> = sourcePresetsRepository.observeAll()
		.stateIn(viewModelScope + Dispatchers.IO, SharingStarted.Eagerly, emptyList())
	val activeLanguagePresetId: StateFlow<Long> = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.IO,
		key = AppSettings.KEY_ACTIVE_SOURCE_PRESET_ID,
		valueProducer = { settings.activeSourcePresetId },
	)
	val chaptersPaneControlsUiState: StateFlow<ChaptersPaneControlsUiState> = extCombine(
		isChaptersReversed,
		isChaptersInGridView,
		isHideReadChapters,
		isMergeRepeatedChapters,
		showMergeRepeatedChapters,
		isDownloadedOnly,
		emptyReason,
	) { isChaptersReversed, isChaptersInGridView, isHideReadChapters, isMergeRepeatedChapters, showMergeRepeatedChapters, isDownloadedOnly, emptyReason ->
		ChaptersPaneControlsUiState(
			isChaptersReversed = isChaptersReversed,
			isChaptersInGridView = isChaptersInGridView,
			isHideReadChapters = isHideReadChapters,
			isMergeRepeatedChapters = isMergeRepeatedChapters,
			showMergeRepeatedChapters = showMergeRepeatedChapters,
			isDownloadedOnly = isDownloadedOnly,
			emptyReason = emptyReason,
		)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, ChaptersPaneControlsUiState())
	val resolvedMetadataContentType = MutableStateFlow<ContentType?>(null)
	val resolvedMetadataLanguage = MutableStateFlow<String?>(null)
	val resolvedReadingLanguage = MutableStateFlow<String?>(null)
	private val sourceOptionsUiState = combine(
		activeLocalSourceOptions,
		entityChapterSourceInfo,
		metadataSourceOptions,
		readingSourceOptions,
	) { activeLocalSourceOptions, entityChapterSourceInfo, metadataSourceOptions, readingSourceOptions ->
		SourceOptionsUiState(
			activeLocalSourceOptions = activeLocalSourceOptions,
			entityChapterSourceInfo = entityChapterSourceInfo,
			metadataSourceOptions = metadataSourceOptions,
			readingSourceOptions = readingSourceOptions,
		)
	}
	private val sourceChapterTabsUiState = combine(
		metadataChapterTabs,
		readingChapterTabs,
	) { metadataChapterTabs, readingChapterTabs ->
		SourceChapterTabsUiState(
			metadataChapterTabs = metadataChapterTabs,
			readingChapterTabs = readingChapterTabs,
		)
	}
	private val sourceResolutionUiState = combine(
		resolvedMetadataContentType,
		resolvedMetadataLanguage,
		resolvedReadingLanguage,
	) { resolvedMetadataContentType, resolvedMetadataLanguage, resolvedReadingLanguage ->
		SourceResolutionUiState(
			resolvedMetadataContentType = resolvedMetadataContentType,
			resolvedMetadataLanguage = resolvedMetadataLanguage,
			resolvedReadingLanguage = resolvedReadingLanguage,
		)
	}
	val sourceBindingUiState: StateFlow<SourceBindingUiState> = combine(
		sourceOptionsUiState,
		sourceChapterTabsUiState,
		sourceResolutionUiState,
	) { sourceOptions, sourceTabs, sourceResolution ->
		SourceBindingUiState(
			activeLocalSourceOptions = sourceOptions.activeLocalSourceOptions,
			entityChapterSourceInfo = sourceOptions.entityChapterSourceInfo,
			metadataSourceOptions = sourceOptions.metadataSourceOptions,
			readingSourceOptions = sourceOptions.readingSourceOptions,
			metadataChapterTabs = sourceTabs.metadataChapterTabs,
			readingChapterTabs = sourceTabs.readingChapterTabs,
			resolvedMetadataContentType = sourceResolution.resolvedMetadataContentType,
			resolvedMetadataLanguage = sourceResolution.resolvedMetadataLanguage,
			resolvedReadingLanguage = sourceResolution.resolvedReadingLanguage,
		)
	}.stateIn(viewModelScope, SharingStarted.Eagerly, SourceBindingUiState())
	val showTranslateAction = MutableStateFlow(false)
	val activeLocalBrowserContent = MutableStateFlow<Content?>(null)
	private val isWorkDetails = MutableStateFlow(initialIsWorkDetails())
	private val allEnabledSourceInfos = MutableStateFlow<List<ContentSourceInfo>>(emptyList())
	private val activeSourcePreset = settings.observeAsFlow(
		AppSettings.KEY_ACTIVE_SOURCE_PRESET_ID,
	) {
		activeSourcePresetId
	}.mapLatest { presetId ->
		if (presetId > 0L) {
			sourcePresetsRepository.getById(presetId)
		} else {
			null
		}
	}
	private val currentObservedLocalMangaId: StateFlow<Long?> = combine(
		activeMangaIdFlow,
		mangaDetails,
	) { activeMangaId, details ->
		activeMangaId
			?: details?.local?.manga?.id
			?: details?.toContent()?.takeIf { it.isLocal }?.id
	}.distinctUntilChanged()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	private var baseLoadedDetails: ContentDetails? = null
	private val selectedMetadataSource = MutableStateFlow<MetadataSourceSelection>(MetadataSourceSelection.Base)

	private sealed interface MetadataSourceSelection {
		data object Base : MetadataSourceSelection
	}

	private fun String?.normalizedImageUrl(): String? {
		val normalized = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
		if (!normalized.startsWith("file://", ignoreCase = true)) {
			return normalized
		}
		val filePath = runCatching {
			Uri.parse(normalized).path
		}.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
		return normalized.takeIf { File(filePath).exists() }
	}

	private fun resolveScrobblingStatusOrNull(rawStatus: String?): ScrobblingStatus? {
		if (rawStatus.isNullOrBlank()) {
			return null
		}
		return runCatching {
			ScrobblingStatus.valueOf(rawStatus)
		}.getOrNull()
	}

	private fun initialIsWorkDetails(): Boolean {
		return true
	}

	private fun currentDetailsTitle(): String {
		return cleanSourceSearchQuery(currentDetailsContent()?.title.orEmpty())
	}

	private fun currentReadingSearchTitle(): String {
		return currentDetailsContent()?.readingSearchTitle().orEmpty()
	}

	private fun currentDetailsContent(): Content? {
		return mangaDetails.safeValueOrNull()?.toContent()
			?: baseLoadedDetails?.toContent()
			?: originContent
	}

	private fun Content.readingSearchTitle(): String {
		return stripEntityDisambiguationTitleSuffix(title, listOf(source.name)).trim()
	}

	private fun cleanSourceSearchQuery(value: String): String {
		return stripEntityDisambiguationTitleSuffix(value, knownSearchSourceNames()).trim()
	}

	private fun knownSearchSourceNames(): Set<String> {
		val readingSearchSourceSnapshot = readingSearchSources.safeValueOrNull().orEmpty()
		val enabledSourceInfoSnapshot = allEnabledSourceInfos.safeValueOrNull().orEmpty()
		val activeLocalSourceOptionSnapshot = activeLocalSourceOptions.safeValueOrNull().orEmpty()
		val metadataSourceOptionSnapshot = metadataSourceOptions.safeValueOrNull().orEmpty()
		val readingSourceOptionSnapshot = readingSourceOptions.safeValueOrNull().orEmpty()
		return buildSet {
			currentDetailsContent()?.source?.name?.let(::add)
			baseLoadedDetails?.toContent()?.source?.name?.let(::add)
			originContent?.source?.name?.let(::add)
			readingSearchSourceSnapshot.forEach { add(it.mangaSource.name) }
			enabledSourceInfoSnapshot.forEach { add(it.mangaSource.name) }
			activeLocalSourceOptionSnapshot.forEach { add(it.source.name) }
			metadataSourceOptionSnapshot.mapNotNull { it.source?.name }.forEach(::add)
			readingSourceOptionSnapshot.mapNotNull { it.source?.name }.forEach(::add)
		}
	}

	private fun currentBaseContentType(): ContentType? {
		return baseLoadedDetails?.toContent()?.source
			?.resolveDetailsSource()
			?.getContentType()
			?: mangaDetails.safeValueOrNull()?.toContent()?.source
				?.resolveDetailsSource()
				?.getContentType()
			?: originContent?.source
				?.resolveDetailsSource()
				?.getContentType()
	}

	private fun com.mangaverse.app.parsers.model.ContentSource.resolveDetailsSource(): com.mangaverse.app.parsers.model.ContentSource {
		return selectResolvedDetailsSource(
			original = this,
			enabledSources = allEnabledSourceInfos.value,
			pipelineResolved = contentSourceResolutionPipeline.resolve(ContentSource(name)),
		)
	}

	private fun currentMetadataContentType(): ContentType? {
		return currentBaseContentType()
	}

	private fun currentDetailsContentType(): ContentType? {
		return currentMetadataContentType()
	}

	private fun currentMetadataLanguageCode(): String? {
		return baseLoadedDetails?.toContent()?.source
			?.resolveDetailsSource()
			?.locale
			?.takeIf { it.isNotBlank() }
			?: originContent?.source
				?.resolveDetailsSource()
				?.locale
				?.takeIf { it.isNotBlank() }
	}

	private fun currentReadingLanguageCode(): String? {
		return readingSourceOptions.safeValueOrNull()
			.orEmpty()
			.firstOrNull { it.isSelected }
			?.source
			?.locale
			?.takeIf { it.isNotBlank() }
			?: activeLocalSourceOptions.safeValueOrNull()
				.orEmpty()
				.firstOrNull { it.isActive }
				?.source
				?.resolveDetailsSource()
				?.locale
				?.takeIf { it.isNotBlank() }
			?: baseLoadedDetails?.local?.manga?.source
				?.resolveDetailsSource()
				?.locale
				?.takeIf { it.isNotBlank() }
	}

	private fun String.normalizedLanguageCode(): String {
		return trim()
			.substringBefore('-')
			.substringBefore('_')
			.lowercase(Locale.ROOT)
	}

	private fun isTrackingSource(source: com.mangaverse.app.parsers.model.ContentSource): Boolean {
		return source.name.startsWith("TRACKING_")
	}

	private fun isReadingSearchSourceEligible(
		source: com.mangaverse.app.parsers.model.ContentSource,
	): Boolean {
		return !isTrackingSource(source) && !source.isLocal
	}

	private fun refreshReadingSearchSources() {
		val currentSource = (baseLoadedDetails?.toContent() ?: mangaDetails.value?.toContent() ?: originContent)
			?.source
			?.takeIf { source -> isReadingSearchSourceEligible(source) }
			?.let { source ->
				ContentSourceInfo(
					mangaSource = source,
					isEnabled = true,
					isPinned = false,
				)
			}
		val filtered = buildList {
			currentSource?.let(::add)
			allEnabledSourceInfos.value
				.filter { info -> isReadingSearchSourceEligible(info.mangaSource) }
				.forEach { info ->
					if (none { it.mangaSource.name == info.mangaSource.name }) {
						add(info)
					}
		}
		}
		readingSearchSources.value = filtered
		readingSearchScopeFilters.update { current ->
			if (current.sourceTypes == ALL_SOURCE_TYPES &&
				current.contentKinds == ALL_SEARCH_CONTENT_KINDS &&
				!current.pinnedOnly &&
				!current.hideEmpty
			) {
				current.copy(contentKinds = defaultReadingSearchContentKinds())
			} else {
				current
			}
		}
		if (selectedReadingSearchSource.value !in filtered.map { it.mangaSource.name }.toSet()) {
			selectedReadingSearchSource.value = null
			readingSearchFilterState.value = ReadingSearchFilterState()
		}
	}

	private fun List<ContentSourceInfo>.filterByPreset(
		preset: SourcePreset?,
	): List<ContentSourceInfo> {
		if (preset == null) {
			return this
		}
		return filter { it.mangaSource.name in preset.sources }
	}

	private fun syntheticSource(
		name: String,
		contentType: ContentType,
		locale: String = "",
	): com.mangaverse.app.parsers.model.ContentSource = object : com.mangaverse.app.parsers.model.ContentSource {
		override val name: String = name
		override val locale: String = locale
		override val contentType: ContentType = contentType
	}

	private fun String?.toDisplayDescription(): String? {
		if (isNullOrBlank()) {
			return null
		}
		return runCatching {
			parseAsHtml().sanitize().toString()
		}.getOrElse {
			sanitize().toString()
		}.takeIf { it.isNotBlank() }
	}

	private fun List<Pair<String, String>>.firstMappedValue(vararg keys: String): String? {
		return firstOrNull { (key, value) ->
			value.isNotBlank() && keys.any { expected ->
				key.normalizedMetadataKey().contains(expected)
			}
		}?.second
	}

	private fun String.normalizedMetadataKey(): String {
		return lowercase(Locale.ROOT)
			.replace("：", ":")
			.replace("_", " ")
			.replace("-", " ")
			.replace(" ", "")
	}

	private suspend fun applyEntityContext(
		entityId: Long,
		preferredLocalMangaId: Long? = null,
		initialProjectionLocalMangaId: Long? = null,
		populateSyntheticHeader: Boolean,
	) {
		Log.i(
			DETAILS_TRACE_TAG,
			"entity.apply start entityId=$entityId preferred=$preferredLocalMangaId " +
				"initial=$initialProjectionLocalMangaId populateSyntheticHeader=$populateSyntheticHeader " +
				"activeMangaId=${activeMangaIdFlow.value} displayed=${mangaDetails.value?.toContent().detailsTraceSummary()}",
		)
		val entity = entityGraphRepository.getEntity(entityId) ?: return
		isWorkDetails.value = entity.type == EntityType.WORK
		val cachedProjectionId = (initialProjectionLocalMangaId ?: preferredLocalMangaId)
			?.takeIf { it != 0L }
		val cachedProjection = if (populateSyntheticHeader) {
			cachedProjectionId?.let { projectionId ->
				dataRepository.findContentById(projectionId, withChapters = true)
				}
		} else {
			null
		}
		activeProjectionStoredContentType = cachedProjectionId?.let { projectionId ->
			db.getMangaDao().find(projectionId)?.manga?.contentType?.let(::parseStoredContentType)
		}
		Log.i(
			DETAILS_TRACE_TAG,
			"entity.apply cache entityId=$entityId cachedProjection=${cachedProjection.detailsTraceSummary()} " +
				"storedContentType=$activeProjectionStoredContentType",
		)
		if (populateSyntheticHeader && cachedProjection != null && mangaDetails.value == null) {
			baseLoadedDetails = ContentDetails(cachedProjection)
			Log.i(DETAILS_TRACE_TAG, "entity.apply initialState=cached ${cachedProjection.detailsTraceSummary()}")
			syncDisplayedState()
		}
		if (populateSyntheticHeader && mangaDetails.value == null) {
			val entityCoverUrl = resolveEntityCoverUrl(entityId)
			baseLoadedDetails = ContentDetails(
				cachedProjection ?: Content(
					id = entityId,
					title = entity.primaryName,
					altTitles = emptySet(),
					url = "",
					publicUrl = "",
					rating = 0f,
					contentRating = null,
					coverUrl = entityCoverUrl,
					largeCoverUrl = entityCoverUrl,
					tags = emptySet(),
					state = null,
					authors = emptySet(),
					description = null,
					chapters = null,
					source = syntheticSource(SYNTHETIC_ENTITY_GRAPH_SOURCE, ContentType.MANGA),
				),
			)
			Log.i(
				DETAILS_TRACE_TAG,
				"entity.apply initialState=synthetic entityId=$entityId source=$SYNTHETIC_ENTITY_GRAPH_SOURCE",
			)
			syncDisplayedState()
		}
		val bindings = entityGraphRepository.getBindings(entityId)
		activeEntityContextId = entityId
		activeEntityContextBindings = bindings
		if (entity.type != EntityType.WORK) {
			activeEntityContextBoundLocalId = null
			activeLocalSourceOptions.value = emptyList()
			sessionReadingProjectionLocalMangaId.value = null
			updateSourceOptions()
			entityChapterSourceInfo.value = null
			restoreEntityMetadataSourceSelection(entityId = entityId)
			submitEntityRelationSections(buildEntityRelationSections(entityId))
			return
		}
		val persistedPreferredLocalId = workResolver.selectPreferredProjection(entityId)
		val requestedProjectionLocalId = initialProjectionLocalMangaId?.takeIf { projectionId ->
			bindings.any { binding ->
				binding.isLocalReadingSource() &&
					binding.externalId.toLongOrNull() == projectionId
			}
		}
		val boundLocalId = requestedProjectionLocalId ?: persistedPreferredLocalId?.takeIf { persistedId ->
			bindings.any { binding ->
				binding.isLocalReadingSource() &&
					binding.externalId.toLongOrNull() == persistedId
			}
		} ?: preferredLocalMangaId?.takeIf { preferredId ->
			bindings.any { binding ->
				binding.isLocalReadingSource() &&
					binding.externalId.toLongOrNull() == preferredId
			}
		} ?: bindings.firstOrNull { it.isLocalReadingSource() }?.externalId?.toLongOrNull()
		activeEntityContextBoundLocalId = boundLocalId
		activeProjectionStoredContentType = boundLocalId?.let { projectionId ->
			db.getMangaDao().find(projectionId)?.manga?.contentType?.let(::parseStoredContentType)
		}
		val localBindingCount = bindings.count { binding ->
			binding.isLocalReadingSource()
		}
		android.util.Log.d(
			DETAILS_TRACE_TAG,
			"applyEntityContext: entityId=$entityId, preferredLocalMangaId=$preferredLocalMangaId, " +
				"initialProjectionLocalMangaId=$initialProjectionLocalMangaId, " +
				"persistedPreferredLocalId=$persistedPreferredLocalId, boundLocalId=$boundLocalId, " +
				"populateSyntheticHeader=$populateSyntheticHeader, localBindings=$localBindingCount",
		)
		activeLocalSourceOptions.value = buildActiveLocalSourceOptions(bindings, boundLocalId)
		sessionReadingProjectionLocalMangaId.value = requestedProjectionLocalId ?: boundLocalId
		Log.i(
			DETAILS_TRACE_TAG,
			"entity.apply bindings entityId=$entityId boundLocalId=$boundLocalId requestedProjection=$requestedProjectionLocalId " +
				"activeOptions=${activeLocalSourceOptions.value.map { "${it.mangaId}:${it.source.name}:${it.source.locale}:${it.isActive}" }}",
		)
		updateSourceOptions()
		if (boundLocalId != null && activeMangaIdFlow.value != boundLocalId) {
			currentLoadIntentOverride = ContentIntent.of(boundLocalId)
			activeMangaIdFlow.value = boundLocalId
			loadingJob.cancel()
			loadingJob = doLoad(force = false)
		}
		entityChapterSourceInfo.value = resolveEntityChapterSourceInfo(boundLocalId)
		restoreEntityMetadataSourceSelection(entityId = entityId)
		submitEntityRelationSections(buildEntityRelationSections(entityId))
	}

	private suspend fun refreshActiveEntitySourceOptions() {
		if (activeEntityContextId == null) {
			return
		}
		val activeMangaId = activeMangaIdFlow.value ?: activeEntityContextBoundLocalId
		activeLocalSourceOptions.value = buildActiveLocalSourceOptions(
			bindings = activeEntityContextBindings,
			activeMangaId = activeMangaId,
		)
		entityChapterSourceInfo.value = resolveEntityChapterSourceInfo(activeMangaId)
	}

	fun setSpaceContext(spaceId: SpaceId?) {
		if (detailsSpaceId == spaceId) {
			return
		}
		detailsSpaceId = spaceId
		val entityId = activeEntityContextId ?: return
		viewModelScope.launch {
			val bindings = activeEntityContextBindings
			val boundLocalId = activeEntityContextBoundLocalId
			activeLocalSourceOptions.value = buildActiveLocalSourceOptions(bindings, boundLocalId)
			updateSourceOptions()
			entityChapterSourceInfo.value = resolveEntityChapterSourceInfo(boundLocalId)
			Log.d("DetailsViewModel", "setSpaceContext: entityId=$entityId, spaceId=$spaceId")
		}
	}

	init {
		Log.i(
			DETAILS_TRACE_TAG,
			"vm.init origin=${activeExternalOrigin.detailsTraceSummary()} intentId=${intent.mangaId} " +
				"initialOverride=${initialProjectionIntentOverride?.mangaId} activeMangaId=${activeMangaIdFlow.value}",
		)
		// Apply instant first paint only from the explicit DetailsOrigin payload.
		// Raw intent seed should not predefine current details before real resolution.
		baseLoadedDetails = originContent?.let { ContentDetails(it) }
		syncDisplayedState()
		val initialReadingTitle = currentReadingSearchTitle()
		if (readingSearchQuery.value.isBlank() && initialReadingTitle.isNotBlank()) {
			readingSearchQuery.value = initialReadingTitle
		}
		activeMangaIdFlow.value
			?.let { _ ->
					launchJob(Dispatchers.IO) {
						val observedLocalMangaId = currentObservedLocalMangaIdSnapshot() ?: return@launchJob
						val localEntityId = entityGraphRepository.findEntityByBinding("0", observedLocalMangaId.toString())?.id
							?: entityGraphRepository.findEntityByBinding("local_manga", observedLocalMangaId.toString())?.id
					if (localEntityId != null) {
						restoreEntityMetadataSourceSelection(entityId = localEntityId)
					} else {
						restorePersistedMetadataSourceSelection(observedLocalMangaId)
					}
				}
			}

		launchJob(Dispatchers.Default) {
			mangaDetails.filterNotNull().collect { details ->
				val content = details.toContent()
				val readingTitle = content.readingSearchTitle()
				if (readingSearchQuery.value.isBlank() && readingTitle.isNotBlank()) {
					readingSearchQuery.value = readingTitle
				}
			}
		}

		launchJob(Dispatchers.Default) {
			combine(
				flowOrFallback(emptyList()) { contentSourcesRepository.observeEnabledSources() },
				activeSourcePreset,
			) { sources, preset ->
				sources.filterByPreset(preset)
			}.collect { sources ->
				allEnabledSourceInfos.value = sources
				updateSourceOptions()
				refreshResolvedPresentationState()
				refreshReadingSearchSources()
				allEnabledSourcesLoaded = true
			}
		}

		launchJob(Dispatchers.Default) {
			merge(
				com.mangaverse.app.core.extensions.GlobalExtensionManager.updates,
				mihonExtensionManager.changes,
			).collect {
				refreshActiveEntitySourceOptions()
				updateSourceOptions()
				refreshResolvedPresentationState()
			}
		}

		launchJob(Dispatchers.IO) {
			currentObservedLocalMangaId
				.collect { syncDisplayedState() }
		}

		if (
			!isTemporaryReadOnly &&
			activeExternalOrigin !is com.mangaverse.app.details.ui.model.DetailsOrigin.EntityGraph
		) {
				launchJob(Dispatchers.IO) {
					val localContent = originContent
						?: currentObservedLocalMangaIdSnapshot()?.let { mangaId -> db.getMangaDao().find(mangaId)?.toContent() }
						?: return@launchJob
				val storedContent = dataRepository.storeContentAndReturn(localContent, replaceExisting = false)
				val identity = workResolver.ensureForProjection(
					content = storedContent,
					provenance = com.mangaverse.app.work.domain.WorkIdentityProvenance.USER,
				)
				val entityId = identity.entityId ?: return@launchJob
				applyEntityContext(
					entityId = entityId,
					preferredLocalMangaId = storedContent.id,
					initialProjectionLocalMangaId = storedContent.id,
					populateSyntheticHeader = false,
				)
			}
		}

		if (activeExternalOrigin is com.mangaverse.app.details.ui.model.DetailsOrigin.EntityGraph) {
			launchJob(Dispatchers.IO) {
				applyEntityContext(
					entityId = activeExternalOrigin.entityId,
					preferredLocalMangaId = activeExternalOrigin.preferredLocalMangaId,
					initialProjectionLocalMangaId = activeExternalOrigin.initialProjectionLocalMangaId,
					populateSyntheticHeader = true,
				)
			}
		}

	}

	private fun MetadataSourceSelection.toPersistedSelection(): PersistedMetadataSourceSelection {
		return PersistedMetadataSourceSelection.Base
	}

	private suspend fun persistMetadataSourceSelection(mangaId: Long) {
		dataRepository.setMetadataSourceSelection(
			mangaId = mangaId,
			selection = selectedMetadataSource.value.toPersistedSelection(),
		)
	}

	private suspend fun resolveCurrentMetadataPersistenceMangaId(): Long? {
		val projectionSnapshot = currentWorkProjectionSnapshot()
		return projectionSnapshot.activeLocalMangaId
			?: projectionSnapshot.currentReadingProjectionMangaId
			?: baseLoadedDetails?.local?.manga?.id
			?: resolveCurrentLocalMangaId()
	}

	private suspend fun persistMetadataSourceSelectionForCurrentEntity(
		fallbackMangaId: Long? = null,
	) {
		if (isTemporaryReadOnly) {
			return
		}
		val selection = selectedMetadataSource.value.toPersistedSelection()
		val entityId = resolveContextualEntityId()
		val resolvedFallbackMangaId = fallbackMangaId ?: resolveCurrentMetadataPersistenceMangaId()
		val targetIds = listOfNotNull(resolvedFallbackMangaId)
		android.util.Log.d(
			"DetailsViewModel",
			"persistMetadataSourceSelectionForCurrentEntity: entityId=$entityId, fallbackMangaId=$resolvedFallbackMangaId, " +
				"selection=$selection, fallbackTargetIds=$targetIds",
		)
		if (entityId != null) {
			dataRepository.setEntityMetadataSourceSelection(
				entityId = entityId,
				selection = selection,
			)
		} else {
			targetIds.forEach { mangaId ->
				dataRepository.setMetadataSourceSelection(
					mangaId = mangaId,
					selection = selection,
				)
			}
		}
	}

	private suspend fun persistPreferredLocalSourceForCurrentEntity(mangaId: Long) {
		if (isTemporaryReadOnly) {
			return
		}
		val entityId = resolveContextualEntityId()
		if (entityId != null) {
			dataRepository.setEntityPreferredLocalMangaId(entityId = entityId, mangaId = mangaId)
		}
	}

	private suspend fun restoreEntityMetadataSourceSelection(
		entityId: Long,
		fallbackMangaId: Long? = null,
	) {
		val resolvedFallbackMangaId = fallbackMangaId ?: resolveCurrentMetadataPersistenceMangaId()
		val entitySelection = dataRepository.getEntityMetadataSourceSelection(entityId)
		android.util.Log.d(
			"DetailsViewModel",
			"restoreEntityMetadataSourceSelection: entityId=$entityId, fallbackMangaId=$resolvedFallbackMangaId, entitySelection=$entitySelection",
		)
		if (entitySelection != null) {
			if (entitySelection == PersistedMetadataSourceSelection.Base) {
				if (selectedMetadataSource.value != MetadataSourceSelection.Base) {
					selectedMetadataSource.value = MetadataSourceSelection.Base
					syncDisplayedState()
				}
			}
			return
		}
		resolvedFallbackMangaId?.let { restorePersistedMetadataSourceSelection(it) }
	}

	private suspend fun restorePersistedMetadataSourceSelection(mangaId: Long) {
		val persisted = dataRepository.getMetadataSourceSelection(mangaId)
		android.util.Log.d(
			"DetailsViewModel",
			"restorePersistedMetadataSourceSelection: mangaId=$mangaId, persisted=$persisted",
		)
		if (persisted == PersistedMetadataSourceSelection.Base) {
			if (selectedMetadataSource.value != MetadataSourceSelection.Base) {
				selectedMetadataSource.value = MetadataSourceSelection.Base
				syncDisplayedState()
			}
		}
	}

	private fun mergeActualAndMetadataChapters(
		metadataChapters: List<ContentChapter>,
		actualChapters: List<ContentChapter>,
	): List<ContentChapter> {
		if (actualChapters.isEmpty()) {
			return metadataChapters
		}
		if (metadataChapters.isEmpty()) {
			return actualChapters
		}
		val remainingActual = actualChapters.toMutableList()
		return buildList {
			metadataChapters.forEach { metadataChapter ->
				val match = remainingActual.firstOrNull { actual ->
					actual.id == metadataChapter.id ||
						(
							actual.number > 0f &&
								metadataChapter.number > 0f &&
								actual.number == metadataChapter.number
						) ||
						(
							!actual.title.isNullOrBlank() &&
								actual.title == metadataChapter.title
						)
				}
				if (match != null) {
					add(match)
					remainingActual.remove(match)
				} else {
					add(metadataChapter)
				}
			}
			addAll(remainingActual)
		}
	}

	private suspend fun resolveWorkProjectionContext(mangaId: Long): WorkProjectionContext {
		val identity = workResolver.resolveByMangaId(mangaId)
		val entityId = identity.entityId
		if (entityId == null) {
			return WorkProjectionContext(
				entityId = null,
				requestedMangaId = mangaId,
				preferredLocalMangaId = mangaId,
				persistedLocalMangaId = mangaId,
				candidateMangaIds = listOf(mangaId),
			)
		}
		val localMangaIds = identity.localMangaIds
			.filter { localId -> db.getMangaDao().contains(localId) }
		val persistedLocalMangaId = identity.preferredMangaId
			?.takeIf { preferredId -> db.getMangaDao().contains(preferredId) }
			?: localMangaIds.firstOrNull()
			?: mangaId
		val candidateMangaIds = buildList {
			add(mangaId)
			add(persistedLocalMangaId)
			addAll(localMangaIds)
		}.distinct()
		return WorkProjectionContext(
			entityId = entityId,
			requestedMangaId = mangaId,
			preferredLocalMangaId = persistedLocalMangaId,
			persistedLocalMangaId = persistedLocalMangaId,
			candidateMangaIds = candidateMangaIds.ifEmpty { listOf(mangaId) },
		)
	}

	private fun syncDisplayedState() {
		val base = baseLoadedDetails
		mangaDetails.value = base
		Log.i(
			DETAILS_TRACE_TAG,
			"state.sync base=${base?.toContent().detailsTraceSummary()} displayed=${mangaDetails.value?.toContent().detailsTraceSummary()} " +
				"entityId=$activeEntityContextId activeMangaId=${activeMangaIdFlow.value}",
		)
		updateSourceOptions()
		refreshReadingSearchSources()
		refreshResolvedPresentationState()
		if (activeExternalOrigin !is com.mangaverse.app.details.ui.model.DetailsOrigin.EntityGraph) {
			refreshContextualEntityRelations()
		}
	}

	private fun currentObservedLocalMangaIdSnapshot(): Long? {
		val activeMangaId = activeMangaIdFlow.safeValueOrNull()
		val currentDetails = mangaDetails.safeValueOrNull()
		return activeMangaId
			?: currentDetails?.local?.manga?.id
			?: currentDetails?.toContent()?.takeIf { it.isLocal }?.id
	}

	private fun currentWorkProjectionSnapshot(): CurrentWorkProjectionSnapshot {
		val activeLocalSourceOption = activeLocalSourceOptions.safeValueOrNull().orEmpty().firstOrNull { it.isActive }
		val activeLocalMangaId = activeLocalSourceOption?.mangaId
			?: currentObservedLocalMangaIdSnapshot()
		val currentReadingProjectionMangaId = sessionReadingProjectionLocalMangaId.safeValueOrNull()
			?.takeIf { readingId ->
				activeLocalSourceOptions.safeValueOrNull().orEmpty().any { it.mangaId == readingId }
			}
			?: activeLocalMangaId
		return CurrentWorkProjectionSnapshot(
			activeLocalMangaId = activeLocalMangaId,
			currentReadingProjectionMangaId = currentReadingProjectionMangaId,
		)
	}

	private fun refreshResolvedPresentationState() {
		val metadataLanguage = currentMetadataLanguageCode()?.takeIf { it.isNotBlank() }?.normalizedLanguageCode()
		val readingLanguage = currentReadingLanguageCode()?.takeIf { it.isNotBlank() }?.normalizedLanguageCode()
		resolvedMetadataContentType.value = currentMetadataContentType()
		resolvedMetadataLanguage.value = metadataLanguage
		resolvedReadingLanguage.value = readingLanguage
		Log.i(
			DETAILS_TRACE_TAG,
			"state.presentation metadataSource=${metadataSourceOptions.value.map { "${it.source?.name}:${it.source?.locale}:${it.isSelected}" }} " +
				"readingSource=${readingSourceOptions.value.map { "${it.source?.name}:${it.source?.locale}:${it.isSelected}" }} " +
				"metadataLanguage=$metadataLanguage readingLanguage=$readingLanguage contentType=${currentMetadataContentType()}",
		)
		refreshActiveLocalBrowserContent()
		refreshTranslateActionVisibility(metadataLanguage)
	}

	private fun refreshActiveLocalBrowserContent() {
		val activeLocalId = currentWorkProjectionSnapshot().activeLocalMangaId
		val baseContent = baseLoadedDetails?.toContent()
		if (activeLocalId == null || activeLocalId == baseLoadedDetails?.id) {
			activeLocalBrowserContent.value = baseContent?.takeIf { it.publicUrl.isNotBlank() }
			return
		}
		launchJob(Dispatchers.IO) {
			activeLocalBrowserContent.value = db.getMangaDao()
				.find(activeLocalId)
				?.toContent()
				?.takeIf { it.publicUrl.isNotBlank() }
				?: baseContent?.takeIf { it.publicUrl.isNotBlank() }
		}
	}

	private fun refreshTranslateActionVisibility(metadataLanguage: String?) {
		if (!settings.isDetailsTranslateButtonVisible) {
			showTranslateAction.value = false
			return
		}
		val targetLanguage = currentTargetLang().takeIf { it.isNotBlank() }?.normalizedLanguageCode()
		if (targetLanguage.isNullOrBlank()) {
			showTranslateAction.value = false
			return
		}
		translateAvailabilityJob?.cancel()
		translateAvailabilityJob = viewModelScope.launch(Dispatchers.IO) {
			val details = mangaDetails.safeValueOrNull()
			val sampleText = buildString {
				append(details?.toContent()?.title.orEmpty())
				if (length < 24) {
					append(' ')
					append(details?.description?.toString().orEmpty())
				}
			}.trim()
			val detectedLanguage = if (sampleText.isNotBlank()) {
				detectLanguageViaMlKit(sampleText)?.normalizedLanguageCode()
			} else {
				null
			}
			val effectiveLanguage = metadataLanguage ?: detectedLanguage
			showTranslateAction.value = effectiveLanguage == null || effectiveLanguage != targetLanguage
		}
	}

	private fun updateSourceOptions() {
		val selection = selectedMetadataSource.value
		val baseContent = baseLoadedDetails?.toContent() ?: originContent
		val baseSource = baseContent?.source?.resolveDetailsSource()
		val baseLooksLikeTracking = baseSource?.name?.startsWith("TRACKING_") == true
		val metadata = buildList {
			if (baseSource != null && !baseLooksLikeTracking) {
				add(
					DetailsSourceOption(
						key = "base:${baseSource.name}",
						source = baseSource,
						title = baseContent?.title,
						coverUrl = baseContent?.coverUrl.normalizedImageUrl(),
						isSelected = selection == MetadataSourceSelection.Base,
					),
				)
			}
			if (isEmpty() && baseSource != null) {
				add(
					DetailsSourceOption(
						key = "base:${baseSource.name}",
						source = baseSource,
						title = baseContent?.title,
						coverUrl = baseContent?.coverUrl.normalizedImageUrl(),
						isSelected = true,
					),
				)
			}
		}.distinctBy(DetailsSourceOption::key)
		metadataSourceOptions.value = metadata

		val currentDisplayedDetails = mangaDetails.safeValueOrNull()
		val activeLocalOptions = activeLocalSourceOptions.safeValueOrNull().orEmpty()
		readingSourceOptions.value = if (activeLocalOptions.isNotEmpty()) {
			val selectedReadingProjectionId = sessionReadingProjectionLocalMangaId.safeValueOrNull()
				?.takeIf { projectionId -> activeLocalOptions.any { it.mangaId == projectionId } }
				?: activeLocalOptions.firstOrNull { it.isActive }?.mangaId
			activeLocalOptions.map { option ->
				DetailsSourceOption(
					key = "reading:${option.mangaId}",
					source = option.source,
					targetMangaId = option.mangaId,
					title = option.title,
					isSelected = option.mangaId == selectedReadingProjectionId,
				)
			}
		} else {
			val source = baseSource
				?.takeUnless { it.name.startsWith("TRACKING_") }
				?: currentDisplayedDetails
					?.toContent()
					?.source
					?.resolveDetailsSource()
					?.takeUnless { it.name.startsWith("TRACKING_") }
				?: currentDisplayedDetails
					?.takeIf { it.isLocal }
					?.local
					?.manga
					?.source
			val spaceAllowedTypes = detailsSpaceId?.let(spaceContentPolicy::allowedTypes)
			val currentType = currentBaseContentType()
			val projectionType = source?.resolvedContentTypeForSnapshot() ?: activeProjectionStoredContentType
			val isAllowed = source != null && isDetailsProjectionAllowed(
				currentType = currentType,
				projectionType = projectionType,
				spaceAllowedTypes = spaceAllowedTypes,
			)
			Log.i(
				DETAILS_TRACE_TAG,
				"state.readingCandidate source=${source?.name} sourceLocale=${source?.locale} " +
					"currentType=$currentType projectionType=$projectionType spaceId=$detailsSpaceId " +
					"spaceAllowedTypes=$spaceAllowedTypes storedContentType=$activeProjectionStoredContentType accepted=$isAllowed",
			)
			source
				?.takeIf { isAllowed }
					?.let {
						listOf(
							DetailsSourceOption(
								key = "reading:${it.name}",
								source = it,
								title = baseContent?.title,
								coverUrl = baseContent?.coverUrl.normalizedImageUrl(),
								isSelected = true,
							),
						)
					}.orEmpty()
		}
		updateChapterSourceTabs()
		Log.i(
			DETAILS_TRACE_TAG,
			"state.options base=${baseContent.detailsTraceSummary()} activeLocal=${activeLocalOptions.map { "${it.mangaId}:${it.source.name}:${it.source.locale}:${it.isActive}" }} " +
				"metadata=${metadataSourceOptions.value.map { "${it.key}:${it.source?.name}:${it.isSelected}" }} " +
				"reading=${readingSourceOptions.value.map { "${it.key}:${it.source?.name}:${it.source?.locale}:${it.isSelected}" }}",
		)
	}

	private fun updateChapterSourceTabs() {
		metadataChapterTabs.value = emptyList()

		readingChapterTabs.value = readingSourceOptions.safeValueOrNull().orEmpty().map { option ->
			DetailsChapterSourceTab(
				key = option.key,
				source = option.source,
				targetMangaId = option.targetMangaId,
				url = option.url,
				isSelected = option.isSelected,
			)
		}
		val metadataTabSummary = metadataChapterTabs.value.map { "${it.key}:${it.isSelected}:${it.chapters.size}" }
		val readingTabSummary = readingChapterTabs.value.map { "${it.key}:${it.isSelected}:${it.chapters.size}" }
		android.util.Log.d(
			"DetailsViewModel",
			"updateChapterSourceTabs: metadataTabs=$metadataTabSummary, readingTabs=$readingTabSummary",
		)
	}

	private fun refreshContextualEntityRelations() {
		launchJob(Dispatchers.IO) {
			val entityId = resolveContextualEntityId()
			val sections = if (entityId != null) {
				buildEntityRelationSections(entityId)
			} else {
				emptyList()
			}
			submitEntityRelationSections(sections)
		}
	}

	private suspend fun resolveContextualEntityId(): Long? {
		activeEntityContextId?.let { return it }
		val localMangaId = currentObservedLocalMangaIdSnapshot() ?: baseLoadedDetails?.local?.manga?.id
		if (localMangaId != null) {
			workResolver.resolveByMangaId(localMangaId).entityId?.let { entityId ->
				android.util.Log.d(
					"DetailsViewModel",
					"resolveContextualEntityId: resolved localMangaId=$localMangaId, entityId=$entityId",
				)
				return entityId
			}
		}
		val currentSelection = selectedMetadataSource.value
		android.util.Log.d(
			"DetailsViewModel",
			"resolveContextualEntityId: unresolved, activeMangaId=${activeMangaIdFlow.value}, baseId=${baseLoadedDetails?.id}, selection=$currentSelection",
		)
		return null
	}

	@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
	val history = currentObservedLocalMangaId.flatMapLatest { mangaId ->
		if (mangaId == null) {
			flowOf(null)
		} else {
			flowOrFallback(null) { historyRepository.observeOne(mangaId) }
		}
	}
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	private val readingStateSync: StateFlow<ReaderState?> = combine(
		mangaDetails,
		history,
		selectedBranch,
	) { details, h, branch ->
		val chapter = details?.allChapters?.findChapterByHistory(h)
			if (h != null && chapter != null) {
				val isCompleted = h.percent >= 0.99999f
				if (isCompleted) {
					val branchChapters = details.allChapters
						.filter { it.branch == branch }
						.sortedBy { it.number }
				val index = branchChapters.indexOfFirst { it.id == chapter.id }
				if (index != -1 && index + 1 < branchChapters.size) {
					val nextChapter = branchChapters[index + 1]
					ReaderState(
						chapterId = nextChapter.id,
						page = 0,
						scroll = 0,
					)
					} else {
						ReaderState(
							chapterId = FULLY_READ_CHAPTER_ID,
							page = 0,
							scroll = 0,
					)
				}
			} else {
				ReaderState(h.copy(chapterId = chapter.id))
			}
		} else {
			null
		}
	}
		.onEach { state ->
			readingState.value = state
		}
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)

	@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
	val favouriteCategories = currentObservedLocalMangaId.flatMapLatest { mangaId ->
		if (mangaId == null) {
			flowOf(emptySet())
		} else {
			flowOrFallback(emptySet()) { interactor.observeFavourite(mangaId) }
		}
	}
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptySet())

	@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
	val isStatsAvailable = currentObservedLocalMangaId.flatMapLatest { mangaId ->
		if (mangaId == null) {
			flowOf(false)
		} else {
			flowOrFallback(false) { statsRepository.observeHasStats(mangaId) }
		}
	}
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)

	@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
	val readingRecordSnapshot: StateFlow<ReadingRecordSnapshot> = currentObservedLocalMangaId
		.flatMapLatest { mangaId ->
			if (mangaId == null) {
				flowOf(ReadingRecordSnapshot())
			} else {
				flowOrFallback(ReadingRecordSnapshot()) { readingRecordRepository.observeSnapshot(mangaId) }
			}
		}
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, ReadingRecordSnapshot())

	fun recordDetailsJump(toState: ReaderState, source: String) {
		val fromState = readingState.value ?: return
		if (fromState == toState) return
		val manga = getContentOrNull() ?: return
		launchJob(Dispatchers.Default) {
			readingRecordRepository.recordJumpPoint(
				manga = manga,
				fromState = fromState,
				fromPercent = estimateProgress(fromState),
				toState = toState,
				toPercent = estimateProgress(toState),
				source = source,
				force = true,
			)
		}
	}

	private fun estimateProgress(state: ReaderState): Float {
		val chapters = mangaDetails.value?.allChapters.orEmpty()
		if (chapters.isEmpty()) return 0f
		val chapterIndex = chapters.indexOfFirst { it.id == state.chapterId }
		if (chapterIndex < 0) return 0f
		val chapterProgress = (state.scroll / 10000f).coerceIn(0f, 1f)
		return ((chapterIndex + chapterProgress) / chapters.size).coerceIn(0f, 1f)
	}

	val isMarkedSafe = MutableStateFlow(false)

	val remoteContent = MutableStateFlow<Content?>(null)

	private val cachedTranslatedTitle = MutableStateFlow<String?>(null)
	private val cachedTranslatedDescription = MutableStateFlow<String?>(null)
	val isShowingTranslation = MutableStateFlow(false)
	val hasTranslationCache: StateFlow<Boolean> = combine(
		cachedTranslatedTitle,
		cachedTranslatedDescription,
	) { title, description ->
		title != null || description != null
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, false)
	val translatedTitle: StateFlow<String?> = combine(
		cachedTranslatedTitle,
		isShowingTranslation,
	) { title, isShowing ->
		title.takeIf { isShowing }
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)
	val translatedDescription: StateFlow<String?> = combine(
		cachedTranslatedDescription,
		isShowingTranslation,
	) { description, isShowing ->
		description.takeIf { isShowing }
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, null)
	val isTranslating = MutableStateFlow(false)
	private val translationTextUiState = combine(
		translatedTitle,
		translatedDescription,
		isShowingTranslation,
		hasTranslationCache,
	) { translatedTitle, translatedDescription, isShowingTranslation, hasTranslationCache ->
		TranslationTextUiState(
			translatedTitle = translatedTitle,
			translatedDescription = translatedDescription,
			isShowingTranslation = isShowingTranslation,
			hasTranslationCache = hasTranslationCache,
		)
	}
	val translationUiState: StateFlow<TranslationUiState> = combine(
		translationTextUiState,
		isTranslating,
		showTranslateAction,
	) { textState, isTranslating, showTranslateAction ->
		TranslationUiState(
			translatedTitle = textState.translatedTitle,
			translatedDescription = textState.translatedDescription,
			isShowingTranslation = textState.isShowingTranslation,
			hasTranslationCache = textState.hasTranslationCache,
			isTranslating = isTranslating,
			showTranslateAction = showTranslateAction,
		)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, TranslationUiState())

	val historyInfo: StateFlow<HistoryInfo> = combine(
		mangaDetails,
		selectedBranch,
		history,
		interactor.observeIncognitoMode(manga),
		isMergeRepeatedChapters,
	) { m, b, h, im, mergeRepeated ->
		val estimatedTime = readingTimeUseCase.invoke(m, b, h)
		HistoryInfo(m, b, h, im == TriStateOption.ENABLED, estimatedTime, mergeRepeated)
	}.withErrorHandling()
		.stateIn(
			scope = viewModelScope + Dispatchers.Default,
			started = SharingStarted.Eagerly,
			initialValue = HistoryInfo(null, null, null, false, null),
		)

	val localSize = mangaDetails
		.map { it }  // 获取完整的ContentDetails
		.distinctUntilChanged()
		.combine(localStorageChanges.onStart { emit(null) }) { details, _ -> details }
		.map { details ->
			if (details == null) return@map 0L
			
			val local = details.local
			if (local != null) {
				// 普通本地漫画：计算文件夹大小
				runCatchingCancellable {
					local.file.computeSize()
				}.getOrDefault(0L)
			} else {
				0L
			}
		}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(5000), 0L)

	val relatedContent: StateFlow<List<ContentListModel>> = combine(
		currentObservedLocalMangaId,
		mangaDetails,
	) { localMangaId, details ->
		localMangaId to details
	}.mapLatest { (localMangaId, details) ->
		val seed = localMangaId
			?.let { db.getMangaDao().find(it)?.toContent() }
			?: details?.toContent()?.takeUnless { it.isSyntheticEntityGraphContent() }
		if (seed != null && settings.isRelatedContentEnabled) {
			mangaListMapper.toListModelList(
				manga = relatedContentUseCase(seed).orEmpty(),
				mode = ListMode.GRID,
			)
		} else {
			emptyList()
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, emptyList())

	val tags = manga.mapLatest {
		mangaListMapper.mapTags(it?.tags.orEmpty())
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val branches: StateFlow<List<ContentBranch>> = combine(
		mangaDetails,
		selectedBranch,
		history,
	) { m, b, h ->
		val c = m?.chapters
		if (c.isNullOrEmpty()) {
			return@combine emptyList()
		}
		val currentBranch = m.allChapters.findChapterByHistory(h)?.branch
		c.map { x ->
			ContentBranch(
				name = x.key,
				count = x.value.size,
				isSelected = x.key == b,
				isCurrent = h != null && x.key == currentBranch,
			)
		}.sortedWith(BranchComparator())
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	private val readingStatus: StateFlow<ScrobblingStatus> = combine(
		currentObservedLocalMangaId.flatMapLatest { mangaId ->
			if (mangaId == null) {
				flowOf(null)
			} else {
				flowOrFallback(null) { dataRepository.observeReadingStatus(mangaId) }
			}
		},
		history,
	) { localStatus, history ->
		localStatus
			?: when {
				history == null -> ScrobblingStatus.PLANNED
				com.mangaverse.app.list.domain.ReadingProgress.isCompleted(history.percent) -> ScrobblingStatus.COMPLETED
				else -> ScrobblingStatus.READING
			}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, ScrobblingStatus.PLANNED)

	private val unifiedRating: StateFlow<Float> = MutableStateFlow(0f)

	private val canEditUnifiedRating: StateFlow<Boolean> = MutableStateFlow(false)

	private val detailsHeaderUiState = combine(
		mangaDetails,
		favouriteCategories,
		historyInfo,
	) { mangaDetails, favouriteCategories, historyInfo ->
		DetailsHeaderUiState(
			mangaDetails = mangaDetails,
			favouriteCategories = favouriteCategories,
			historyInfo = historyInfo,
		)
	}.combine(unifiedRating) { header, unifiedRating ->
		header.copy(unifiedRating = unifiedRating)
	}.combine(canEditUnifiedRating) { header, canEditUnifiedRating ->
		header.copy(canEditUnifiedRating = canEditUnifiedRating)
	}.combine(readingStatus) { header, readingStatus ->
		DetailsHeaderUiState(
			mangaDetails = header.mangaDetails,
			favouriteCategories = header.favouriteCategories,
			historyInfo = header.historyInfo,
			readingStatus = readingStatus,
			unifiedRating = header.unifiedRating,
			canEditUnifiedRating = header.canEditUnifiedRating,
		)
	}
	private val detailsPaneSummaryUiState = combine(
		remoteContent,
		branches,
		isStatsAvailable,
		isLoading,
		activeLocalBrowserContent,
	) { remoteContent, branches, isStatsAvailable, isLoading, activeLocalBrowserContent ->
		DetailsPaneSummaryUiState(
			remoteContent = remoteContent,
			branches = branches,
			isStatsAvailable = isStatsAvailable,
			isLoading = isLoading,
			activeLocalBrowserContent = activeLocalBrowserContent,
		)
	}
	val detailsPrimaryUiState: StateFlow<DetailsPrimaryUiState> = combine(
		detailsHeaderUiState,
		detailsPaneSummaryUiState,
		entityRelationSections,
		relatedContent,
		isWorkDetails,
	) { header, pane, entityRelationSections, relatedContent, isWorkDetails ->
		DetailsPrimaryUiState(
			mangaDetails = header.mangaDetails,
			remoteContent = pane.remoteContent,
			relatedContent = relatedContent,
			favouriteCategories = header.favouriteCategories,
			historyInfo = header.historyInfo,
			branches = pane.branches,
			isStatsAvailable = pane.isStatsAvailable,
			readingStatus = header.readingStatus,
			unifiedRating = header.unifiedRating,
			canEditUnifiedRating = header.canEditUnifiedRating,
			isLoading = pane.isLoading,
			entityRelationSections = entityRelationSections,
			activeLocalBrowserContent = pane.activeLocalBrowserContent,
			isWorkDetails = isWorkDetails,
		)
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, DetailsPrimaryUiState())

	val selectedBranchValue: String?
		get() = selectedBranch.value

	init {
		if (initialProjectionIntentOverride?.mangaId?.takeIf { it != 0L } != null || intent.mangaId != 0L || intent.manga != null) {
			loadingJob = doLoad(force = false)
		}
		launchJob(Dispatchers.Default + SkipErrors) {
			val manga = mangaDetails.firstOrNull { !it?.chapters.isNullOrEmpty() } ?: return@launchJob
			val h = history.firstOrNull()
			if (h != null) {
				progressUpdateUseCase(manga.toContent())
			}
		}
		launchJob(Dispatchers.Default) {
			val manga = mangaDetails.firstOrNull { it != null && it.isLocal } ?: return@launchJob
			remoteContent.value = interactor.findRemote(manga.toContent())
		}
		launchJob(Dispatchers.Default) {
			mangaDetails.filterNotNull().collect { details ->
				val override = dataRepository.getOverride(details.id)
				isMarkedSafe.value = override?.contentRating == com.mangaverse.app.parsers.model.ContentRating.SAFE
			}
		}
		launchJob(Dispatchers.Default) {
			mangaDetails.filterNotNull().collect { details ->
				restorePersistedTranslation(details)
			}
		}
	}

	private suspend fun buildEntityRelationSections(entityId: Long): List<EntityRelationSection> {
		val anchorEntity = entityGraphRepository.getEntity(entityId) ?: return emptyList()
		val relations = entityGraphRepository.getRelations(entityId)
		if (relations.isEmpty()) {
			return emptyList()
		}
		val relatedIds = relations.mapNotNull { relation ->
			relation.toEntityId.takeIf { it != entityId } ?: relation.fromEntityId.takeIf { it != entityId }
		}.distinct()
		val relatedEntities = entityGraphRepository.getEntitiesByIds(relatedIds).associateBy(Entity::id)
		val relationSections = relations.groupBy(Relation::type).mapNotNull { (relationType, typeRelations) ->
			if (anchorEntity.type == EntityType.WORK && relationType == RelationType.BELONGS_TO) {
				return@mapNotNull null
			}
			val items = typeRelations.mapNotNull { relation ->
				val relatedId = relation.relatedEntityId(entityId) ?: return@mapNotNull null
				val related = relatedEntities[relatedId] ?: return@mapNotNull null
				EntityRelationItem(
					stableKey = "entity:${related.id}",
					entityId = related.id,
					name = related.primaryName,
					type = related.type,
					coverUrl = resolveEntityCoverUrl(related.id),
					subtitle = null,
					supportingText = null,
					detailLines = emptyList(),
					remoteId = null,
					url = null,
				)
			}.distinctBy(EntityRelationItem::stableKey)
			val titleRes = relationSectionTitleRes(anchorEntity.type, typeRelations.first().type) ?: return@mapNotNull null
			items.takeIf { it.isNotEmpty() }?.let {
				EntityRelationSection(
					titleRes = titleRes,
					items = it,
				)
			}
		}
		if (anchorEntity.type != EntityType.PERSON) {
			return relationSections
		}
		return relationSections
	}

	private fun Long?.orEmptyKey(): String {
		return this?.toString() ?: "none"
	}

	private suspend fun resolveEntityCoverUrl(entityId: Long): String? {
		val bindings = entityGraphRepository.getBindings(entityId)
			.sortedWith(compareByDescending<EntityBinding> { it.isPrimary }.thenByDescending { it.confidence })
		for (binding in bindings) {
			resolveBindingCoverUrl(binding)?.let { return it }
		}
		return null
	}

	private suspend fun resolveBindingCoverUrl(binding: EntityBinding): String? {
		if (!binding.isLocalReadingSource()) {
			return null
		}
		val localMangaId = binding.externalId.toLongOrNull() ?: return null
		val localManga = db.getMangaDao().find(localMangaId)?.manga ?: return null
		return localManga.largeCoverUrl.ifNullOrEmpty { localManga.coverUrl }.normalizedImageUrl()
	}

	private fun relationSectionTitleRes(
		anchorType: EntityType,
		relationType: RelationType,
	): Int? = when (relationType) {
		RelationType.HAS_CHARACTER -> when (anchorType) {
			EntityType.CHARACTER -> null
			else -> R.string.entity_graph_section_characters
		}
		RelationType.CREATED_BY -> when (anchorType) {
			EntityType.PERSON, EntityType.ORGANIZATION -> R.string.entity_graph_section_created_works
			else -> R.string.entity_graph_section_creators
		}
		RelationType.RELATED_TO -> R.string.entity_graph_section_related_entities
		RelationType.VOICED_BY -> when (anchorType) {
			EntityType.PERSON -> R.string.entity_graph_section_voiced_characters
			else -> R.string.entity_graph_section_voice_actors
		}
		RelationType.BELONGS_TO -> R.string.entity_graph_section_parent_work
	}

	private fun Relation.relatedEntityId(anchorEntityId: Long): Long? {
		return toEntityId.takeIf { it != anchorEntityId } ?: fromEntityId.takeIf { it != anchorEntityId }
	}

	private suspend fun buildActiveLocalSourceOptions(
		bindings: List<EntityBinding>,
		activeMangaId: Long?,
	): List<ActiveLocalSourceOption> {
		val currentType = activeMangaId
			?.let { localMangaId ->
				db.getMangaDao().find(localMangaId)?.manga?.let { manga ->
					parseStoredContentType(manga.contentType) ?: ContentSource(manga.source).resolvedContentTypeForSnapshot()
				}
			}
			?: currentBaseContentType()
		val spaceAllowedTypes = detailsSpaceId?.let(spaceContentPolicy::allowedTypes)
		val localMangaIds = bindings.asSequence()
			.filter { it.isLocalReadingSource() }
			.mapNotNull { it.externalId.toLongOrNull() }
			.distinct()
			.toList()
		val localMangaOptions = localMangaIds.mapNotNull { localMangaId ->
			val manga = db.getMangaDao().find(localMangaId)?.manga ?: return@mapNotNull null
			if (manga.source.startsWith("TRACKING_")) {
				return@mapNotNull null
			}
			val source = ContentSource(manga.source).resolveDetailsSource()
			val projectionType = parseStoredContentType(manga.contentType)
				?: source.resolvedContentTypeForSnapshot()
			if (!isDetailsProjectionAllowed(currentType, projectionType, spaceAllowedTypes)) {
				return@mapNotNull null
			}
			ActiveLocalSourceOption(
				mangaId = localMangaId,
				title = manga.title,
				source = source,
				isActive = localMangaId == activeMangaId,
			)
		}
		if (localMangaOptions.size <= 1) {
			return emptyList()
		}
		return localMangaOptions
	}

	private fun parseStoredContentType(value: String?): ContentType? {
		return value?.let { raw -> runCatching { ContentType.valueOf(raw) }.getOrNull() }
	}

	private fun updateActiveLocalSourceSelection(activeMangaId: Long) {
		activeLocalSourceOptions.value = activeLocalSourceOptions.value.map { option ->
			option.copy(isActive = option.mangaId == activeMangaId)
		}
		updateSourceOptions()
	}

	private fun submitEntityRelationSections(sections: List<EntityRelationSection>) {
		pendingEntityRelationSections.tryEmit(sections)
	}

	private suspend fun resolveEntityChapterSourceInfo(
		mangaId: Long?,
		activeProjectionMangaId: Long? = null,
		currentReadingProjectionMangaId: Long? = null,
	): EntityChapterSourceInfo {
		val manga = mangaId?.let { localMangaId ->
			db.getMangaDao().find(localMangaId)?.manga
		}
		val source = manga?.source?.let(::ContentSource)
		val projectionType = manga?.let { localManga ->
			parseStoredContentType(localManga.contentType)
				?: source?.resolvedContentTypeForSnapshot()
		}
		val isVisibleInDetails = isDetailsProjectionAllowed(
			currentType = projectionType,
			projectionType = projectionType,
			spaceAllowedTypes = detailsSpaceId?.let(spaceContentPolicy::allowedTypes),
		)
		val projectionSnapshot = currentWorkProjectionSnapshot()
		return EntityChapterSourceInfo(
			source = source?.takeIf { isVisibleInDetails },
			projectionTitle = manga?.title?.takeIf { isVisibleInDetails },
			projectionCount = if (isVisibleInDetails) {
				activeLocalSourceOptions.value.size.coerceAtLeast(if (manga != null) 1 else 0)
			} else {
				0
			},
			activeProjectionMangaId = (activeProjectionMangaId ?: projectionSnapshot.activeLocalMangaId)
				.takeIf { isVisibleInDetails },
			currentReadingProjectionMangaId = (
				currentReadingProjectionMangaId ?: projectionSnapshot.currentReadingProjectionMangaId
			).takeIf { isVisibleInDetails },
		)
	}

	fun selectActiveLocalSource(mangaId: Long) {
		if (activeLocalSourceOptions.value.none { it.mangaId == mangaId }) {
			return
		}
		if (activeMangaIdFlow.value == mangaId) {
			// Already active; persist preferred source and clear any temporary session projection
			launchJob(Dispatchers.IO) {
				persistPreferredLocalSourceForCurrentEntity(mangaId)
			}
			if (sessionReadingProjectionLocalMangaId.value != mangaId) {
				sessionReadingProjectionLocalMangaId.value = mangaId
				launchJob(Dispatchers.IO) {
					entityChapterSourceInfo.value = resolveEntityChapterSourceInfo(mangaId)
				}
				updateSourceOptions()
			}
			return
		}
		sessionReadingProjectionLocalMangaId.value = mangaId
		currentLoadIntentOverride = ContentIntent.of(mangaId)
		activeMangaIdFlow.value = mangaId
		selectedBranch.value = null
		selectedMetadataSource.value = MetadataSourceSelection.Base
		updateActiveLocalSourceSelection(mangaId)
		syncDisplayedState()
		loadingJob.cancel()
		launchJob(Dispatchers.IO) {
			persistPreferredLocalSourceForCurrentEntity(mangaId)
			persistMetadataSourceSelectionForCurrentEntity(fallbackMangaId = mangaId)
			entityChapterSourceInfo.value = resolveEntityChapterSourceInfo(mangaId)
			loadingJob = doLoad(force = false)
		}
	}

	fun removeActiveLocalSource(mangaId: Long) {
		launchJob(Dispatchers.IO) {
			val entityId = resolveContextualEntityId() ?: return@launchJob
			val bindings = entityGraphRepository.getBindings(entityId)
			val localBindings = bindings.filter {
				it.isLocalReadingSource() && it.externalId.toLongOrNull() != null
			}
			android.util.Log.i("DetailsVM", "removeActiveLocalSource: entityId=$entityId mangaId=$mangaId localBindings=${localBindings.map { it.externalId }} activeMangaId=${activeMangaIdFlow.value}")
			if (localBindings.isEmpty()) {
				return@launchJob
			}
			localBindings.firstOrNull { it.externalId.toLongOrNull() == mangaId } ?: return@launchJob
			entityGraphRepository.splitLocalWorkProjection(mangaId)
			val nextActiveMangaId = if (activeMangaIdFlow.value == mangaId) {
				localBindings.firstNotNullOfOrNull { binding ->
					binding.externalId.toLongOrNull()?.takeIf { it != mangaId }
				}
			} else {
				activeMangaIdFlow.value
			}
			if (nextActiveMangaId != null && nextActiveMangaId != activeMangaIdFlow.value) {
				currentLoadIntentOverride = ContentIntent.of(nextActiveMangaId)
				activeMangaIdFlow.value = nextActiveMangaId
				selectedBranch.value = null
				selectedMetadataSource.value = MetadataSourceSelection.Base
				updateActiveLocalSourceSelection(nextActiveMangaId)
				syncDisplayedState()
				persistPreferredLocalSourceForCurrentEntity(nextActiveMangaId)
				persistMetadataSourceSelectionForCurrentEntity(fallbackMangaId = nextActiveMangaId)
				loadingJob.cancel()
				loadingJob = doLoad(force = false)
			}
			refreshEntityBoundLocalSources(
				entityId = entityId,
				activeMangaId = nextActiveMangaId ?: return@launchJob,
			)
		}
	}

	fun selectReadingProjection(mangaId: Long) {
		if (activeLocalSourceOptions.value.none { it.mangaId == mangaId }) {
			return
		}
		if (sessionReadingProjectionLocalMangaId.value == mangaId) {
			return
		}
			sessionReadingProjectionLocalMangaId.value = mangaId
			launchJob(Dispatchers.IO) {
				entityChapterSourceInfo.value = resolveEntityChapterSourceInfo(currentWorkProjectionSnapshot().activeLocalMangaId)
			}
			updateSourceOptions()
		}

	fun selectMetadataSource(option: DetailsSourceOption) {
		if (option.source != null) {
			if (selectedMetadataSource.value == MetadataSourceSelection.Base) {
				return
			}
			selectedMetadataSource.value = MetadataSourceSelection.Base
			syncDisplayedState()
			launchJob(Dispatchers.IO) {
				persistMetadataSourceSelectionForCurrentEntity()
				refreshContextualEntityRelations()
			}
		}
	}


	fun setReadingSearchSource(sourceName: String?) {
		if (selectedReadingSearchSource.value == sourceName) {
			return
		}
		selectedReadingSearchSource.value = sourceName
		readingSearchSections.value = emptyList()
		readingSearchLoading.value = false
		readingSearchHasSearched.value = false
		readingSearchState.value = null
		if (sourceName == null) {
			readingSearchFilterState.value = ReadingSearchFilterState()
		} else {
			loadReadingSearchFilters(sourceName)
		}
	}

	fun updateReadingSearchQuery(query: String) {
		readingSearchQuery.value = query
	}

	fun toggleReadingSearchSourceType(type: SourceType) {
		readingSearchScopeFilters.update { current ->
			val updated = current.sourceTypes.toMutableSet().apply {
				if (!add(type)) {
					remove(type)
				}
			}.ifEmpty { ALL_SOURCE_TYPES }
			current.copy(sourceTypes = updated)
		}
	}

	fun toggleReadingSearchContentKind(kind: SearchContentKind) {
		readingSearchScopeFilters.update { current ->
			val updated = current.contentKinds.toMutableSet().apply {
				if (!add(kind)) {
					remove(kind)
				}
			}.ifEmpty { ALL_SEARCH_CONTENT_KINDS }
			current.copy(contentKinds = updated)
		}
	}

	fun setReadingSearchPinnedOnly(enabled: Boolean) {
		readingSearchScopeFilters.update { it.copy(pinnedOnly = enabled) }
	}

	fun setReadingSearchHideEmpty(enabled: Boolean) {
		readingSearchScopeFilters.update { it.copy(hideEmpty = enabled) }
	}

	fun setActiveLanguagePreset(presetId: Long) {
		if (settings.activeSourcePresetId != presetId) {
			settings.activeSourcePresetId = presetId
		}
	}

	fun setReadingSearchSortOrder(sortOrder: SortOrder) {
		readingSearchFilterState.update { state ->
			if (state.source == null) return@update state
			state.copy(selectedSortOrder = sortOrder)
		}
	}

	fun setReadingSearchAuthor(author: String?) {
		readingSearchFilterState.update { state ->
			if (state.source == null) return@update state
			state.copy(
				listFilter = state.listFilter.copy(author = author?.trim()?.takeIf { it.isNotEmpty() }),
			)
		}
	}

	fun setReadingSearchLocale(locale: Locale?) {
		readingSearchFilterState.update { state ->
			if (state.source == null) return@update state
			state.copy(listFilter = state.listFilter.copy(locale = locale))
		}
	}

	fun toggleReadingSearchState(value: ContentState, isSelected: Boolean) {
		readingSearchFilterState.update { state ->
			if (state.source == null) return@update state
			state.copy(
				listFilter = state.listFilter.copy(
					states = if (isSelected) state.listFilter.states + value else state.listFilter.states - value,
				),
			)
		}
	}

	fun toggleReadingSearchContentType(value: ContentType, isSelected: Boolean) {
		readingSearchFilterState.update { state ->
			if (state.source == null) return@update state
			state.copy(
				listFilter = state.listFilter.copy(
					types = if (isSelected) state.listFilter.types + value else state.listFilter.types - value,
				),
			)
		}
	}

	fun toggleReadingSearchTag(value: com.mangaverse.app.parsers.model.ContentTag, isSelected: Boolean, excludeMode: Boolean) {
		readingSearchFilterState.update { state ->
			if (state.source == null) return@update state
			val tagGroup = state.filterOptions.effectiveTagGroups.firstOrNull { value in it.tags }
			if (excludeMode) {
				val newTagsExclude = when {
					tagGroup?.isExclusive == true -> {
						val tagsWithoutGroup = state.listFilter.tagsExclude - tagGroup.tags
						if (isSelected) tagsWithoutGroup + value else state.listFilter.tagsExclude - value
					}
					state.capabilities.isMultipleTagsSupported -> {
						if (isSelected) state.listFilter.tagsExclude + value else state.listFilter.tagsExclude - value
					}
					else -> {
						if (isSelected) setOf(value) else emptySet()
					}
				}
				state.copy(
					listFilter = state.listFilter.copy(
						tags = state.listFilter.tags - newTagsExclude,
						tagsExclude = newTagsExclude,
					),
				)
			} else {
				val newTags = when {
					tagGroup?.isExclusive == true -> {
						val tagsWithoutGroup = state.listFilter.tags - tagGroup.tags
						if (isSelected) tagsWithoutGroup + value else state.listFilter.tags - value
					}
					state.capabilities.isMultipleTagsSupported -> {
						if (isSelected) state.listFilter.tags + value else state.listFilter.tags - value
					}
					else -> {
						if (isSelected) setOf(value) else emptySet()
					}
				}
				state.copy(
					listFilter = state.listFilter.copy(
						tags = newTags,
						tagsExclude = state.listFilter.tagsExclude - newTags,
					),
				)
			}
		}
	}

	fun resetReadingSearchFilters() {
		readingSearchFilterState.update { state ->
			if (state.source == null) return@update state
			state.copy(
				selectedSortOrder = state.defaultReadingSearchSortOrder(),
				listFilter = ContentListFilter.EMPTY,
				errorMessage = null,
			)
		}
	}

	fun isReadingSearchTextInputTag(tag: com.mangaverse.app.parsers.model.ContentTag): Boolean {
		return tag.key.startsWith("text:")
	}

	fun getReadingSearchTextInputLabel(tag: com.mangaverse.app.parsers.model.ContentTag): String {
		return tag.title.removePrefix("📝 ")
	}

	fun getReadingSearchTextInputValue(tag: com.mangaverse.app.parsers.model.ContentTag): String? {
		val baseKey = tag.key
		return readingSearchFilterState.value.listFilter.tags
			.find { it.key.startsWith(baseKey) && it.key.contains("=") }
			?.key
			?.substringAfter("=")
	}

	fun setReadingSearchTextInputValue(originalTag: com.mangaverse.app.parsers.model.ContentTag, value: String) {
		readingSearchFilterState.update { state ->
			if (state.source == null) return@update state
			val baseKey = originalTag.key
			val filteredTags = state.listFilter.tags.filter { !it.key.startsWith(baseKey) }.toSet()
			val newTags = if (value.isNotBlank()) {
				val tagWithValue = com.mangaverse.app.parsers.model.ContentTag(
					title = "${originalTag.title.removePrefix("📝 ")}: $value",
					key = "$baseKey=$value",
					source = originalTag.source,
				)
				filteredTags + tagWithValue
			} else {
				filteredTags
			}
			state.copy(listFilter = state.listFilter.copy(tags = newTags))
		}
	}

	private fun List<Content>.withCurrentReadingSourceResult(
		currentContent: Content?,
		sourceInfo: ContentSourceInfo,
		query: String,
	): List<Content> {
		if (currentContent == null || currentContent.source.name != sourceInfo.mangaSource.name) {
			return this
		}
		val normalizedQuery = query.trim()
		val matchesQuery = normalizedQuery.isBlank() ||
			currentContent.title.contains(normalizedQuery, ignoreCase = true) ||
			normalizedQuery.contains(currentContent.title, ignoreCase = true)
		if (!matchesQuery || any { it.id == currentContent.id || it.url == currentContent.url }) {
			return this
		}
		return listOf(currentContent) + this
	}

	private fun com.mangaverse.app.core.parser.ContentRepository.resolveReadingSearchSortOrder(): SortOrder {
		return when {
			SortOrder.RELEVANCE in sortOrders -> SortOrder.RELEVANCE
			SortOrder.POPULARITY in sortOrders -> SortOrder.POPULARITY
			SortOrder.ALPHABETICAL in sortOrders -> SortOrder.ALPHABETICAL
			else -> defaultSortOrder
		}
	}

	private fun replaceReadingSearchSection(
		sourceIndex: Int,
		section: ReadingSearchSectionUiState,
	) {
		readingSearchSections.update { sections ->
			if (sourceIndex !in sections.indices) {
				return@update sections
			}
			sections.toMutableList().also { updated ->
				updated[sourceIndex] = section
			}
		}
	}

	fun searchReadingBindings() {
		readingSearchJob?.cancel()
		val generation = ++readingSearchGeneration
		val scopeFilter = readingSearchScopeFilters.value
		val availableSources = readingSearchSources.value
		// If allEnabledSourceInfos hasn't been populated yet (race condition during init),
		// defer the search until the full source list becomes available.
		if (!allEnabledSourcesLoaded) {
			Log.d(READING_SEARCH_LOG_TAG, "defer search: allEnabledSourceInfos not yet loaded, sources=${availableSources.size}")
			readingSearchLoading.value = true
			readingSearchHasSearched.value = false
			readingSearchState.value = LocalSearchState.Loading
			launchJob(Dispatchers.Default) {
				// Wait up to 3s for the source list to load, then proceed anyway
				kotlinx.coroutines.withTimeoutOrNull(3000L) {
					while (!allEnabledSourcesLoaded && generation == readingSearchGeneration) {
						kotlinx.coroutines.delay(100)
					}
				}
				if (generation != readingSearchGeneration) return@launchJob
				searchReadingBindings()
			}
			return
		}
		val sources = availableSources.filter { sourceInfo ->
			val source = sourceInfo.mangaSource
			val sourceType = sourceTypeIdentifier.getSourceType(source.name)
			sourceType in scopeFilter.sourceTypes &&
				scopeFilter.contentKinds.any { kind -> kind.matches(source) } &&
				(!scopeFilter.pinnedOnly || sourceInfo.isPinned)
		}
		if (sources.isEmpty()) {
			readingSearchSections.value = emptyList()
			readingSearchLoading.value = false
			readingSearchHasSearched.value = true
			readingSearchState.value = LocalSearchState.Loaded(emptyList())
			Log.d(READING_SEARCH_LOG_TAG, "skip search: no sources after scope filter")
			return
		}
		val query = cleanSourceSearchQuery(readingSearchQuery.value).ifBlank { currentReadingSearchTitle() }
		if (query != readingSearchQuery.value) {
			readingSearchQuery.value = query
		}
		val currentContent = (baseLoadedDetails?.toContent() ?: mangaDetails.value?.toContent() ?: originContent)
			?.takeUnless { it.source.name.startsWith("TRACKING_") }
		readingSearchJob = launchJob(Dispatchers.IO) {
			val searchStartedAt = SystemClock.elapsedRealtime()
			Log.d(
				READING_SEARCH_LOG_TAG,
				"start query=${query.take(80)} sources=${sources.size} parallelism=$READING_SEARCH_MAX_PARALLELISM " +
					"timeoutMs=$SOURCE_SEARCH_TIMEOUT_MS hideEmpty=${scopeFilter.hideEmpty}",
			)
			readingSearchLoading.value = true
			readingSearchHasSearched.value = false
			readingSearchState.value = LocalSearchState.Loading
			readingSearchSections.value = sources.map { sourceInfo ->
				ReadingSearchSectionUiState(source = sourceInfo, isPending = true)
			}
			val semaphore = Semaphore(READING_SEARCH_MAX_PARALLELISM)
			supervisorScope {
				sources.mapIndexed { sourceIndex, sourceInfo ->
					async {
						semaphore.withPermit {
							if (generation != readingSearchGeneration) {
								return@withPermit
							}
							val section = try {
								val items = withTimeout(SOURCE_SEARCH_TIMEOUT_MS) {
									val sourceStartedAt = SystemClock.elapsedRealtime()
									val repository = mangaRepositoryFactory.create(sourceInfo.mangaSource)
									replaceReadingSearchSection(
										sourceIndex,
										ReadingSearchSectionUiState(source = sourceInfo, isLoading = true),
									)
									Log.d(
										READING_SEARCH_LOG_TAG,
										"source start index=$sourceIndex source=${sourceInfo.mangaSource.name} " +
											"repo=${repository.javaClass.simpleName}",
									)
									if (!repository.filterCapabilities.isSearchSupported) {
										Log.d(
											READING_SEARCH_LOG_TAG,
											"source unsupported index=$sourceIndex source=${sourceInfo.mangaSource.name} " +
												"elapsedMs=${SystemClock.elapsedRealtime() - sourceStartedAt}",
										)
										return@withTimeout emptyList()
									}
									val listStartedAt = SystemClock.elapsedRealtime()
									val list = repository.getList(
										offset = 0,
										order = repository.resolveReadingSearchSortOrder(),
										filter = ContentListFilter(query = query),
									).take(20)
									Log.d(
										READING_SEARCH_LOG_TAG,
										"source list index=$sourceIndex source=${sourceInfo.mangaSource.name} " +
											"count=${list.size} elapsedMs=${SystemClock.elapsedRealtime() - listStartedAt}",
									)
									val detailsStartedAt = SystemClock.elapsedRealtime()
									val detailed = list.map { content ->
										runCatchingCancellable {
											repository.getDetails(content)
										}.getOrDefault(content)
									}
									Log.d(
										READING_SEARCH_LOG_TAG,
										"source details index=$sourceIndex source=${sourceInfo.mangaSource.name} " +
											"count=${detailed.size} elapsedMs=${SystemClock.elapsedRealtime() - detailsStartedAt} " +
											"totalMs=${SystemClock.elapsedRealtime() - sourceStartedAt}",
									)
									detailed
								}
								Log.d(
									READING_SEARCH_LOG_TAG,
									"source success index=$sourceIndex source=${sourceInfo.mangaSource.name} " +
										"items=${items.size}",
								)
								ReadingSearchSectionUiState(
									source = sourceInfo,
									items = items.withCurrentReadingSourceResult(currentContent, sourceInfo, query),
									isLoading = false,
								)
							} catch (throwable: TimeoutCancellationException) {
								Log.w(
									READING_SEARCH_LOG_TAG,
									"source failed index=$sourceIndex source=${sourceInfo.mangaSource.name} " +
										"timeout=true error=${throwable.javaClass.name}:${throwable.message}",
									throwable,
								)
								ReadingSearchSectionUiState(
									source = sourceInfo,
									isLoading = false,
									errorMessage = throwable.localizedMessage ?: throwable.javaClass.simpleName,
								)
							} catch (throwable: Throwable) {
								if (throwable is CancellationException) {
									throw throwable
								}
								Log.w(
									READING_SEARCH_LOG_TAG,
									"source failed index=$sourceIndex source=${sourceInfo.mangaSource.name} " +
										"timeout=false error=${throwable.javaClass.name}:${throwable.message}",
									throwable,
								)
								ReadingSearchSectionUiState(
									source = sourceInfo,
									isLoading = false,
									errorMessage = throwable.localizedMessage ?: throwable.javaClass.simpleName,
								)
							}
							if (generation != readingSearchGeneration) {
								return@withPermit
							}
							replaceReadingSearchSection(sourceIndex, section)
						}
					}
				}.awaitAll()
			}
			if (generation != readingSearchGeneration) {
				return@launchJob
			}
			readingSearchLoading.value = false
			readingSearchHasSearched.value = true
			val finalSections = if (scopeFilter.hideEmpty) {
				readingSearchSections.value.filter { it.items.isNotEmpty() }
			} else {
				readingSearchSections.value
			}
			readingSearchSections.value = finalSections
			readingSearchState.value = LocalSearchState.Loaded(finalSections.flatMap { it.items })
			Log.d(
				READING_SEARCH_LOG_TAG,
				"finish sources=${sources.size} visibleSections=${finalSections.size} " +
					"items=${finalSections.sumOf { it.items.size }} elapsedMs=${SystemClock.elapsedRealtime() - searchStartedAt}",
			)
		}
	}

	private fun defaultReadingSearchContentKinds(): Set<SearchContentKind> {
		return when (currentDetailsContentType()) {
			ContentType.VIDEO, ContentType.HENTAI_VIDEO -> setOf(SearchContentKind.VIDEO)
			ContentType.NOVEL, ContentType.HENTAI_NOVEL -> setOf(SearchContentKind.NOVEL)
			null -> ALL_SEARCH_CONTENT_KINDS
			else -> setOf(SearchContentKind.MANGA)
		}
	}

	private fun loadReadingSearchFilters(sourceName: String) {
		val sourceInfo = readingSearchSources.value.firstOrNull { it.mangaSource.name == sourceName }
		if (sourceInfo == null) {
			readingSearchFilterState.value = ReadingSearchFilterState()
			return
		}
		readingSearchFilterState.value = ReadingSearchFilterState(
			source = sourceInfo,
			isLoading = true,
		)
		launchJob(Dispatchers.IO) {
			val repository = mangaRepositoryFactory.create(sourceInfo.mangaSource)
			val sortOrders = repository.sortOrders.toList().sortedBy { it.ordinal }
			val defaultSortOrder = repository.resolveReadingSearchSortOrder()
			val optionsResult = runCatchingCancellable {
				repository.getFilterOptions()
			}
			if (selectedReadingSearchSource.value != sourceName) {
				return@launchJob
			}
			readingSearchFilterState.value = optionsResult.fold(
				onSuccess = { options ->
					ReadingSearchFilterState(
						source = sourceInfo,
						capabilities = repository.filterCapabilities,
						filterOptions = options,
						sortOrders = sortOrders,
						selectedSortOrder = defaultSortOrder,
					)
				},
				onFailure = { error ->
					ReadingSearchFilterState(
						source = sourceInfo,
						capabilities = repository.filterCapabilities,
						sortOrders = sortOrders,
						selectedSortOrder = defaultSortOrder,
						errorMessage = error.localizedMessage ?: error.javaClass.simpleName,
					)
				},
			)
		}
	}

	private fun ReadingSearchFilterState.defaultReadingSearchSortOrder(): SortOrder? {
		return sortOrders.firstOrNull { it == SortOrder.RELEVANCE } ?: selectedSortOrder ?: sortOrders.firstOrNull()
	}

	private fun ReadingSearchFilterState.toUiState(): ReadingSearchFilterUiState {
		val sortedLocales = filterOptions.availableLocales
			.sortedBy { it.getDisplayName(it).ifBlank { it.toLanguageTag() } }
		return ReadingSearchFilterUiState(
			hasSelectedSource = source != null,
			isLoading = isLoading,
			errorMessage = errorMessage,
			sortOrders = sortOrders,
			selectedSortOrder = selectedSortOrder,
			tagGroups = filterOptions.effectiveTagGroups.map { group ->
				UiTagGroup(
					title = group.title,
					tags = group.tags,
					selected = group.tags.intersect(listFilter.tags),
					isExclusive = group.isExclusive,
				)
			},
			excludedTagGroups = filterOptions.effectiveTagGroups.map { group ->
				UiTagGroup(
					title = group.title,
					tags = group.tags,
					selected = group.tags.intersect(listFilter.tagsExclude),
					isExclusive = group.isExclusive,
				)
			},
			contentTypes = filterOptions.availableContentTypes.toList().sortedBy { it.ordinal },
			selectedContentTypes = listFilter.types,
			states = filterOptions.availableStates.toList().sortedBy { it.ordinal },
			selectedStates = listFilter.states,
			locales = if (sortedLocales.isNotEmpty()) listOf(null) + sortedLocales else emptyList(),
			selectedLocale = listFilter.locale,
			author = listFilter.author,
			canSearchByAuthor = capabilities.isAuthorSearchSupported,
			supportsTagExclusion = capabilities.isTagsExclusionSupported,
			appliedFilterCount = listFilter.appliedFilterCount(),
		)
	}

	private fun ContentListFilter.appliedFilterCount(): Int {
		var count = 0
		count += tags.size
		count += tagsExclude.size
		count += states.size
		count += types.size
		if (locale != null) count++
		if (!author.isNullOrBlank()) count++
		return count
	}

	private fun com.mangaverse.app.core.parser.ContentRepository.buildReadingSearchFilter(
		query: String,
		baseFilter: ContentListFilter,
	): ContentListFilter {
		var filter = baseFilter.copy(query = query.takeIf { it.isNotBlank() })
		if (!filter.author.isNullOrBlank() && !filterCapabilities.isAuthorSearchSupported) {
			filter = filter.copy(author = null)
		}
		if (!filter.query.isNullOrBlank() && filter.hasNonSearchOptions() && !filterCapabilities.isSearchWithFiltersSupported) {
			filter = filter.copy(query = null)
		}
		if (!filter.query.isNullOrBlank() && !filterCapabilities.isSearchSupported) {
			filter = filter.copy(query = null)
		}
		return filter
	}

	fun bindReadingCandidate(content: Content, onComplete: (() -> Unit)? = null) {
		launchJob(Dispatchers.IO + SkipErrors) {
			var bindingSucceeded = false
			try {
				val targetEntityId = activeEntityContextId ?: resolveContextualEntityId()
				if (targetEntityId == null) {
					errorEvent.call(IllegalStateException("Unable to resolve the current Work"))
					return@launchJob
				}
				val bindingResult = attachReadingSourceToEntityUseCase.attachToEntity(
					targetEntityId = targetEntityId,
					newContent = content,
				)
				if (bindingResult !is WorkProjectionBindingResult.Success) {
					val conflict = bindingResult as WorkProjectionBindingResult.Conflict
					Log.w(
						DETAILS_TRACE_TAG,
						"reading source bind rejected: targetEntityId=$targetEntityId " +
							"projectionId=${conflict.projectionId} reason=${conflict.reason}",
					)
					errorEvent.call(IllegalStateException("Reading source binding failed: ${conflict.reason}"))
					return@launchJob
				}
				val targetContent = bindingResult.projection
				refreshEntityBoundLocalSources(
					entityId = bindingResult.entityId,
					activeMangaId = targetContent.id,
				)
				activeMangaIdFlow.value = targetContent.id
				currentLoadIntentOverride = ContentIntent.of(targetContent.id)
				loadingJob.cancel()
				loadingJob = doLoad(force = true)
				bindingSucceeded = true
			} finally {
				if (bindingSucceeded) {
					withContext(Dispatchers.Main) {
						onComplete?.invoke()
					}
				}
			}
		}
	}

	private suspend fun refreshEntityBoundLocalSources(
		entityId: Long,
		activeMangaId: Long,
	) {
		val identity = workResolver.resolveByEntityId(entityId) ?: return
		if (activeMangaId !in identity.localMangaIds) {
			return
		}
		val bindings = workResolver.resolveBindingsByEntityId(entityId)
		activeEntityContextId = entityId
		activeEntityContextBindings = bindings
		activeEntityContextBoundLocalId = activeMangaId
		sessionReadingProjectionLocalMangaId.value = activeMangaId
		activeLocalSourceOptions.value = buildActiveLocalSourceOptions(bindings, activeMangaId)
		entityChapterSourceInfo.value = resolveEntityChapterSourceInfo(
			mangaId = activeMangaId,
			activeProjectionMangaId = activeMangaId,
			currentReadingProjectionMangaId = activeMangaId,
		)
		updateSourceOptions()
		refreshResolvedPresentationState()
	}

	private suspend fun resolveCurrentLocalMangaId(): Long? {
		currentWorkProjectionSnapshot().activeLocalMangaId?.let { return it }
		val currentContent = manga.filterNotNull().firstOrNull()
		if (currentContent?.isLocal == true) {
			return currentContent.id
		}
		return null
	}

	private fun preferredFallbackTrackingMangaIds(): List<Long> {
		val projectionSnapshot = currentWorkProjectionSnapshot()
		return buildList {
			projectionSnapshot.currentReadingProjectionMangaId?.let(::add)
			projectionSnapshot.activeLocalMangaId?.let(::add)
			baseLoadedDetails?.local?.manga?.id?.let(::add)
		}.distinct()
	}

	private suspend fun resolveCurrentLocalContent(): Content? {
		val localMangaId = resolveCurrentLocalMangaId() ?: return null
		return dataRepository.findPreferredLocalContentById(localMangaId, withChapters = false)
			?: dataRepository.findContentById(localMangaId, withChapters = false)
			?: manga.filterNotNull().firstOrNull { it.id == localMangaId }
	}

	fun reload() {
		loadingJob.cancel()
		loadingJob = doLoad(force = true)
	}

	fun refreshSourceBindings() {
		sourceBindingsRefreshJob?.cancel()
		sourceBindingsRefreshJob = launchJob(Dispatchers.IO) {
			refreshActiveEntitySourceOptions()
			updateSourceOptions()
			refreshResolvedPresentationState()
		}
	}

	fun updateUnifiedReadingStatus(status: ScrobblingStatus) {
		launchJob(Dispatchers.Default) {
			val currentMangaId = resolveCurrentLocalMangaId() ?: return@launchJob
			dataRepository.setReadingStatus(currentMangaId, status)
		}
	}

	fun updateUnifiedRating(rating: Float) {
	}

	fun removeFromHistory() {
		launchJob(Dispatchers.Default) {
			val currentMangaId = resolveCurrentLocalMangaId() ?: return@launchJob
			val handle = historyRepository.delete(setOf(currentMangaId))
			onActionDone.call(ReversibleAction(R.string.removed_from_history, handle))
		}
	}

	// --- Favorite Category Management (for Compose dialog) ---

	val allCategories: StateFlow<List<FavouriteCategory>> = favouritesRepository.observeCategories()
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, emptyList())

	val duplicateFavoritePrompt = MutableStateFlow<FavoriteDuplicatePrompt?>(null)

	fun setFavouriteCategory(categoryId: Long, isChecked: Boolean) {
		launchJob(Dispatchers.Default) {
			val content = getContentOrNull() ?: return@launchJob
			if (isChecked) {
				val candidates = duplicateCandidateRepository.findCandidates(content)
				if (candidates.isNotEmpty()) {
					duplicateFavoritePrompt.value = FavoriteDuplicatePrompt(
						categoryId = categoryId,
						contentTitle = content.title,
						candidates = candidates.take(MAX_DUPLICATE_PROMPT_CANDIDATES),
					)
					return@launchJob
				}
				favouritesRepository.addToCategory(categoryId, listOf(content))
			} else {
				favouritesRepository.removeFromCategory(categoryId, listOf(content.id))
			}
		}
	}

	fun confirmDuplicateFavourite() {
		val prompt = duplicateFavoritePrompt.value ?: return
		duplicateFavoritePrompt.value = null
		launchJob(Dispatchers.Default) {
			val content = getContentOrNull() ?: return@launchJob
			favouritesRepository.addToCategoryAsSeparateWorks(prompt.categoryId, listOf(content))
		}
	}

	fun dismissDuplicateFavourite() {
		duplicateFavoritePrompt.value = null
	}

	fun mergeBackDuplicateFavourite() {
		val prompt = duplicateFavoritePrompt.value ?: return
		val targetEntityId = prompt.mergeBackTargetEntityId ?: return
		duplicateFavoritePrompt.value = null
		launchJob(Dispatchers.Default) {
			val content = getContentOrNull() ?: return@launchJob
			mergeBackAndAddFavouriteUseCase(
				categoryId = prompt.categoryId,
				content = content,
				targetEntityId = targetEntityId,
			)
		}
	}

	fun toggleMarkSafe() {
		launchJob(Dispatchers.Default) {
				val manga = baseLoadedDetails?.toContent()
					?: mangaDetails.value?.local?.manga
					?: mangaDetails.value?.toContent()
					?: return@launchJob
			val override = dataRepository.getOverride(manga.id) ?: com.mangaverse.app.core.ui.model.ContentOverride(null, null, null)
			
			val isCurrentlyNsfw = manga.isNsfw()
			val newRating = if (isCurrentlyNsfw) {
				com.mangaverse.app.parsers.model.ContentRating.SAFE
			} else {
				com.mangaverse.app.parsers.model.ContentRating.ADULT
			}
			
			dataRepository.setOverride(manga, override.copy(contentRating = newRating))
			doLoad(false)
		}
	}

	private fun doLoad(force: Boolean) = launchLoadingJob(Dispatchers.Default) {
		val resolvedIntent = currentLoadIntentOverride ?: intent
		val requestedMangaId = resolvedIntent.mangaId.takeIf { it != ContentIntent.ID_NONE }
		Log.i(
			DETAILS_TRACE_TAG,
			"load.start force=$force intentId=${resolvedIntent.mangaId} requestedMangaId=$requestedMangaId " +
				"activeMangaId=${activeMangaIdFlow.value} currentOverride=${currentLoadIntentOverride?.mangaId}",
		)
		if (resolvedIntent.mangaId == 0L && resolvedIntent.manga == null) {
			Log.w(DETAILS_TRACE_TAG, "load.skip reason=emptyIntent")
			return@launchLoadingJob
		}
		try {
			detailsLoadUseCase.invoke(resolvedIntent, force)
			.onEachWhile {
				if (it.allChapters.isNotEmpty()) {
					val manga = it.toContent()
					// find default branch
					val hist = historyRepository.getOne(manga)
					selectedBranch.value = it.allChapters.findChapterByHistory(hist)?.branch
						?: manga.getPreferredBranch(hist)
					true
				} else {
					false
				}
			}.collect { details ->
				if (requestedMangaId != null && activeMangaIdFlow.value != requestedMangaId) {
					Log.w(
						DETAILS_TRACE_TAG,
						"load.drop reason=staleRequest requestedMangaId=$requestedMangaId activeMangaId=${activeMangaIdFlow.value} details=${details.toContent().detailsTraceSummary()}",
					)
					return@collect
				}
					Log.i(
						DETAILS_TRACE_TAG,
						"load.emit details=${details.toContent().detailsTraceSummary()} isLoaded=${details.isLoaded} " +
							"selectedBranchBefore=${selectedBranch.value}",
				)
				Log.i(
					DETAILS_TRACE_TAG,
					"load.apply details=${details.toContent().detailsTraceSummary()} selectedBranchAfter=${selectedBranch.value}",
				)
				baseLoadedDetails = details
				activeProjectionStoredContentType = db.getMangaDao().find(details.id)?.manga?.contentType?.let(::parseStoredContentType)
				refreshActiveEntitySourceOptions()
				syncDisplayedState()
				val localEntityId = entityGraphRepository.findEntityByBinding("0", details.id.toString())?.id
					?: entityGraphRepository.findEntityByBinding("local_manga", details.id.toString())?.id
				if (localEntityId != null) {
					restoreEntityMetadataSourceSelection(entityId = localEntityId)
				}
				}
		} catch (error: Throwable) {
			if (error !is CancellationException) {
				Log.e(DETAILS_TRACE_TAG, "load.failed force=$force requestedMangaId=$requestedMangaId", error)
			}
			throw error
		}
	}

	/**
	 * Expand EPUB chapters in the details page (NEW ARCHITECTURE - SIMPLIFIED)
	 * 
	 * In the new architecture, EPUB chapters are already loaded by LocalEpubSource
	 * in the doLoad() method, so this method simply returns the chapters as-is.
	 * 
	 * This method is kept for backward compatibility with old EPUB data that
	 * still uses the file://path#chapter/N format. Once all data is migrated,
	 * this method can be removed entirely.
	 */
	override suspend fun expandEpubChaptersIfNeeded(chapters: List<com.mangaverse.app.details.ui.model.ChapterListItem>): List<com.mangaverse.app.details.ui.model.ChapterListItem> {
		android.util.Log.d("DetailsViewModel", "expandEpubChaptersIfNeeded: NEW ARCHITECTURE - returning chapters as-is (${chapters.size} chapters)")
		return chapters
	}

	override suspend fun onDownloadComplete(downloadedContent: LocalContent?) {
		super.onDownloadComplete(downloadedContent)
		downloadedContent ?: return
		baseLoadedDetails = interactor.updateLocal(baseLoadedDetails, downloadedContent)
	}

	fun translateTitleAndDescription(forceRefresh: Boolean = false) {
		viewModelScope.launch(Dispatchers.IO) {
			if (!forceRefresh && hasTranslationCache.value) {
				isShowingTranslation.value = true
				persistCurrentTranslationState()
				return@launch
			}
			val manga = getContentOrNull() ?: return@launch
			val title = manga.title
			val description = mangaDetails.value?.description?.toString() ?: ""
			if (title.isBlank()) return@launch

			isTranslating.value = true
			try {
				val targetLang = currentTargetLang()
				val sourceLang = currentSourceLang()

				// Use ML Kit for simple text translation (same as reader pipeline)
				val translatedTitleText = translateViaMlKit(title, sourceLang, targetLang)
				val nextTranslatedTitle = translatedTitleText.takeIf { it.isNotBlank() && it != title }

				var nextTranslatedDescription: String? = null
				if (description.isNotBlank()) {
					val translatedDescText = translateViaMlKit(description, sourceLang, targetLang)
					nextTranslatedDescription = translatedDescText.takeIf { it.isNotBlank() && it != description }
				}

				if (nextTranslatedTitle != null || nextTranslatedDescription != null) {
					cachedTranslatedTitle.value = nextTranslatedTitle
					cachedTranslatedDescription.value = nextTranslatedDescription
					isShowingTranslation.value = true
					translationCacheSourceLang = sourceLang
					translationCacheTargetLang = targetLang
					detailsTranslationCache.put(
						content = manga,
						sourceLang = sourceLang,
						targetLang = targetLang,
						entry = CachedTranslationEntry(
							originalTitle = title,
							translatedTitle = nextTranslatedTitle,
							originalDescription = description,
							translatedDescription = nextTranslatedDescription,
							isShowingTranslation = true,
						),
					)
				}
			} catch (e: Exception) {
				if (e is kotlinx.coroutines.CancellationException) throw e
				android.util.Log.e("DetailsViewModel", "Translation failed", e)
			} finally {
				isTranslating.value = false
			}
		}
	}

	fun toggleTranslationDisplay() {
		if (!hasTranslationCache.value) return
		isShowingTranslation.value = !isShowingTranslation.value
		persistCurrentTranslationState()
	}

	fun clearTranslationCache() {
		clearInMemoryTranslationState()
	}

	private suspend fun translateViaMlKit(text: String, sourceLang: String, targetLang: String): String {
		// 改为调后端 /api/translate（模型在服务器，客户端零内置）
		return try {
			val from = if (sourceLang.trim().lowercase() == "auto") "en" else sourceLang
			val payload = org.json.JSONObject()
				.put("text", text)
				.put("from_lang", from)
				.put("to_lang", targetLang)
				.toString()
			val request = okhttp3.Request.Builder()
				.url("${com.mangaverse.app.BuildConfig.MANGAVERSE_API_BASE_URL}/api/translate")
				.post(payload.toRequestBody("application/json".toMediaType()))
				.build()
			withTimeout(30_000) {
				appOkHttpClient.newCall(request).execute().use { resp ->
					if (resp.isSuccessful) {
						org.json.JSONObject(resp.body?.string().orEmpty()).optString("translated").ifBlank { text }
					} else {
						text
					}
				}
			}
		} catch (_: Exception) {
			text
		}
	}

	private suspend fun detectLanguageViaMlKit(text: String): String? {
		// 后端翻译会自动处理语言识别
		return null
	}

private fun resolveMlKitLang(lang: String): String? {
		// mlkit 已移除，直接返回语言标签（后端翻译会处理映射）
		return lang.trim().lowercase().replace("-", "_").ifBlank { null }
	}

	private fun restorePersistedTranslation(details: ContentDetails) {
		val content = details.toContent()
		val sourceLang = currentSourceLang()
		val targetLang = currentTargetLang()
		val originalTitle = content.title
		val originalDescription = details.description?.toString().orEmpty()
		val restored = detailsTranslationCache.get(
			content = content,
			sourceLang = sourceLang,
			targetLang = targetLang,
			originalTitle = originalTitle,
			originalDescription = originalDescription,
		)
		if (restored == null) {
			clearInMemoryTranslationState()
			return
		}
		cachedTranslatedTitle.value = restored.translatedTitle
		cachedTranslatedDescription.value = restored.translatedDescription
		isShowingTranslation.value = restored.isShowingTranslation
		translationCacheSourceLang = sourceLang
		translationCacheTargetLang = targetLang
	}

	private fun persistCurrentTranslationState() {
		val sourceLang = translationCacheSourceLang ?: return
		val targetLang = translationCacheTargetLang ?: return
		val details = mangaDetails.value ?: return
		val content = details.toContent()
		if (!hasTranslationCache.value) return
		detailsTranslationCache.put(
			content = content,
			sourceLang = sourceLang,
			targetLang = targetLang,
			entry = CachedTranslationEntry(
				originalTitle = content.title,
				translatedTitle = cachedTranslatedTitle.value,
				originalDescription = details.description?.toString().orEmpty(),
				translatedDescription = cachedTranslatedDescription.value,
				isShowingTranslation = isShowingTranslation.value,
			),
		)
	}

	private fun clearInMemoryTranslationState() {
		cachedTranslatedTitle.value = null
		cachedTranslatedDescription.value = null
		isShowingTranslation.value = false
		translationCacheSourceLang = null
		translationCacheTargetLang = null
	}

	private fun currentSourceLang(): String {
		return settings.readerTranslationSourceLanguage.ifBlank { "auto" }
	}

	private fun currentTargetLang(): String {
		return settings.readerTranslationTargetLanguage.ifBlank { "zh" }
	}
}
