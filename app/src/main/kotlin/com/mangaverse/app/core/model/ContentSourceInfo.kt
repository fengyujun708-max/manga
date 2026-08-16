package com.mangaverse.app.core.model

import com.mangaverse.app.parsers.model.ContentSource

data class ContentSourceInfo(
	val mangaSource: ContentSource,
	val isEnabled: Boolean,
	val isPinned: Boolean,
	val availability: ContentSourceAvailability = ContentSourceAvailability.UNKNOWN,
) : ContentSource by mangaSource
