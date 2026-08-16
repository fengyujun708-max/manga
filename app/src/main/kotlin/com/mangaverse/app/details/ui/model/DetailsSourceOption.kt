package com.mangaverse.app.details.ui.model

import com.mangaverse.app.parsers.model.ContentSource

data class DetailsSourceOption(
    val key: String,
    val source: ContentSource? = null,
    val targetMangaId: Long? = null,
    val remoteId: Long? = null,
    val url: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val coverUrl: String? = null,
    val isSelected: Boolean = false,
)
