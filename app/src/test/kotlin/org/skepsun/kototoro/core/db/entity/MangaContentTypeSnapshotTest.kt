package com.mangaverse.app.core.db.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import com.mangaverse.app.core.model.TestContentSource
import com.mangaverse.app.core.model.UnknownContentSource
import com.mangaverse.app.core.model.resolvedContentTypeForSnapshot
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentType

class MangaContentTypeSnapshotTest {

	@Test
	fun `content mapping persists resolved source type`() {
		assertEquals(ContentType.MANGA.name, content(TestContentSource).toEntity().contentType)
	}

	@Test
	fun `content mapping leaves unresolved source type null`() {
		assertNull(content(UnknownContentSource).toEntity().contentType)
	}

	@Test
	fun `known external source prefix resolves without installed extension`() {
		assertEquals(
			ContentType.MANGA,
			com.mangaverse.app.core.model.ContentSource("MIHON_123").resolvedContentTypeForSnapshot(),
		)
		assertEquals(
			ContentType.MANGA,
			com.mangaverse.app.core.model.ContentSource("JSON_LEGADO_M_123").resolvedContentTypeForSnapshot(),
		)
		assertNull(
			com.mangaverse.app.core.model.ContentSource("UNAVAILABLE_SOURCE").resolvedContentTypeForSnapshot(),
		)
	}

	private fun content(source: ContentSource) = Content(
		id = 1L,
		title = "Title",
		altTitles = emptySet(),
		url = "/work",
		publicUrl = "https://example.test/work",
		rating = 0f,
		contentRating = null,
		coverUrl = null,
		tags = emptySet(),
		state = null,
		authors = emptySet(),
		source = source,
	)
}
