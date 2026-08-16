package com.mangaverse.app.list.ui.model

import com.mangaverse.app.core.ui.model.ContentOverride
import com.mangaverse.app.core.ui.widgets.ChipsView
import com.mangaverse.app.list.domain.ReadingProgress
import com.mangaverse.app.list.ui.ListModelDiffCallback.Companion.PAYLOAD_ANYTHING_CHANGED
import com.mangaverse.app.list.ui.ListModelDiffCallback.Companion.PAYLOAD_PROGRESS_CHANGED
import com.mangaverse.app.parsers.model.Content

data class ContentDetailedListModel(
	override val manga: Content,
	override val override: ContentOverride?,
	val subtitle: String?,
	val supportingText: String? = null,
	override val counter: Int,
	override val projectionCount: Int = 0,
	override val id: Long = manga.id,
	val progress: ReadingProgress?,
	val isFavorite: Boolean,
	val isSaved: Boolean,
	val tags: List<ChipsView.ChipModel>,
	override val isPinned: Boolean = false,
	override val scoreText: String? = null,
) : ContentListModel() {

	override fun getChangePayload(previousState: ListModel): Any? = when {
		previousState !is ContentDetailedListModel || previousState.manga != manga -> null

		previousState.progress != progress -> PAYLOAD_PROGRESS_CHANGED
		previousState.subtitle != subtitle ||
			previousState.supportingText != supportingText ||
			previousState.projectionCount != projectionCount ||
			previousState.isFavorite != isFavorite ||
			previousState.isSaved != isSaved ||
			previousState.scoreText != scoreText -> PAYLOAD_ANYTHING_CHANGED

		else -> super.getChangePayload(previousState)
	}
}
