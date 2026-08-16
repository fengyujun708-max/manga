package com.mangaverse.app.suggestions.domain

import androidx.annotation.FloatRange
import com.mangaverse.app.parsers.model.Content

data class ContentSuggestion(
	val manga: Content,
	@FloatRange(from = 0.0, to = 1.0)
	val relevance: Float,
)