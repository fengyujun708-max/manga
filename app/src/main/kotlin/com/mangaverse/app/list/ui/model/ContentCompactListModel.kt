package com.mangaverse.app.list.ui.model

import com.mangaverse.app.core.ui.model.ContentOverride
import com.mangaverse.app.list.domain.ReadingProgress
import com.mangaverse.app.parsers.model.Content

data class ContentCompactListModel(
	override val manga: Content,
	override val override: ContentOverride?,
	val subtitle: String?,
	val supportingText: String? = null,
	override val counter: Int,
	override val projectionCount: Int = 0,
	override val id: Long = manga.id,
	val progress: ReadingProgress? = null,
	override val isPinned: Boolean = false,
	override val scoreText: String? = null,
) : ContentListModel()
