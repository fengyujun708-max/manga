package com.mangaverse.app.tracker.ui.debug

import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.parsers.model.Content
import java.time.Instant

data class TrackDebugItem(
	val manga: Content,
	val lastChapterId: Long,
	val newChapters: Int,
	val lastCheckTime: Instant?,
	val lastChapterDate: Instant?,
	val lastResult: Int,
	val lastError: String?,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is TrackDebugItem && other.manga.id == manga.id
	}
}
