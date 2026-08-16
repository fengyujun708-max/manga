package com.mangaverse.app.favourites.domain.model

import com.mangaverse.app.core.model.ContentSource

data class Cover(
	val mangaId: Long,
	val url: String?,
	val source: String,
) {
	val mangaSource by lazy { ContentSource(source) }
}
