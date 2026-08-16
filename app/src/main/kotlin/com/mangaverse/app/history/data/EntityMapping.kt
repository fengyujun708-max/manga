package com.mangaverse.app.history.data

import com.mangaverse.app.core.model.ContentHistory
import java.time.Instant

fun HistoryEntity.toContentHistory() = ContentHistory(
	createdAt = Instant.ofEpochMilli(createdAt),
	updatedAt = Instant.ofEpochMilli(updatedAt),
	chapterId = chapterId,
	page = page,
	scroll = scroll.toInt(),
	percent = percent,
	chaptersCount = chaptersCount,
	parentChapterId = parentChapterId,
)
