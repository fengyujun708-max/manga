package com.mangaverse.app.details.ui

import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentType

/**
 * Test ContentSource for property tests
 */
object TestContentSource : ContentSource {
    override val name: String = "TestSource"
    override val locale: String = "en"
    override val contentType: ContentType = ContentType.MANGA
}
