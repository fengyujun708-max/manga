package com.mangaverse.app.core.parser

import com.mangaverse.app.core.model.ContentSourceInfo
import com.mangaverse.app.parsers.model.ContentSource
import javax.inject.Inject

class ContentSourceInfoResolver @Inject constructor() : ContentSourceResolver {
	override fun supports(source: ContentSource): Boolean = source is ContentSourceInfo

	override fun resolve(source: ContentSource): ContentSource? {
		return (source as? ContentSourceInfo)?.mangaSource
	}
}
