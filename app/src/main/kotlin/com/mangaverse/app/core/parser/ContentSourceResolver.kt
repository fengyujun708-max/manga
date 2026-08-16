package com.mangaverse.app.core.parser

import com.mangaverse.app.parsers.model.ContentSource

interface ContentSourceResolver {
	fun supports(source: ContentSource): Boolean = true
	fun resolve(source: ContentSource): ContentSource?
}
