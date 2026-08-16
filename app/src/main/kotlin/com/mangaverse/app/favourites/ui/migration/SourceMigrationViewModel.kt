package com.mangaverse.app.favourites.ui.migration

import android.app.Application
import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mangaverse.app.R
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.model.getStableIdentityKey
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.util.ext.getDisplayMessage
import com.mangaverse.app.core.jsonsource.SourceGroupManager
import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.explore.ui.model.BrowseGroupTab
import com.mangaverse.app.explore.ui.model.SourceTag
import com.mangaverse.app.favourites.data.FavouriteContent
import com.mangaverse.app.favourites.data.toContent
import com.mangaverse.app.favourites.domain.DEFAULT_FUZZY_MERGE_THRESHOLD
import com.mangaverse.app.favourites.domain.EntityOrganizeRepository
import com.mangaverse.app.favourites.domain.MigrationProgress
import com.mangaverse.app.favourites.domain.MergeCandidateOptions
import com.mangaverse.app.favourites.domain.MergeCandidateGroup
import com.mangaverse.app.favourites.domain.MergeEntitiesResult
import com.mangaverse.app.favourites.domain.MergeFavoriteEntitiesUseCase
import com.mangaverse.app.favourites.domain.OrganizableWork
import com.mangaverse.app.favourites.domain.PreviewReadingSourceMigrationUseCase
import com.mangaverse.app.favourites.domain.ReadingSourcePreview
import com.mangaverse.app.favourites.domain.ReadingSourcePreviewAction
import com.mangaverse.app.favourites.work.SourceMigrationWorker
import com.mangaverse.app.entitygraph.data.EntityGraphRepository
import com.mangaverse.app.entitygraph.domain.EntityBinding
import com.mangaverse.app.entitygraph.domain.EntityGraphRepairIssueKind
import com.mangaverse.app.entitygraph.domain.EntityGraphRepairReport
import com.mangaverse.app.mihon.MihonExtensionManager
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.work.domain.WorkResolver
import java.util.zip.ZipOutputStream
import javax.inject.Inject

private const val TAG = "SourceMigrationVM"

internal fun MergeCandidateGroup.isExecutableMergeCandidate(): Boolean {
    return mangaIds.size >= 2 && !isAlreadyMerged
}

enum class EntityOrganizeStage {
    MERGE,
    READING,
}

enum class EntityOrganizeFeedbackKind {
    PREVIEW,
    EXECUTE,
}

data class EntityOrganizeFeedback(
    val stage: EntityOrganizeStage,
    val kind: EntityOrganizeFeedbackKind,
    val message: String,
)

data class EntityOrganizeCloseResult(
    val shouldRefreshFavorites: Boolean,
    val message: String?,
)

data class EntityOrganizeStagePlan(
    val stage: EntityOrganizeStage,
    val enabled: Boolean,
    val canPreview: Boolean,
    val canExecute: Boolean,
    val previewCount: Int,
    val acceptedCount: Int,
    val feedback: EntityOrganizeFeedback?,
)

data class MigrationUiState(
    val favouriteSources: List<ContentSource> = emptyList(),
    val availableSources: List<ContentSource> = emptyList(),
    val selectedContentIds: Set<Long> = emptySet(),
    val organizableWorks: List<OrganizableWork> = emptyList(),
    val scopedFavouriteContents: List<FavouriteContent> = emptyList(),
    val mergeCandidateGroups: List<MergeCandidateGroup> = emptyList(),
    val mergePreviewReady: Boolean = false,
    val selectedMergeGroupIds: Set<String> = emptySet(),
    val selectedMergeItemsByGroup: Map<String, Set<Long>> = emptyMap(),
    val selectedManualMergeMangaIds: Set<Long> = emptySet(),
    val fuzzyMergeCandidatesEnabled: Boolean = false,
    val fuzzyMergeThresholdPercent: Int = (DEFAULT_FUZZY_MERGE_THRESHOLD * 100).toInt(),
    val readingSourcePreviews: List<ReadingSourcePreview> = emptyList(),
    val acceptedReadingPreviewIds: Set<Long> = emptySet(),
    val stageFeedbacks: Map<EntityOrganizeStage, EntityOrganizeFeedback> = emptyMap(),
    val selectedFromSource: ContentSource? = null,
    val selectedTargetSources: List<ContentSource> = emptyList(),
    val fromContentTypeFilter: Set<BrowseGroupTab> = emptySet(),
    val fromSourceTagFilter: Set<SourceTag> = emptySet(),
    val toContentTypeFilter: Set<BrowseGroupTab> = emptySet(),
    val toSourceTagFilter: Set<SourceTag> = emptySet(),
    val concurrency: Int = 3,
    val migrationProgress: MigrationProgress? = null,
    val isExecuting: Boolean = false,
    val workId: String? = null,
    val isFinished: Boolean = false,
    val fromFilteredSources: List<ContentSource> = emptyList(),
    val toFilteredSources: List<ContentSource> = emptyList(),
    val repairReport: EntityGraphRepairReport? = null,
    val isLoadingRepairReport: Boolean = true,
    val isEntityResetRunning: Boolean = false,
    val entityResetFeedback: String? = null,
    val isEntityResetConfirmationPending: Boolean = false,
) {
    val suspectMismergedLocalMangaIds: Set<Long>
        get() = repairReport
            ?.issues
            .orEmpty()
            .asSequence()
            .filter { it.kind == EntityGraphRepairIssueKind.SUSPECT_MISMERGED_LOCAL_WORK }
            .mapNotNull { it.externalId?.toLongOrNull() }
            .toSet()

    val manualMergeMangaIds: Set<Long>
        get() = selectedManualMergeMangaIds

    val hasManualSelection: Boolean
        get() = selectedContentIds.isNotEmpty()

    fun feedbackOf(stage: EntityOrganizeStage): EntityOrganizeFeedback? = stageFeedbacks[stage]

    fun stagePlan(stage: EntityOrganizeStage): EntityOrganizeStagePlan {
        return when (stage) {
            EntityOrganizeStage.MERGE -> EntityOrganizeStagePlan(
                stage = stage,
                enabled = true,
                canPreview = !isExecuting,
                canExecute = mergePreviewReady &&
                    mergeCandidateGroups.any {
                        it.id in selectedMergeGroupIds && it.isExecutableMergeCandidate()
                    } &&
                    !isExecuting,
                previewCount = mergeCandidateGroups.count { it.isExecutableMergeCandidate() },
                acceptedCount = mergeCandidateGroups.count {
                    it.id in selectedMergeGroupIds && it.isExecutableMergeCandidate()
                },
                feedback = feedbackOf(stage),
            )

            EntityOrganizeStage.READING -> {
                val hasScope = hasManualSelection || selectedFromSource != null
                EntityOrganizeStagePlan(
                    stage = stage,
                    enabled = true,
                    canPreview =
                        hasScope &&
                        selectedTargetSources.isNotEmpty() &&
                        !isExecuting,
                    canExecute =
                        hasScope &&
                        selectedTargetSources.isNotEmpty() &&
                        acceptedReadingPreviewIds.isNotEmpty() &&
                        !isExecuting,
                    previewCount = readingSourcePreviews.size,
                    acceptedCount = acceptedReadingPreviewIds.size,
                    feedback = feedbackOf(stage),
                )
            }
        }
    }
}

