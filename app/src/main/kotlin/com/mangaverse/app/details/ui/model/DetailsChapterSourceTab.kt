package com.mangaverse.app.details.ui.model

import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.model.ContentSource

data class DetailsChapterSourceTab(
    val key: String,
    val source: ContentSource? = null,
    val targetMangaId: Long? = null,
    val remoteId: Long? = null,
    val url: String? = null,
    val chapters: List<ContentChapter> = emptyList(),
    val isSelected: Boolean = false,
)
