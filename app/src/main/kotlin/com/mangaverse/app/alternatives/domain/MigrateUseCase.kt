package com.mangaverse.app.alternatives.domain

import androidx.room.withTransaction
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.model.ContentHistory
import com.mangaverse.app.core.model.getPreferredBranch
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.details.domain.ProgressUpdateUseCase
import com.mangaverse.app.entitygraph.data.EntityGraphRepository
import com.mangaverse.app.history.data.WorkHistoryEntity
import com.mangaverse.app.list.domain.ReadingProgress.Companion.PROGRESS_NONE
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.util.runCatchingCancellable
import com.mangaverse.app.core.domain.model.ScrobblingStatus
import com.mangaverse.app.tracker.data.TrackEntity
import com.mangaverse.app.tracker.data.resolveTrackOwnerId
import java.time.Instant
import javax.inject.Inject

class MigrateUseCase
@Inject
constructor(
	private val mangaRepositoryFactory: ContentRepository.Factory,
	private val mangaDataRepository: ContentDataRepository,
	private val database: MangaDatabase,
	private val entityGraphRepository: EntityGraphRepository,
	private val progressUpdateUseCase: ProgressUpdateUseCase,
) {
	suspend operator fun invoke(
		oldContent: Content,
		newContent: Content,
	) {
		val oldDetails = if (oldContent.chapters.isNullOrEmpty()) {
			runCatchingCancellable {
				mangaRepositoryFactory.create(oldContent.source).getDetails(oldContent)
			}.getOrDefault(oldContent)
		} else {
			oldContent
		}
		val newDetails = if (newContent.chapters.isNullOrEmpty()) {
			mangaRepositoryFactory.create(newContent.source).getDetails(newContent)
		} else {
			newContent
		}
		val storedNewDetails = mangaDataRepository.storeContentAndReturn(newDetails, replaceExisting = true)
		database.withTransaction {
			val currentTime = System.currentTimeMillis()
			val oldLocalReadingBinding = entityGraphRepository.findLocalReadingBinding(oldDetails.id)
			val newLocalReadingBinding = entityGraphRepository.findLocalReadingBinding(storedNewDetails.id)
			var targetEntityId = oldLocalReadingBinding?.entityId ?: newLocalReadingBinding?.entityId
			// Source migration adds the selected projection to the current Work. If the
			// projection already has a separate Work, merge that identity into the current Work.
			targetEntityId?.let { entityId ->
				newLocalReadingBinding
					?.entityId
					?.takeIf { it != entityId }
					?.let { sourceEntityId ->
						entityGraphRepository.mergeEntities(
							targetEntityId = entityId,
							sourceEntityIds = listOf(sourceEntityId),
						)
					}
				entityGraphRepository.attachLocalWorksToEntity(
					entityId = entityId,
					contents = listOf(storedNewDetails),
				)
				targetEntityId = entityGraphRepository.findLocalReadingBinding(storedNewDetails.id)?.entityId
					?: entityId
			}
			// replace favorites
			val workFavouritesDao = database.getWorkFavouritesDao()
			val oldFavourites = workFavouritesDao.findActiveByAnchorMangaId(oldDetails.id)
			if (oldFavourites.isNotEmpty()) {
				for (favourite in oldFavourites) {
					workFavouritesDao.upsert(
						favourite.copy(
							anchorMangaId = storedNewDetails.id,
							updatedAt = currentTime,
						),
					)
				}
			}
			// replace history
			val workHistoryDao = database.getWorkHistoryDao()
			val oldHistory = workHistoryDao.findActiveByAnchorMangaId(oldDetails.id)
			val newHistory =
				if (oldHistory != null) {
					val newHistory = makeNewHistory(oldDetails, storedNewDetails, oldHistory)
					workHistoryDao.upsert(newHistory)
					newHistory
				} else {
					null
				}
			// Only projection-local prefs should follow source migration.
			// Work-owned state such as metadata authority, overrides, and reading status must stay on entity/work.
			database.getPreferencesDao().find(oldDetails.id)?.let { pref ->
				database.getPreferencesDao().upsert(
					pref.copy(
						mangaId = storedNewDetails.id,
						titleOverride = null,
						coverUrlOverride = null,
						contentRatingOverride = null,
						metadataSourceKind = null,
						metadataSourceService = null,
						metadataSourceRemoteId = null,
						readingStatus = null,
					),
				)
			}
			// track
			val tracksDao = database.getTracksDao()
			val oldTrack = tracksDao.find(oldDetails.id)
			if (oldTrack != null) {
				val lastChapter = storedNewDetails.chapters?.lastOrNull()
				val newTrack =
					TrackEntity(
						ownerId = resolveTrackOwnerId(targetEntityId, storedNewDetails.id),
						mangaId = storedNewDetails.id,
						entityId = targetEntityId,
						lastChapterId = lastChapter?.id ?: 0L,
						newChapters = 0,
						lastCheckTime = currentTime,
						lastChapterDate = lastChapter?.uploadDate ?: 0L,
						lastResult = TrackEntity.RESULT_EXTERNAL_MODIFICATION,
						lastError = null,
					)
				tracksDao.delete(oldDetails.id)
				tracksDao.upsert(newTrack)
			}
		}
		progressUpdateUseCase(storedNewDetails)
	}

	private fun makeNewHistory(
		oldContent: Content,
		newContent: Content,
		history: WorkHistoryEntity,
	): WorkHistoryEntity {
		if (oldContent.chapters.isNullOrEmpty()) { // probably broken manga/source
			val branch = newContent.getPreferredBranch(null)
			val chapters = checkNotNull(newContent.getChapters(branch))
			val currentChapter =
				if (history.percent in 0f..1f) {
					chapters[(chapters.lastIndex * history.percent).toInt()]
				} else {
					chapters.first()
				}
			return history.copy(
				anchorMangaId = newContent.id,
				chapterId = currentChapter.id,
				page = history.page,
				scroll = history.scroll,
				percent = history.percent,
				deletedAt = 0,
				chaptersCount = chapters.count { it.branch == currentChapter.branch },
			)
		}
		val branch = oldContent.getPreferredBranch(history.toContentHistory())
		val oldChapters = checkNotNull(oldContent.getChapters(branch))
		var index = oldChapters.indexOfFirst { it.id == history.chapterId }
		if (index < 0) {
			index =
				if (history.percent in 0f..1f) {
					(oldChapters.lastIndex * history.percent).toInt()
				} else {
					0
				}
		}
		val newChapters = checkNotNull(newContent.chapters).groupBy { it.branch }
		val newBranch =
			if (newChapters.containsKey(branch)) {
				branch
			} else {
				newContent.getPreferredBranch(null)
			}
		val newChapterId =
			checkNotNull(newChapters[newBranch])
				.let {
					val oldChapter = oldChapters[index]
					it.findByNumber(oldChapter.volume, oldChapter.number) ?: it.getOrNull(index) ?: it.last()
				}.id

		return history.copy(
			anchorMangaId = newContent.id,
			chapterId = newChapterId,
			page = history.page,
			scroll = history.scroll,
			percent = PROGRESS_NONE,
			deletedAt = 0,
			chaptersCount = checkNotNull(newChapters[newBranch]).size,
		)
	}

	private fun WorkHistoryEntity.toContentHistory() = ContentHistory(
		createdAt = Instant.ofEpochMilli(createdAt),
		updatedAt = Instant.ofEpochMilli(updatedAt),
		chapterId = chapterId,
		page = page,
		scroll = scroll.toInt(),
		percent = percent,
		chaptersCount = chaptersCount,
		parentChapterId = parentChapterId,
	)

	private fun List<ContentChapter>.findByNumber(
		volume: Int,
		number: Float,
	): ContentChapter? =
		if (number <= 0f) {
			null
		} else {
			firstOrNull { it.volume == volume && it.number == number }
		}
}
