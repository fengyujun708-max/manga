package com.mangaverse.app.details.ui.model

import com.mangaverse.app.parsers.model.ContentSource

data class ActiveLocalSourceOption(
    val mangaId: Long,
    val title: String,
    val source: ContentSource,
    val isActive: Boolean,
)
