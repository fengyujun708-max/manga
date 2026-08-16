package com.mangaverse.app.tracker.domain.model

import com.mangaverse.app.parsers.exception.TooManyRequestExceptions
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.util.ifZero

sealed interface MangaUpdates {

	val manga: Content
	val entityId: Long?
	val anchorMangaId: Long

	data class Success(
		override val manga: Content,
		override val entityId: Long,
		override val anchorMangaId: Long,
		val branch: String?,
		val newChapters: List<ContentChapter>,
		val isValid: Boolean,
	) : MangaUpdates {

		val newChapterKeys: List<SourceChapterKey> = newChapters.map(ContentChapter::sourceScopedKey)

		fun isNotEmpty() = newChapters.isNotEmpty()

		fun lastChapterDate(): Long {
			val lastChapter = newChapters.lastOrNull()
			return lastChapter?.uploadDate?.ifZero { System.currentTimeMillis() }
				?: (manga.chapters?.lastOrNull()?.uploadDate ?: 0L)
		}
	}

	data class Failure(
		override val manga: Content,
		override val entityId: Long?,
		override val anchorMangaId: Long,
		val error: Throwable?,
	) : MangaUpdates {

		fun shouldRetry() = error is TooManyRequestExceptions
	}
}

data class SourceChapterKey(
	val source: String,
	val key: String,
)

private fun ContentChapter.sourceScopedKey() = SourceChapterKey(
	source = source.name,
	key = url.ifBlank { id.toString() },
)
