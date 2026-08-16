package com.mangaverse.app.filter.ui.model

import com.mangaverse.app.parsers.model.ContentTag

data class UiTagGroup(
    val title: String,
    val tags: Set<ContentTag>,
    val selected: Set<ContentTag> = emptySet(),
    val isExclusive: Boolean = false,
)
