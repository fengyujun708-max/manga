package com.mangaverse.app.details.ui

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import com.mangaverse.app.core.model.ContentSource
import com.mangaverse.app.core.model.ContentSourceInfo
import com.mangaverse.app.parsers.model.ContentType

class DetailsSourceResolutionTest {

	@Test
	fun `enabled jar source replaces an anonymous restored source`() {
		val restored = ContentSource("JAR_MANGA")
		val loaded = TestSource("JAR_MANGA", ContentType.MANGA, "zh")

		val result = selectResolvedDetailsSource(
			original = restored,
			enabledSources = listOf(ContentSourceInfo(loaded, isEnabled = true, isPinned = false)),
			pipelineResolved = restored,
		)

		assertSame(loaded, result)
	}

	@Test
	fun `pipeline jar source replaces an anonymous restored source`() {
		val restored = ContentSource("JAR_MANGA")
		val loaded = TestSource("JAR_MANGA", ContentType.MANGA, "zh")

		val result = selectResolvedDetailsSource(
			original = restored,
			enabledSources = emptyList(),
			pipelineResolved = loaded,
		)

		assertSame(loaded, result)
	}

	private data class TestSource(
		override val name: String,
		override val contentType: ContentType,
		override val locale: String,
	) : com.mangaverse.app.parsers.model.ContentSource
}
