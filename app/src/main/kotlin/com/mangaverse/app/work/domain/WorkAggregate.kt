package com.mangaverse.app.work.domain

import com.mangaverse.app.core.model.FavouriteCategory
import com.mangaverse.app.favourites.data.WorkFavouriteEntity
import com.mangaverse.app.history.data.WorkHistoryEntity
import com.mangaverse.app.list.domain.ListFilterOption
import com.mangaverse.app.list.domain.ReadingProgress
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.core.domain.model.ScrobblingStatus

data class WorkStatsSummary(
	val totalPages: Int = 0,
	val averageTimePerPage: Long = 0L,
	val entryCount: Int = 0,
)

data class WorkTrackingSummary(
	val anchorMangaId: Long,
	val lastChapterId: Long,
	val newChapters: Int,
	val lastCheckTime: Long,
	val lastChapterDate: Long,
)

data class WorkAggregate(
	val identity: WorkIdentity,
	val displayProjection: Content?,
	val projections: List<Content>,
	val categories: Set<FavouriteCategory> = emptySet(),
	val history: WorkHistoryEntity? = null,
	val favourite: WorkFavouriteEntity? = null,
	val stats: WorkStatsSummary? = null,
	val tracking: WorkTrackingSummary? = null,
)

internal fun WorkAggregate.matchesFavouriteMacroFilter(
	option: ListFilterOption.Macro,
	brokenProjectionSourceNames: Set<String> = emptySet(),
): Boolean = when (option) {
	ListFilterOption.Macro.COMPLETED -> history?.percent?.let(ReadingProgress::isCompleted) == true
	ListFilterOption.Macro.NEW_CHAPTERS -> (tracking?.newChapters ?: 0) > 0
	ListFilterOption.Macro.BROKEN_PROJECTION -> projections
		.ifEmpty { listOfNotNull(displayProjection) }
		.any { it.source.name in brokenProjectionSourceNames }
	else -> true
}

internal fun Content.matchesPublicationStateFilters(filterOptions: Set<ListFilterOption>): Boolean {
	val selectedStates = filterOptions.asSequence()
		.filterIsInstance<ListFilterOption.PublicationState>()
		.map(ListFilterOption.PublicationState::state)
		.toSet()
	return selectedStates.isEmpty() || state in selectedStates
}

internal fun WorkAggregate.resolveReadingStatus(explicitStatus: ScrobblingStatus?): ScrobblingStatus =
	explicitStatus ?: when {
		history == null -> ScrobblingStatus.PLANNED
		ReadingProgress.isCompleted(history.percent) -> ScrobblingStatus.COMPLETED
		else -> ScrobblingStatus.READING
	}

internal fun ScrobblingStatus.matchesReadingStatusFilters(filterOptions: Set<ListFilterOption>): Boolean {
	val selectedStatuses = filterOptions.asSequence()
		.filterIsInstance<ListFilterOption.ReadingStatus>()
		.map(ListFilterOption.ReadingStatus::status)
		.toSet()
	return selectedStatuses.isEmpty() || this in selectedStatuses
}
