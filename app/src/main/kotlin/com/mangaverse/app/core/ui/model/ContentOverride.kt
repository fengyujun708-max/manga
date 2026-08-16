package com.mangaverse.app.core.ui.model

import com.mangaverse.app.parsers.model.ContentRating

data class ContentOverride(
	val coverUrl: String?,
	val title: String?,
	val contentRating: ContentRating?,
)
