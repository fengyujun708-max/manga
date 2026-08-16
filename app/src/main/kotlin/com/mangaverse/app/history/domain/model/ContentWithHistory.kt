package com.mangaverse.app.history.domain.model

import com.mangaverse.app.core.model.ContentHistory
import com.mangaverse.app.parsers.model.Content

data class ContentWithHistory(
	val manga: Content,
	val history: ContentHistory,
	val entityId: Long?,
	val preferredLocalMangaId: Long?,
)
