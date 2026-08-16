package com.mangaverse.app.core.parser

import com.mangaverse.app.parsers.model.ContentSource

interface ContentRepositoryProvider {
	fun supports(source: ContentSource): Boolean = true
	fun create(source: ContentSource): ContentRepository?
}
