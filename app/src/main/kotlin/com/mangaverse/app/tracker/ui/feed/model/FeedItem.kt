package com.mangaverse.app.tracker.ui.feed.model

import com.mangaverse.app.core.model.withOverride
import com.mangaverse.app.core.ui.model.ContentOverride
import com.mangaverse.app.list.ui.ListModelDiffCallback
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.util.ifNullOrEmpty

data class FeedItem(
	val id: Long,
	val entityId: Long?,
	val preferredLocalMangaId: Long?,
	private val override: ContentOverride?,
	val manga: Content,
	val count: Int,
	val isNew: Boolean,
	val totalChapters: Int = 0,
) : ListModel {

	val imageUrl: String?
		get() = override?.coverUrl.ifNullOrEmpty { manga.coverUrl }

	val title: String
		get() = override?.title.ifNullOrEmpty { manga.title }

	fun toContentWithOverride() = manga.withOverride(override)

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is FeedItem && other.id == id
	}

	override fun getChangePayload(previousState: ListModel): Any? = when {
		previousState !is FeedItem -> null
		isNew != previousState.isNew -> ListModelDiffCallback.PAYLOAD_ANYTHING_CHANGED
		else -> super.getChangePayload(previousState)
	}
}
