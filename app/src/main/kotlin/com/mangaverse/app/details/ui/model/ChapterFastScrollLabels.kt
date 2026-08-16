package com.mangaverse.app.details.ui.model

import com.mangaverse.app.list.ui.model.ListModel

fun List<ListModel>.chapterFastScrollLabelAt(position: Int): String? {
	if (isEmpty()) {
		return null
	}
	val target = position.coerceIn(indices)
	var ordinal = 0
	var lastChapterOrdinal: Int? = null
	forEachIndexed { index, item ->
		if (item is ChapterListItem) {
			ordinal++
			lastChapterOrdinal = ordinal
		}
		if (index >= target && lastChapterOrdinal != null) {
			return lastChapterOrdinal.toString()
		}
	}
	return lastChapterOrdinal?.toString()
}