internal fun buildEntityOrganizeCloseResult(
    uiState: MigrationUiState,
    context: Context,
): EntityOrganizeCloseResult {
    val executeFeedbacks = EntityOrganizeStage.entries.mapNotNull { stage ->
        uiState.feedbackOf(stage)?.takeIf { it.kind == EntityOrganizeFeedbackKind.EXECUTE }
    }
    if (executeFeedbacks.isEmpty()) {
        return EntityOrganizeCloseResult(
            shouldRefreshFavorites = false,
            message = null,
        )
    }
    val primaryMessage = executeFeedbacks.lastOrNull()?.message
    val summaryMessage = if (executeFeedbacks.size <= 1) {
        primaryMessage
    } else {
        context.getString(
            R.string.entity_organize_close_summary,
            executeFeedbacks.size,
            primaryMessage,
        )
    }
    return EntityOrganizeCloseResult(
        shouldRefreshFavorites = true,
        message = summaryMessage,
    )
}

@HiltViewModel
class SourceMigrationViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appSettings: AppSettings,
    private val entityOrganizeRepository: EntityOrganizeRepository,
    private val sourcesRepository: ContentSourcesRepository,
    private val sourceGroupManager: SourceGroupManager,
    private val mergeFavoriteEntitiesUseCase: MergeFavoriteEntitiesUseCase,
    private val previewReadingSourceMigrationUseCase: PreviewReadingSourceMigrationUseCase,
    private val entityGraphRepository: EntityGraphRepository,
    private val backupRepository: BackupRepository,
    private val workResolver: WorkResolver,
    private val database: MangaDatabase,
    private val contentDataRepository: ContentDataRepository,
    private val mihonExtensionManager: MihonExtensionManager,
) : AndroidViewModel(appContext as Application) {

    companion object {
        private const val LOW_CONFIDENCE_THRESHOLD = 0.85f
    }

    private val _uiState = MutableStateFlow(
        MigrationUiState(
            isEntityResetConfirmationPending = appSettings.isWorkMigrationSyncWriteBlocked,
        ),
    )
    val uiState: StateFlow<MigrationUiState> = _uiState.asStateFlow()

    private var migrationObserver: Observer<WorkInfo?>? = null

    init {
        loadSources()
        refreshMergeCandidates()
        refreshRepairReport()
    }

    override fun onCleared() {
        super.onCleared()
        removeMigrationObserver()
    }

    fun loadSources() {
        viewModelScope.launch(Dispatchers.IO) {
            loadSourcesNow()
        }
    }

    fun refreshRepairReport() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshRepairReportNow()
        }
    }

    fun resetEntityIdentities() {
        val state = _uiState.value
        if (state.isExecuting || state.isEntityResetRunning) {
            return
        }
        _uiState.value = state.copy(
            isEntityResetRunning = true,
            isFinished = false,
            entityResetFeedback = null,
            isEntityResetConfirmationPending = false,
        )
        viewModelScope.launch(Dispatchers.IO) {
            var resetSucceeded = false
            val feedback = runCatching {
                val backupFile = BackupUtils.createTempFile(appContext)
                ZipOutputStream(backupFile.outputStream()).use { output ->
                    backupRepository.createBackup(output, null)
                }
                val result = entityGraphRepository.resetAllEntities()
                resetSucceeded = true
                appContext.getString(
                    R.string.entity_organize_reset_feedback,
                    result.rebuiltEntityCount,
                    result.duplicateProjectionGroupCount,
                    result.favouriteActiveRowsBefore,
                    result.favouriteActiveRowsAfter,
                    result.favouriteActiveWorksBefore,
                    result.favouriteActiveWorksAfter,
                    result.restoredHistoryCount,
                    result.restoredStatsCount,
                    backupFile.absolutePath,
                )
            }.getOrElse { error ->
                Log.e(TAG, "Entity identity reset failed", error)
                appContext.getString(
                    R.string.entity_organize_reset_failed,
                    error.getDisplayMessage(appContext.resources),
                )
            }
            runCatching {
                loadSourcesNow()
                refreshRepairReportNow()
            }.onFailure { error ->
                Log.e(TAG, "Entity identity reset refresh failed", error)
            }
            val current = _uiState.value
            _uiState.value = current.copy(
                isEntityResetRunning = false,
                isFinished = true,
                entityResetFeedback = feedback,
                isEntityResetConfirmationPending = resetSucceeded,
            )
        }
    }

    private suspend fun loadSourcesNow() {
        val favouriteContents = entityOrganizeRepository.listFavouriteContents()
        val organizableWorks = entityOrganizeRepository.listOrganizableWorks()
        val sourceCounts = favouriteContents
            .groupingBy { it.manga.source }
            .eachCount()
        val allSources = sourcesRepository.getAllAvailableSourcesForListing()
        val sortedFavSources = allSources
            .filter { it.name in sourceCounts }
            .sortedByDescending { sourceCounts[it.name] ?: 0 }
        val state = _uiState.value
        _uiState.value = state.copy(
            favouriteSources = sortedFavSources,
            availableSources = allSources,
            organizableWorks = organizableWorks,
            fromFilteredSources = filterSources(sortedFavSources, state.fromContentTypeFilter, state.fromSourceTagFilter),
            toFilteredSources = filterSources(allSources, state.toContentTypeFilter, state.toSourceTagFilter),
            selectedTargetSources = state.selectedTargetSources.filter { selected ->
                allSources.any { it.name == selected.name }
            },
        )
        refreshMergeCandidatesNow()
    }

    private suspend fun refreshRepairReportNow() {
        _uiState.value = _uiState.value.copy(isLoadingRepairReport = true)
        val report = runCatching {
            entityGraphRepository.inspectRepairIssues()
        }.getOrNull()
        _uiState.value = _uiState.value.copy(
            repairReport = report,
            isLoadingRepairReport = false,
        )
    }

    fun confirmEntityResetResult() {
        val state = _uiState.value
        if (state.isEntityResetRunning || !state.isEntityResetConfirmationPending) {
            return
        }
        appSettings.isWorkMigrationSyncWriteBlocked = false
        appSettings.requiresWorkMigrationNormalization = false
        _uiState.value = state.copy(
            isEntityResetConfirmationPending = false,
            entityResetFeedback = appContext.getString(R.string.entity_organize_reset_confirmed_feedback),
        )
    }

    fun setSelectedContentIds(ids: Set<Long>) {
        val state = _uiState.value
        if (state.selectedContentIds == ids) {
            return
        }
        _uiState.value = state.copy(
            selectedContentIds = ids,
            selectedManualMergeMangaIds = ids,
            selectedFromSource = if (ids.isNotEmpty()) null else state.selectedFromSource,
            mergePreviewReady = false,
            selectedMergeGroupIds = emptySet(),
            readingSourcePreviews = emptyList(),
            acceptedReadingPreviewIds = emptySet(),
            stageFeedbacks = state.stageFeedbacks
                .without(EntityOrganizeStage.MERGE)
                .without(EntityOrganizeStage.READING),
        )
        refreshMergeCandidates()
    }

    fun selectFromSource(source: ContentSource?) {
        val scopedIds = if (source == null) {
            emptySet()
        } else {
            _uiState.value.mergeCandidateGroups
                .asSequence()
                .filter { group -> group.items.firstOrNull()?.sourceName == source.name }
                .mapNotNull { group -> group.items.firstOrNull()?.mangaId }
                .toSet()
        }
        _uiState.value = _uiState.value.copy(
            selectedFromSource = source,
            selectedContentIds = scopedIds,
            mergePreviewReady = false,
            selectedMergeGroupIds = emptySet(),
            readingSourcePreviews = emptyList(),
            acceptedReadingPreviewIds = emptySet(),
            stageFeedbacks = _uiState.value.stageFeedbacks
                .without(EntityOrganizeStage.MERGE)
                .without(EntityOrganizeStage.READING),
        )
        refreshMergeCandidates()
    }

    fun setFuzzyMergeCandidatesEnabled(enabled: Boolean) {
        val state = _uiState.value
        if (state.fuzzyMergeCandidatesEnabled == enabled) {
            return
        }
        _uiState.value = state.copy(
            fuzzyMergeCandidatesEnabled = enabled,
            mergeCandidateGroups = emptyList(),
            mergePreviewReady = false,
            selectedMergeGroupIds = emptySet(),
            selectedMergeItemsByGroup = emptyMap(),
            selectedManualMergeMangaIds = emptySet(),
            stageFeedbacks = state.stageFeedbacks
                .without(EntityOrganizeStage.MERGE),
        )
        refreshMergeCandidates()
    }

    fun setFuzzyMergeThresholdPercent(percent: Int) {
        val bounded = percent.coerceIn(80, 100)
        val state = _uiState.value
        if (state.fuzzyMergeThresholdPercent == bounded) {
            return
        }
        _uiState.value = state.copy(
            fuzzyMergeThresholdPercent = bounded,
            mergeCandidateGroups = emptyList(),
            mergePreviewReady = false,
            selectedMergeGroupIds = emptySet(),
            selectedMergeItemsByGroup = emptyMap(),
            selectedManualMergeMangaIds = emptySet(),
            stageFeedbacks = state.stageFeedbacks
                .without(EntityOrganizeStage.MERGE),
        )
        refreshMergeCandidates()
    }

    fun toggleMergeGroup(groupId: String) {
        val state = _uiState.value
        val next = if (groupId in state.selectedMergeGroupIds) {
            state.selectedMergeGroupIds - groupId
        } else {
            state.selectedMergeGroupIds + groupId
        }
        _uiState.value = state.copy(selectedMergeGroupIds = next)
    }

    fun setMergeGroupsSelected(groupIds: Set<String>, selected: Boolean) {
        if (groupIds.isEmpty()) return
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedMergeGroupIds = updateMergeGroupSelection(
                current = state.selectedMergeGroupIds,
                groupIds = groupIds,
                selected = selected,
            ),
        )
    }

    fun toggleMergeItem(groupId: String, mangaId: Long) {
        val state = _uiState.value
        val current = state.selectedMergeItemsByGroup[groupId].orEmpty()
        val itemSelected = mangaId !in current
        val next = if (itemSelected) current + mangaId else current - mangaId
        _uiState.value = state.copy(
            selectedMergeItemsByGroup = state.selectedMergeItemsByGroup + (groupId to next),
            selectedManualMergeMangaIds = if (itemSelected) {
                state.selectedManualMergeMangaIds + mangaId
            } else {
                state.selectedManualMergeMangaIds - mangaId
            },
        )
    }

    fun clearManualMergeSelections() {
        val state = _uiState.value
        if (state.selectedManualMergeMangaIds.isEmpty() && state.selectedMergeItemsByGroup.values.all { it.isEmpty() }) {
            return
        }
        _uiState.value = state.copy(
            selectedMergeItemsByGroup = state.selectedMergeItemsByGroup.mapValues { emptySet() },
            selectedManualMergeMangaIds = emptySet(),
        )
    }

    fun splitLocalWorkProjection(mangaId: Long) {
        repairLocalWorkProjection(
            mangaId = mangaId,
            action = {
                entityGraphRepository.splitLocalWorkProjection(mangaId)
            },
            successMessage = R.string.entity_organize_repair_split_feedback,
        )
    }

    fun detachLocalWorkProjection(mangaId: Long) {
        val state = _uiState.value
        if (state.mergeCandidateGroups.any { it.isExecutableMergeCandidate() && mangaId in it.mangaIds }) {
            splitLocalWorkProjection(mangaId)
            return
        }
        repairLocalWorkProjection(
            mangaId = mangaId,
            action = {
                if (entityGraphRepository.detachLocalWorkProjection(mangaId)) mangaId else null
            },
            successMessage = R.string.entity_organize_repair_detach_feedback,
        )
    }

    fun repairDanglingWorkProjectionAnchors() {
        val state = _uiState.value
        if (state.isExecuting) {
            return
        }
        _uiState.value = state.copy(
            isExecuting = true,
            isFinished = false,
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.MERGE),
        )
        viewModelScope.launch(Dispatchers.IO) {
            val repaired = entityGraphRepository.repairDanglingWorkProjectionAnchors()
            refreshRepairReportNow()
            val current = _uiState.value
            _uiState.value = current.copy(
                isExecuting = false,
                isFinished = true,
                stageFeedbacks = current.stageFeedbacks.withFeedback(
                    stage = EntityOrganizeStage.MERGE,
                    kind = EntityOrganizeFeedbackKind.EXECUTE,
                    message = appContext.getString(
                        R.string.entity_organize_repair_dangling_work_anchors_feedback,
                        repaired,
                    ),
                ),
            )
        }
    }

    fun repairWorkEntitiesMissingSyncId() {
        val state = _uiState.value
        if (state.isExecuting) {
            return
        }
        _uiState.value = state.copy(
            isExecuting = true,
            isFinished = false,
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.MERGE),
        )
        viewModelScope.launch(Dispatchers.IO) {
            val repaired = entityGraphRepository.repairWorkEntitiesMissingSyncId()
            refreshRepairReportNow()
            val current = _uiState.value
            _uiState.value = current.copy(
                isExecuting = false,
                isFinished = true,
                stageFeedbacks = current.stageFeedbacks.withFeedback(
                    stage = EntityOrganizeStage.MERGE,
                    kind = EntityOrganizeFeedbackKind.EXECUTE,
                    message = appContext.getString(
                        R.string.entity_organize_repair_work_sync_ids_feedback,
                        repaired,
                    ),
                ),
            )
        }
    }

    fun repairMixedWorkContentTypeEntities() {
        val state = _uiState.value
        if (state.isExecuting) {
            return
        }
        _uiState.value = state.copy(
            isExecuting = true,
            isFinished = false,
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.MERGE),
        )
        viewModelScope.launch(Dispatchers.IO) {
            val repaired = entityGraphRepository.repairMixedWorkContentTypeEntities()
            loadSourcesNow()
            refreshRepairReportNow()
            val current = _uiState.value
            _uiState.value = current.copy(
                isExecuting = false,
                isFinished = true,
                stageFeedbacks = current.stageFeedbacks.withFeedback(
                    stage = EntityOrganizeStage.MERGE,
                    kind = EntityOrganizeFeedbackKind.EXECUTE,
                    message = appContext.getString(
                        R.string.entity_organize_repair_mixed_work_content_types_feedback,
                        repaired,
                    ),
                ),
            )
        }
    }

    fun mergeSelectedEntities() {
        val state = _uiState.value
        if (!state.mergePreviewReady || state.selectedMergeGroupIds.isEmpty() || state.isExecuting) {
            return
        }
        _uiState.value = state.copy(
            isExecuting = true,
            isFinished = false,
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.MERGE),
        )
        viewModelScope.launch(Dispatchers.IO) {
            val selectedGroups = buildSelectedMergeGroupsForExecution(
                groups = state.mergeCandidateGroups,
                selectedGroupIds = state.selectedMergeGroupIds,
                selectedItemsByGroup = state.selectedMergeItemsByGroup,
            )
            val result = mergeFavoriteEntitiesUseCase.merge(selectedGroups)
            refreshMergeCandidates()
            _uiState.value = _uiState.value.copy(
                isExecuting = false,
                isFinished = true,
                mergePreviewReady = false,
                stageFeedbacks = _uiState.value.stageFeedbacks.withFeedback(
                    stage = EntityOrganizeStage.MERGE,
                    kind = EntityOrganizeFeedbackKind.EXECUTE,
                    message = appContext.getString(
                        R.string.entity_organize_merge_execute_feedback,
                        result.succeeded,
                        result.failed,
                        result.skipped,
                    ),
                ),
            )
        }
    }

    fun manualMergeSelectedWorks() {
        val state = _uiState.value
        val selectedMangaIds = state.selectedManualMergeMangaIds
        if (selectedMangaIds.size < 2 || state.isExecuting) {
            return
        }
        _uiState.value = state.copy(
            isExecuting = true,
            isFinished = false,
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.MERGE),
        )
        viewModelScope.launch(Dispatchers.IO) {
            val contents = entityOrganizeRepository.listFavouriteContentsByMangaIds(selectedMangaIds)
                .map { it.toContent() }
                .distinctBy { it.id }
            val sameContentType = contents
                .mapTo(LinkedHashSet()) { it.source.contentType }
                .size == 1
            val result = if (contents.size >= 2 && sameContentType) {
                mergeFavoriteEntitiesUseCase.mergeManual(contents)
            } else {
                MergeEntitiesResult(succeeded = 0, failed = 0, skipped = 1)
            }
            refreshMergeCandidatesNow()
            refreshRepairReportNow()
            val current = _uiState.value
            _uiState.value = current.copy(
                isExecuting = false,
                isFinished = true,
                stageFeedbacks = current.stageFeedbacks.withFeedback(
                    stage = EntityOrganizeStage.MERGE,
                    kind = EntityOrganizeFeedbackKind.EXECUTE,
                    message = if (sameContentType) {
                        appContext.getString(
                            R.string.entity_organize_manual_merge_feedback,
                            result.succeeded,
                            result.failed,
                            result.skipped,
                        )
                    } else {
                        appContext.getString(R.string.entity_organize_manual_merge_type_mismatch)
                    },
                ),
            )
        }
    }

    private fun repairLocalWorkProjection(
        mangaId: Long,
        action: suspend (Long) -> Long?,
        successMessage: Int,
    ) {
        val state = _uiState.value
        if (mangaId == 0L || state.isExecuting) {
            return
        }
        _uiState.value = state.copy(
            isExecuting = true,
            isFinished = false,
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.MERGE),
        )
        viewModelScope.launch(Dispatchers.IO) {
            val repairedId = action(mangaId)
            loadSourcesNow()
            refreshRepairReportNow()
            val current = _uiState.value
            _uiState.value = current.copy(
                isExecuting = false,
                isFinished = true,
                selectedMergeItemsByGroup = current.selectedMergeItemsByGroup.mapValues { (_, ids) ->
                    ids - mangaId
                },
                selectedManualMergeMangaIds = current.selectedManualMergeMangaIds - mangaId,
                stageFeedbacks = current.stageFeedbacks.withFeedback(
                    stage = EntityOrganizeStage.MERGE,
                    kind = EntityOrganizeFeedbackKind.EXECUTE,
                    message = if (repairedId != null) {
                        appContext.getString(successMessage)
                    } else {
                        appContext.getString(R.string.entity_organize_repair_action_failed)
                    },
                ),
            )
        }
    }

    fun previewMergeCandidates() {
        val state = _uiState.value
        if (state.isExecuting) {
            return
        }
        _uiState.value = state.copy(
            isExecuting = true,
            isFinished = false,
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.MERGE),
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favourites = loadScopedFavouriteContents(state)
                val contents = favourites.map { it.toContent() }
                val groups = augmentGroupsWithOrganizableWorkProjections(
                    groups = buildWorkbenchGroups(contents, state),
                    organizableWorks = _uiState.value.organizableWorks,
                )
                val availableGroupIds = groups.mapTo(HashSet(groups.size)) { it.id }
                val defaultSelectedGroupIds = resolveDefaultSelectedMergeGroupIds(
                    groups = groups,
                    selectedContentIds = state.selectedContentIds,
                )
                val selectedItemsByGroup = buildMap {
                    groups.forEach { group ->
                        put(
                            group.id,
                            state.selectedMergeItemsByGroup[group.id]
                                ?.intersect(group.mangaIds)
                                ?.takeIf { it.isNotEmpty() }
                                ?: group.mangaIds
                                    .intersect(state.selectedManualMergeMangaIds)
                                    .takeIf { it.isNotEmpty() }
                                ?: emptySet(),
                        )
                    }
                }
                val visibleManualMergeIds = selectedItemsByGroup.values.flatten().toSet()
                val mergeableCount = groups.count { it.isExecutableMergeCandidate() }
                val mergeableSelectedCount = groups.count {
                    it.id in defaultSelectedGroupIds && it.isExecutableMergeCandidate()
                }
                _uiState.value = _uiState.value.copy(
                    isExecuting = false,
                    isFinished = true,
                    scopedFavouriteContents = favourites,
                    mergeCandidateGroups = groups,
                    mergePreviewReady = true,
                    selectedMergeItemsByGroup = selectedItemsByGroup,
                    selectedManualMergeMangaIds = _uiState.value.selectedManualMergeMangaIds
                        .intersect(visibleManualMergeIds),
                    selectedMergeGroupIds = defaultSelectedGroupIds,
                    stageFeedbacks = _uiState.value.stageFeedbacks.withFeedback(
                        stage = EntityOrganizeStage.MERGE,
                        kind = EntityOrganizeFeedbackKind.PREVIEW,
                        message = appContext.getString(
                            R.string.entity_organize_merge_preview_feedback,
                            mergeableCount,
                            mergeableSelectedCount,
                        ),
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExecuting = false,
                    isFinished = true,
                    mergePreviewReady = true,
                    stageFeedbacks = _uiState.value.stageFeedbacks.withFeedback(
                        stage = EntityOrganizeStage.MERGE,
                        kind = EntityOrganizeFeedbackKind.PREVIEW,
                        message = e.getDisplayMessage(getApplication<Application>().resources),
                    ),
                )
            }
        }
    }

    fun toggleTargetSource(source: ContentSource) {
        val state = _uiState.value
        val sourceKey = source.getStableIdentityKey()
        val existingIndex = state.selectedTargetSources.indexOfFirst { it.getStableIdentityKey() == sourceKey }
        val updated = if (existingIndex >= 0) {
            state.selectedTargetSources.toMutableList().apply { removeAt(existingIndex) }
        } else {
            (state.selectedTargetSources + source)
        }
        _uiState.value = state.copy(
            selectedTargetSources = updated,
            readingSourcePreviews = emptyList(),
            acceptedReadingPreviewIds = emptySet(),
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.READING),
        )
    }

    fun moveTargetSourceUp(sourceKey: String) {
        val state = _uiState.value
        val index = state.selectedTargetSources.indexOfFirst { it.getStableIdentityKey() == sourceKey }
        if (index <= 0) return
        val updated = state.selectedTargetSources.toMutableList()
        val item = updated.removeAt(index)
        updated.add(index - 1, item)
        _uiState.value = state.copy(
            selectedTargetSources = updated,
            readingSourcePreviews = emptyList(),
            acceptedReadingPreviewIds = emptySet(),
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.READING),
        )
    }

    fun moveTargetSourceDown(sourceKey: String) {
        val state = _uiState.value
        val index = state.selectedTargetSources.indexOfFirst { it.getStableIdentityKey() == sourceKey }
        if (index < 0 || index >= state.selectedTargetSources.lastIndex) return
        val updated = state.selectedTargetSources.toMutableList()
        val item = updated.removeAt(index)
        updated.add(index + 1, item)
        _uiState.value = state.copy(
            selectedTargetSources = updated,
            readingSourcePreviews = emptyList(),
            acceptedReadingPreviewIds = emptySet(),
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.READING),
        )
    }

    fun removeTargetSource(sourceKey: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedTargetSources = state.selectedTargetSources.filterNot { it.getStableIdentityKey() == sourceKey },
            readingSourcePreviews = emptyList(),
            acceptedReadingPreviewIds = emptySet(),
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.READING),
        )
    }

    fun setConcurrency(value: Int) {
        _uiState.value = _uiState.value.copy(concurrency = value.coerceIn(1, 10))
    }

    fun toggleFromContentType(tab: BrowseGroupTab) {
        val state = _uiState.value
        val tabs = if (tab in state.fromContentTypeFilter) {
            if (state.fromContentTypeFilter.size == 1) state.fromContentTypeFilter else state.fromContentTypeFilter - tab
        } else {
            state.fromContentTypeFilter + tab
        }
        _uiState.value = state.copy(
            fromContentTypeFilter = tabs,
            fromFilteredSources = filterSources(state.favouriteSources, tabs, state.fromSourceTagFilter),
        )
    }

    fun toggleFromSourceTag(tag: SourceTag) {
        val state = _uiState.value
        val tags = if (tag in state.fromSourceTagFilter) {
            state.fromSourceTagFilter - tag
        } else {
            state.fromSourceTagFilter + tag
        }
        _uiState.value = state.copy(
            fromSourceTagFilter = tags,
            fromFilteredSources = filterSources(state.favouriteSources, state.fromContentTypeFilter, tags),
        )
    }

    fun toggleToContentType(tab: BrowseGroupTab) {
        val state = _uiState.value
        val tabs = if (tab in state.toContentTypeFilter) {
            if (state.toContentTypeFilter.size == 1) state.toContentTypeFilter else state.toContentTypeFilter - tab
        } else {
            state.toContentTypeFilter + tab
        }
        _uiState.value = state.copy(
            toContentTypeFilter = tabs,
            toFilteredSources = filterSources(state.availableSources, tabs, state.toSourceTagFilter),
            readingSourcePreviews = emptyList(),
            acceptedReadingPreviewIds = emptySet(),
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.READING),
        )
    }

    fun toggleToSourceTag(tag: SourceTag) {
        val state = _uiState.value
        val tags = if (tag in state.toSourceTagFilter) {
            state.toSourceTagFilter - tag
        } else {
            state.toSourceTagFilter + tag
        }
        _uiState.value = state.copy(
            toSourceTagFilter = tags,
            toFilteredSources = filterSources(state.availableSources, state.toContentTypeFilter, tags),
            readingSourcePreviews = emptyList(),
            acceptedReadingPreviewIds = emptySet(),
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.READING),
        )
    }

    fun previewReadingSources() {
        val state = _uiState.value
        val targetSources = state.selectedTargetSources
        val hasScope = state.selectedContentIds.isNotEmpty() || state.selectedFromSource != null
        if (!hasScope || targetSources.isEmpty() || state.isExecuting) {
            return
        }
        _uiState.value = state.copy(
            isExecuting = true,
            isFinished = false,
            migrationProgress = MigrationProgress(
                total = state.selectedContentIds.size.takeIf { it > 0 } ?: loadPreviewScopeEstimate(state),
                completed = 0,
                failed = 0,
                notFound = 0,
                currentItem = null,
                items = emptyList(),
            ),
            readingSourcePreviews = emptyList(),
            acceptedReadingPreviewIds = emptySet(),
            stageFeedbacks = state.stageFeedbacks.without(EntityOrganizeStage.READING),
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val favourites = loadScopedFavouriteContents(state)
                _uiState.value = _uiState.value.copy(
                    migrationProgress = MigrationProgress(
                        total = favourites.size,
                        completed = 0,
                        failed = 0,
                        notFound = 0,
                        currentItem = null,
                        items = emptyList(),
                    ),
                )
                val result = previewReadingSourceMigrationUseCase.preview(
                    favourites = favourites,
                    targetSources = targetSources,
                    onProgress = { progress ->
                        _uiState.value = _uiState.value.copy(migrationProgress = progress)
                    },
                )
                _uiState.value = _uiState.value.copy(
                    isExecuting = false,
                    isFinished = true,
                    migrationProgress = _uiState.value.migrationProgress?.copy(isFinished = true),
                    readingSourcePreviews = result.previews.map(::withResolvedDisplaySourceName),
                    acceptedReadingPreviewIds = result.previews.mapTo(LinkedHashSet(result.previews.size)) { it.mangaId },
                    stageFeedbacks = _uiState.value.stageFeedbacks.withFeedback(
                        stage = EntityOrganizeStage.READING,
                        kind = EntityOrganizeFeedbackKind.PREVIEW,
                        message = appContext.getString(
                            R.string.entity_organize_reading_preview_feedback,
                            result.previews.size,
                            result.skipped,
                        ),
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExecuting = false,
                    isFinished = true,
                    migrationProgress = _uiState.value.migrationProgress?.copy(isFinished = true),
                    stageFeedbacks = _uiState.value.stageFeedbacks.withFeedback(
                        stage = EntityOrganizeStage.READING,
                        kind = EntityOrganizeFeedbackKind.PREVIEW,
                        message = e.getDisplayMessage(getApplication<Application>().resources),
                    ),
                )
            }
        }
    }

    fun toggleReadingPreview(mangaId: Long) {
        val state = _uiState.value
        val next = if (mangaId in state.acceptedReadingPreviewIds) {
            state.acceptedReadingPreviewIds - mangaId
        } else {
            state.acceptedReadingPreviewIds + mangaId
        }
        _uiState.value = state.copy(acceptedReadingPreviewIds = next)
    }

    fun toggleReadingScopeGroup(groupId: String) {
        val state = _uiState.value
        val scopeMangaIds = state.mergeCandidateGroups
            .firstOrNull { it.id == groupId }
            ?.mangaIds
            ?.takeIf { it.isNotEmpty() }
            ?: return
        val next = if (scopeMangaIds.all(state.selectedContentIds::contains)) {
            clearSelectionIds(state.selectedContentIds, scopeMangaIds)
        } else {
            state.selectedContentIds + scopeMangaIds
        }
        _uiState.value = state.copy(
            selectedContentIds = next,
            selectedFromSource = null,
            mergePreviewReady = false,
            selectedMergeGroupIds = emptySet(),
            readingSourcePreviews = emptyList(),
            acceptedReadingPreviewIds = emptySet(),
            stageFeedbacks = state.stageFeedbacks
                .without(EntityOrganizeStage.MERGE)
                .without(EntityOrganizeStage.READING),
        )
        refreshMergeCandidates()
    }

    fun setReadingScopeGroupsSelected(groupIds: Set<String>, selected: Boolean) {
        if (groupIds.isEmpty()) return
        val state = _uiState.value
        val scopeMangaIds = state.mergeCandidateGroups
            .asSequence()
            .filter { it.id in groupIds }
            .flatMap { it.mangaIds.asSequence() }
            .toSet()
        if (scopeMangaIds.isEmpty()) return
        _uiState.value = state.copy(
            selectedContentIds = if (selected) {
                state.selectedContentIds + scopeMangaIds
            } else {
                clearSelectionIds(state.selectedContentIds, scopeMangaIds)
            },
            selectedFromSource = null,
            mergePreviewReady = false,
            selectedMergeGroupIds = emptySet(),
            readingSourcePreviews = emptyList(),
            acceptedReadingPreviewIds = emptySet(),
            stageFeedbacks = state.stageFeedbacks
                .without(EntityOrganizeStage.MERGE)
                .without(EntityOrganizeStage.READING),
        )
        refreshMergeCandidates()
    }

    fun acceptReadingPreviews(mangaIds: Set<Long>) {
        val state = _uiState.value
        if (mangaIds.isEmpty()) return
        _uiState.value = state.copy(
            acceptedReadingPreviewIds = state.acceptedReadingPreviewIds + mangaIds,
        )
    }

    fun clearReadingPreviews(mangaIds: Set<Long>) {
        if (mangaIds.isEmpty()) return
        val state = _uiState.value
        _uiState.value = state.copy(
            acceptedReadingPreviewIds = clearSelectionIds(state.acceptedReadingPreviewIds, mangaIds),
        )
    }

    fun startMigration() {
        val state = _uiState.value
        val selectedTargetSourceNames = state.selectedTargetSources.map { it.name }.distinct()
        val hasSourceScope = state.selectedFromSource != null
        val hasSelectionScope = state.selectedContentIds.isNotEmpty()
        val acceptedPreviews = state.readingSourcePreviews.filter { it.mangaId in state.acceptedReadingPreviewIds }
        if ((!hasSelectionScope && !hasSourceScope) || selectedTargetSourceNames.isEmpty() || acceptedPreviews.isEmpty()) {
            return
        }

        Log.d(
            TAG,
            "startMigration: selectedIds=${state.selectedContentIds.size}, from=${state.selectedFromSource?.name}, " +
                "targets=${selectedTargetSourceNames.joinToString()} concurrency=${state.concurrency}",
        )

        val input = workDataOf(
            SourceMigrationWorker.KEY_CONCURRENCY to state.concurrency,
            SourceMigrationWorker.KEY_TARGET_SOURCES to selectedTargetSourceNames.toTypedArray(),
            SourceMigrationWorker.KEY_SELECTED_CONTENT_IDS to state.selectedContentIds.toLongArray(),
            SourceMigrationWorker.KEY_FROM_SOURCE to state.selectedFromSource?.name,
            SourceMigrationWorker.KEY_PREVIEW_MANGA_IDS to acceptedPreviews.map { it.mangaId }.toLongArray(),
            SourceMigrationWorker.KEY_PREVIEW_TARGET_IDS to acceptedPreviews.map { it.targetContentId }.toLongArray(),
            SourceMigrationWorker.KEY_PREVIEW_ACTIONS to acceptedPreviews.map { it.action.ordinal }.toIntArray(),
        )
        val workManager = WorkManager.getInstance(appContext)
        val request = OneTimeWorkRequestBuilder<SourceMigrationWorker>()
            .setInputData(input)
            .addTag(SourceMigrationWorker.WORK_TAG)
            .build()

        removeMigrationObserver()
        workManager.enqueue(request)
        val workId = request.id.toString()

        _uiState.value = state.copy(
            isExecuting = true,
            workId = workId,
            isFinished = false,
            migrationProgress = MigrationProgress(
                total = 0,
                completed = 0,
                failed = 0,
                notFound = 0,
                reused = 0,
                attached = 0,
                currentItem = null,
                items = emptyList(),
            ),
        )

        migrationObserver = Observer { workInfo ->
            workInfo ?: return@Observer
            val currentState = _uiState.value
            val progressData = workInfo.progress
            val outputData = workInfo.outputData
            val prevProgress = currentState.migrationProgress
            val total = maxOf(progressData.getInt(SourceMigrationWorker.KEY_TOTAL, 0), prevProgress?.total ?: 0)
            val completed = maxOf(progressData.getInt(SourceMigrationWorker.KEY_COMPLETED, 0), prevProgress?.completed ?: 0)
            val failed = maxOf(progressData.getInt(SourceMigrationWorker.KEY_FAILED, 0), prevProgress?.failed ?: 0)
            val notFound = maxOf(progressData.getInt(SourceMigrationWorker.KEY_NOT_FOUND, 0), prevProgress?.notFound ?: 0)
            val reused = maxOf(
                progressData.getInt(SourceMigrationWorker.KEY_REUSED, 0),
                outputData.getInt(SourceMigrationWorker.KEY_REUSED, 0),
                prevProgress?.reused ?: 0,
            )
            val attached = maxOf(
                progressData.getInt(SourceMigrationWorker.KEY_ATTACHED, 0),
                outputData.getInt(SourceMigrationWorker.KEY_ATTACHED, 0),
                prevProgress?.attached ?: 0,
            )
            val currentTitle = progressData.getString(SourceMigrationWorker.KEY_CURRENT_TITLE)
            val finished = progressData.getBoolean(SourceMigrationWorker.KEY_FINISHED, false) || workInfo.state.isFinished
            val feedbackMessage = when {
                !workInfo.state.isFinished -> null
                !outputData.getString(SourceMigrationWorker.KEY_MESSAGE).isNullOrBlank() ->
                    outputData.getString(SourceMigrationWorker.KEY_MESSAGE)
                workInfo.state == WorkInfo.State.SUCCEEDED ->
                    appContext.getString(
                        R.string.entity_organize_reading_execute_feedback,
                        completed,
                        reused,
                        attached,
                        failed,
                        notFound,
                    )
                else -> appContext.getString(R.string.entity_organize_reading_execute_incomplete)
            }
            _uiState.value = currentState.copy(
                migrationProgress = MigrationProgress(
                    total = total,
                    completed = completed,
                    failed = failed,
                    notFound = notFound,
                    reused = reused,
                    attached = attached,
                    currentItem = currentTitle?.takeIf { it.isNotBlank() }?.let {
                        com.mangaverse.app.favourites.domain.MigrationItem(
                            mangaId = -1L,
                            title = it,
                        )
                    } ?: prevProgress?.currentItem,
                    items = prevProgress?.items ?: emptyList(),
                    isFinished = finished,
                ),
                isExecuting = !workInfo.state.isFinished,
                isFinished = workInfo.state == WorkInfo.State.SUCCEEDED,
                stageFeedbacks = if (feedbackMessage != null) {
                    currentState.stageFeedbacks.withFeedback(
                        stage = EntityOrganizeStage.READING,
                        kind = EntityOrganizeFeedbackKind.EXECUTE,
                        message = feedbackMessage,
                    )
                } else {
                    currentState.stageFeedbacks
                },
            )
        }

        workManager.getWorkInfoByIdLiveData(request.id).observeForever(migrationObserver!!)
    }

    fun cancelMigration() {
        val workId = _uiState.value.workId ?: return
        WorkManager.getInstance(appContext).cancelWorkById(java.util.UUID.fromString(workId))
        removeMigrationObserver()
        _uiState.value = _uiState.value.copy(isExecuting = false, isFinished = true)
    }

    private fun refreshMergeCandidates() {
        val requestState = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            refreshMergeCandidatesNow(requestState)
        }
    }

    private suspend fun refreshMergeCandidatesNow(requestState: MigrationUiState = _uiState.value) {
        val favourites = loadScopedFavouriteContents(requestState)
        val contents = favourites.map { it.toContent() }
        val groups = augmentGroupsWithOrganizableWorkProjections(
            groups = buildWorkbenchGroups(contents, requestState),
            organizableWorks = _uiState.value.organizableWorks,
        )
        val selected = requestState.selectedMergeGroupIds.intersect(groups.map { it.id }.toSet())
        val mergePreviewReady = requestState.mergePreviewReady
        val defaultSelectedGroupIds = if (mergePreviewReady) {
            resolveDefaultSelectedMergeGroupIds(
                groups = groups,
                selectedContentIds = requestState.selectedContentIds,
            )
        } else {
            emptySet()
        }
        val selectedItemsByGroup = buildMap {
            groups.forEach { group ->
                put(
                    group.id,
                    requestState.selectedMergeItemsByGroup[group.id]
                        ?.intersect(group.mangaIds)
                        ?.takeIf { it.isNotEmpty() }
                        ?: group.mangaIds
                            .intersect(requestState.selectedManualMergeMangaIds)
                            .takeIf { it.isNotEmpty() }
                        ?: emptySet(),
                )
            }
        }
        val visibleManualMergeIds = selectedItemsByGroup.values.flatten().toSet()
        val currentState = _uiState.value
        if (!currentState.matchesRefreshRequest(requestState)) {
            Log.d(
                TAG,
                "refreshMergeCandidates stale result ignored: " +
                    "requestSelected=${requestState.selectedContentIds.size}, " +
                    "currentSelected=${currentState.selectedContentIds.size}, " +
                    "requestManual=${requestState.selectedManualMergeMangaIds.size}, " +
                    "currentManual=${currentState.selectedManualMergeMangaIds.size}, " +
                    "requestSource=${requestState.selectedFromSource?.name}, " +
                    "currentSource=${currentState.selectedFromSource?.name}, groups=${groups.size}",
            )
            return
        }
        _uiState.value = currentState.copy(
            scopedFavouriteContents = favourites,
            mergeCandidateGroups = groups,
            mergePreviewReady = mergePreviewReady,
            selectedMergeItemsByGroup = selectedItemsByGroup,
            selectedManualMergeMangaIds = requestState.selectedManualMergeMangaIds
                .intersect(visibleManualMergeIds),
            selectedMergeGroupIds = if (!mergePreviewReady) {
                emptySet()
            } else if (selected.isEmpty()) {
                defaultSelectedGroupIds
            } else {
                selected
            },
        )
    }

    private suspend fun resolveEntityIdsByMangaIds(mangaIds: Collection<Long>): Map<Long, Long> {
        return workResolver.resolveManyByMangaIds(mangaIds)
            .mapValues { it.value.entityId }
            .filterValues { it != null }
            .mapValues { requireNotNull(it.value) }
    }

    private suspend fun buildWorkbenchGroups(
        contents: List<com.mangaverse.app.parsers.model.Content>,
        state: MigrationUiState,
    ): List<MergeCandidateGroup> {
        val candidateGroups = mergeFavoriteEntitiesUseCase.buildCandidateGroups(
            contents = contents,
            options = MergeCandidateOptions(
                fuzzyEnabled = state.fuzzyMergeCandidatesEnabled,
                fuzzyThreshold = state.fuzzyMergeThresholdPercent.coerceIn(80, 100) / 100f,
            ),
        )
        val groupedIds = candidateGroups.flatMapTo(HashSet()) { it.mangaIds }
        val entityIdsByMangaId = resolveEntityIdsByMangaIds(
            contents.map { it.id },
        )
        val preferredLocalIdsByEntity = (candidateGroups.mapNotNull { it.resolvedEntityId } + entityIdsByMangaId.values)
            .distinct()
            .associateWith { entityId ->
                workResolver.resolveByEntityId(entityId)?.preferredMangaId
            }
        val reorderedCandidateGroups = candidateGroups.map { group ->
            group.copy(
                items = sortGroupItems(
                    items = group.items.map(::withResolvedDisplaySourceName),
                    preferredLocalMangaId = group.resolvedEntityId?.let(preferredLocalIdsByEntity::get),
                ),
            )
        }
        val singletonGroups = contents
            .asSequence()
            .filterNot { it.id in groupedIds }
            .groupBy { entityIdsByMangaId[it.id] ?: -it.id }
            .map { (_, groupedContents) ->
                val primary = groupedContents.first()
                val entityId = entityIdsByMangaId[primary.id]
                val hasEntity = entityId != null
                val groupId = if (hasEntity) {
                    "${primary.source.contentType.name}:entity:$entityId"
                } else {
                    "${primary.source.contentType.name}:single:${primary.id}"
                }
                MergeCandidateGroup(
                    id = groupId,
                    title = primary.title,
                    normalizedTitle = primary.title.trim().lowercase(),
                    contentType = primary.source.contentType,
                    mangaIds = groupedContents.mapTo(LinkedHashSet(groupedContents.size)) { it.id },
                    items = sortGroupItems(
                        items = groupedContents.map { content ->
                            com.mangaverse.app.favourites.domain.MergeCandidateItem(
                                mangaId = content.id,
                                title = content.title,
                                normalizedTitle = content.title.trim().lowercase(),
                                sourceName = content.source.name,
                                coverUrl = content.coverUrl,
                                score = 1f,
                            )
                        }.map(::withResolvedDisplaySourceName),
                        preferredLocalMangaId = entityId?.let(preferredLocalIdsByEntity::get),
                    ),
                    matchScore = 1f,
                    isExactMatch = true,
                    resolvedEntityId = entityId,
                    isAlreadyMerged = hasEntity,
                )
            }
        return (reorderedCandidateGroups + singletonGroups).sortedWith(
            compareByDescending<MergeCandidateGroup> { it.mangaIds.size > 1 }
                .thenByDescending { it.isExactMatch }
                .thenByDescending { it.matchScore }
                .thenByDescending { it.mangaIds.size }
                .thenBy { it.title.lowercase() },
        )
    }

    private fun augmentGroupsWithOrganizableWorkProjections(
        groups: List<MergeCandidateGroup>,
        organizableWorks: List<OrganizableWork>,
    ): List<MergeCandidateGroup> {
        if (groups.isEmpty() || organizableWorks.isEmpty()) {
            return groups
        }
        val workByEntityId = organizableWorks.associateBy { it.entityId }
        val entityIdByMangaId = buildMap {
            organizableWorks.forEach { work ->
                work.projections.forEach { projection ->
                    put(projection.mangaId, work.entityId)
                }
            }
        }
        return groups.map { group ->
            val relatedWorks = buildList {
                group.resolvedEntityId
                    ?.let(workByEntityId::get)
                    ?.let(::add)
                group.mangaIds
                    .mapNotNull(entityIdByMangaId::get)
                    .distinct()
                    .mapNotNull(workByEntityId::get)
                    .forEach(::add)
            }.distinctBy { it.entityId }
            if (relatedWorks.isEmpty()) {
                return@map group
            }
            val existingIds = group.mangaIds.toMutableSet()
            val additionalItems = relatedWorks
                .asSequence()
                .flatMap { work -> work.projections.asSequence() }
                .filterNot { projection -> projection.mangaId in existingIds }
                .map { projection ->
                    com.mangaverse.app.favourites.domain.MergeCandidateItem(
                        mangaId = projection.mangaId,
                        title = projection.title,
                        normalizedTitle = projection.title.trim().lowercase(),
                        sourceName = projection.source,
                        coverUrl = null,
                        score = group.matchScore,
                    )
                }
                .map(::withResolvedDisplaySourceName)
                .toList()
            if (additionalItems.isEmpty()) {
                return@map group
            }
            val preferredLocalMangaId = relatedWorks
                .mapNotNull(OrganizableWork::preferredMangaId)
                .firstOrNull { preferredId -> preferredId in (group.mangaIds + additionalItems.map { it.mangaId }) }
            val mergedItems = sortGroupItems(
                items = (group.items + additionalItems).distinctBy { it.mangaId },
                preferredLocalMangaId = preferredLocalMangaId,
            )
            group.copy(
                mangaIds = mergedItems.mapTo(LinkedHashSet(mergedItems.size)) { it.mangaId },
                items = mergedItems,
            )
        }
    }

    private fun sortGroupItems(
        items: List<com.mangaverse.app.favourites.domain.MergeCandidateItem>,
        preferredLocalMangaId: Long?,
    ): List<com.mangaverse.app.favourites.domain.MergeCandidateItem> {
        if (preferredLocalMangaId == null) {
            return items
        }
        return items.sortedWith(
            compareByDescending<com.mangaverse.app.favourites.domain.MergeCandidateItem> { it.mangaId == preferredLocalMangaId }
                .thenBy { it.title.lowercase() },
        )
    }

    private suspend fun loadScopedFavouriteContents(): List<FavouriteContent> {
        return loadScopedFavouriteContents(_uiState.value)
    }

    private suspend fun loadScopedFavouriteContents(state: MigrationUiState): List<FavouriteContent> = when {
        state.selectedContentIds.isNotEmpty() -> {
            entityOrganizeRepository.listFavouriteContentsByMangaIds(state.selectedContentIds)
        }

        state.selectedFromSource != null -> {
            entityOrganizeRepository.listFavouriteContents(
                state.selectedFromSource.name,
            )
        }

        else -> {
            entityOrganizeRepository.listFavouriteContents()
        }
    }

    private fun MigrationUiState.matchesRefreshRequest(request: MigrationUiState): Boolean {
        return selectedContentIds == request.selectedContentIds &&
            selectedFromSource?.name == request.selectedFromSource?.name &&
            selectedManualMergeMangaIds == request.selectedManualMergeMangaIds &&
            fuzzyMergeCandidatesEnabled == request.fuzzyMergeCandidatesEnabled &&
            fuzzyMergeThresholdPercent == request.fuzzyMergeThresholdPercent &&
            mergePreviewReady == request.mergePreviewReady
    }

    private fun loadPreviewScopeEstimate(state: MigrationUiState): Int {
        return when {
            state.selectedContentIds.isNotEmpty() -> state.selectedContentIds.size
            state.selectedFromSource != null -> state.mergeCandidateGroups.count {
                it.items.firstOrNull()?.sourceName == state.selectedFromSource.name
            }
            else -> state.mergeCandidateGroups.size
        }
    }

    private fun resolveDefaultSelectedMergeGroupIds(
        groups: List<MergeCandidateGroup>,
        selectedContentIds: Set<Long>,
    ): Set<String> {
        val defaultSelectableGroups = groups.asSequence()
            .filter { it.isExecutableMergeCandidate() }
            .filterNot { it.id.contains(":fuzzy:") }
        if (selectedContentIds.isEmpty()) {
            return defaultSelectableGroups.mapTo(LinkedHashSet()) { it.id }
        }
        return defaultSelectableGroups
            .filter { group -> group.mangaIds.any(selectedContentIds::contains) }
            .mapTo(LinkedHashSet()) { it.id }
    }

    private fun removeMigrationObserver() {
        val observer = migrationObserver ?: return
        val workId = _uiState.value.workId ?: return
        runCatching {
            WorkManager.getInstance(appContext)
                .getWorkInfoByIdLiveData(java.util.UUID.fromString(workId))
                .removeObserver(observer)
        }
        migrationObserver = null
    }

    private fun filterSources(
        sources: List<ContentSource>,
        contentTypeFilter: Set<BrowseGroupTab>,
        sourceTagFilter: Set<SourceTag>,
    ): List<ContentSource> {
        if (contentTypeFilter.isEmpty() && sourceTagFilter.isEmpty()) return sources
        return sources.filter { source ->
            val sourceName = source.name
            val contentGroup = sourceGroupManager.getContentGroupByName(sourceName, source.isNsfw())
            val originGroup = sourceGroupManager.getOriginGroupByName(sourceName)
            val contentTypeMatch = contentTypeFilter.isEmpty() ||
                contentTypeFilter.any { it.matchesContentGroup(contentGroup) }
            val sourceTagMatch = sourceTagFilter.isEmpty() ||
                sourceTagFilter.any { it.matches(contentGroup, originGroup) }
            contentTypeMatch && sourceTagMatch
        }
    }

    private fun withResolvedDisplaySourceName(
        item: com.mangaverse.app.favourites.domain.MergeCandidateItem,
    ): com.mangaverse.app.favourites.domain.MergeCandidateItem {
        return item.copy(displaySourceName = resolveDisplaySourceName(item.sourceName))
    }

    private fun withResolvedDisplaySourceName(
        preview: ReadingSourcePreview,
    ): ReadingSourcePreview {
        return preview.copy(targetSourceDisplayName = resolveDisplaySourceName(preview.targetSourceName))
    }

    private fun resolveDisplaySourceName(sourceName: String): String {
        return mihonExtensionManager.getMihonMangaSourceByName(sourceName)?.displayName ?: sourceName
    }
}

