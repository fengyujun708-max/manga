package com.mangaverse.app.local.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.mangaverse.app.core.model.ids
import com.mangaverse.app.core.model.isLocal
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.history.data.HistoryRepository
import com.mangaverse.app.local.data.LocalMangaRepository
import com.mangaverse.app.local.domain.model.LocalContent
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.util.findById
import com.mangaverse.app.parsers.util.recoverCatchingCancellable
import com.mangaverse.app.parsers.util.runCatchingCancellable
import javax.inject.Inject
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.db.entity.toContentChapters

class DeleteReadChaptersUseCase @Inject constructor(
	private val localContentRepository: LocalMangaRepository,
	private val historyRepository: HistoryRepository,
	private val mangaRepositoryFactory: ContentRepository.Factory,
	private val db: MangaDatabase,
) {

	suspend operator fun invoke(manga: Content): Int {
		val localContent = if (manga.isLocal) {
			LocalContent(manga)
		} else {
			checkNotNull(localContentRepository.findSavedContent(manga)) { "Cannot find local manga" }
		}
		val task = getDeletionTask(localContent) ?: return 0
		localContentRepository.deleteChapters(task.manga.manga, task.chaptersIds)
		return task.chaptersIds.size
	}

	suspend operator fun invoke(): Int {
		val list = localContentRepository.getList(0, null, null)
		if (list.isEmpty()) {
			return 0
		}
		return channelFlow {
			for (manga in list) {
				launch(Dispatchers.Default) {
					val task = runCatchingCancellable {
						val localContent = if (manga.isLocal) {
							LocalContent(manga)
						} else {
							localContentRepository.findSavedContent(manga)
						}
						localContent?.let { getDeletionTask(it) }
					}.onFailure {
						it.printStackTraceDebug()
					}.getOrNull()
					if (task != null) {
						send(task)
					}
				}
			}
		}.buffer().map {
			runCatchingCancellable {
				localContentRepository.deleteChapters(it.manga.manga, it.chaptersIds)
				it.chaptersIds.size
			}.onFailure {
				it.printStackTraceDebug()
			}.getOrDefault(0)
		}.fold(0) { acc, x -> acc + x }
	}

	private suspend fun getDeletionTask(manga: LocalContent): DeletionTask? {
		val history = historyRepository.getOne(manga.manga) ?: return null
		val localChapters = getLocalChapters(manga)
		val remoteMangaId = runCatchingCancellable {
			localContentRepository.getRemoteContent(manga.manga)?.id
		}.getOrNull() ?: manga.manga.id
		val dbChapters = runCatchingCancellable {
			db.getChaptersDao().findAll(remoteMangaId).toContentChapters()
		}.getOrDefault(emptyList())
		val combined = (localChapters + dbChapters).distinctBy { it.id }


		val chapters = if (combined.any { it.id == history.chapterId }) {
			combined
		} else {
			getAllChaptersRemote(manga, combined)
		}
		if (chapters.isEmpty()) {
			return null
		}
		val sortedChapters = chapters.sortedBy { it.number }
		val historyChapter = sortedChapters.findById(history.chapterId) ?: return null
		val branch = historyChapter.branch
		val filteredChapters = sortedChapters
			.filter { x -> x.branch == branch }
			.takeWhile { it.id != historyChapter.id }

		val toDeleteIds = filteredChapters.ids().intersect(localChapters.ids())
		return if (toDeleteIds.isEmpty()) {
			null
		} else {
			DeletionTask(
				manga = manga,
				chaptersIds = toDeleteIds,
			)
		}
	}

	private suspend fun getLocalChapters(manga: LocalContent): List<ContentChapter> {
		return manga.manga.chapters.let {
			if (it.isNullOrEmpty()) {
				runCatchingCancellable {
					localContentRepository.getDetails(manga.manga).chapters
				}.getOrNull()
			} else {
				it
			}
		}.orEmpty()
	}

	private suspend fun getAllChaptersRemote(manga: LocalContent, fallback: List<ContentChapter>): List<ContentChapter> = runCatchingCancellable {
		val remoteContent = checkNotNull(localContentRepository.getRemoteContent(manga.manga))
		checkNotNull(mangaRepositoryFactory.create(remoteContent.source).getDetails(remoteContent).chapters)
	}.getOrDefault(fallback)

	private class DeletionTask(
		val manga: LocalContent,
		val chaptersIds: Set<Long>,
	)
}
