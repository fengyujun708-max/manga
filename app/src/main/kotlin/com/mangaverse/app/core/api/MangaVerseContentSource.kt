package com.mangaverse.app.core.api

import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentType

/**
 * MangaVerse API 内容源。
 *
 * 代表通过 MangaVerse 后端（/api 路由）获取漫画内容的统一源。
 * 不暴露具体底层源名，只呈现为"MangaVerse"。
 */
data object MangaVerseContentSource : ContentSource {
	override val name: String = "MANGAVERSE"
	override val locale: String = ""
	override val contentType: ContentType = ContentType.MANGA
}