private fun Map<EntityOrganizeStage, EntityOrganizeFeedback>.withFeedback(
    stage: EntityOrganizeStage,
    kind: EntityOrganizeFeedbackKind,
    message: String,
): Map<EntityOrganizeStage, EntityOrganizeFeedback> {
    return this + (stage to EntityOrganizeFeedback(stage = stage, kind = kind, message = message))
}

private fun Map<EntityOrganizeStage, EntityOrganizeFeedback>.without(
    stage: EntityOrganizeStage,
): Map<EntityOrganizeStage, EntityOrganizeFeedback> {
    return this - stage
}

internal fun updateMergeGroupSelection(
    current: Set<String>,
    groupIds: Set<String>,
    selected: Boolean,
): Set<String> {
    return if (selected) current + groupIds else current - groupIds
}

internal fun buildSelectedMergeGroupsForExecution(
    groups: List<MergeCandidateGroup>,
    selectedGroupIds: Set<String>,
    selectedItemsByGroup: Map<String, Set<Long>>,
): List<MergeCandidateGroup> {
    return groups
        .filter { it.id in selectedGroupIds && it.isExecutableMergeCandidate() }
        .mapNotNull { group ->
            group.withMergeableSelectedItems(
                selectedItemsByGroup[group.id]?.takeIf { it.isNotEmpty() },
            )
        }
}

private fun MergeCandidateGroup.withScopedItems(selectedIds: Set<Long>?): MergeCandidateGroup? {
    val resolvedIds = selectedIds
        ?.intersect(mangaIds)
        ?: mangaIds
    if (resolvedIds.isEmpty()) {
        return null
    }
    return copy(
        mangaIds = resolvedIds,
        items = items.filter { it.mangaId in resolvedIds },
    )
}

private fun MergeCandidateGroup.withMergeableSelectedItems(selectedIds: Set<Long>?): MergeCandidateGroup? {
    val scoped = withScopedItems(selectedIds) ?: return null
    return scoped.takeIf { it.mangaIds.size >= 2 }
}

internal fun <T> clearSelectionIds(
    current: Set<T>,
    idsToClear: Set<T>,
): Set<T> {
    return current - idsToClear
}
