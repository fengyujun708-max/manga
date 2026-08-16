package com.mangaverse.app.details.ui.model

import com.mangaverse.app.parsers.model.ContentSource

data class EntityChapterSourceInfo(
    val source: ContentSource?,
    val projectionTitle: String? = null,
    val projectionCount: Int = 0,
    val activeProjectionMangaId: Long? = null,
    val currentReadingProjectionMangaId: Long? = null,
)
