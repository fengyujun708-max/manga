package com.mangaverse.app.core.parser.tsuki

import com.mangaverse.app.parsers.model.ContentSource

internal data class TsukiContentSource(
    val delegate: tsuki.model.MangaSource,
) : ContentSource {
    override val name: String get() = delegate.name
    val title: String get() = delegate.title
    override val locale: String get() = delegate.locale
    override val contentType get() = delegate.contentType.toKototoro()
}
