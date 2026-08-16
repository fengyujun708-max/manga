package com.mangaverse.app.settings.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.mangaverse.app.core.jsonsource.SourceType
import com.mangaverse.app.parsers.model.ContentType

class SuggestionSourceFilterTest {

    private val options = listOf(
        SuggestionSourceOption("MANGA_NATIVE", "Manga Native", ContentType.MANGA, SourceType.NATIVE),
        SuggestionSourceOption("JSON_LEGADO_BOOK", "Novel Legado", ContentType.NOVEL, SourceType.JSON_LEGADO),
        SuggestionSourceOption("EXTERNAL_VIDEO", "Video External", ContentType.VIDEO, SourceType.EXTERNAL),
    )

    @Test
    fun `empty filters include every source`() {
        assertEquals(options, filterSuggestionSourceOptions(options, "", emptySet(), emptySet()))
    }

    @Test
    fun `content and source type filters are combined`() {
        assertEquals(
            listOf(options[1]),
            filterSuggestionSourceOptions(
                options = options,
                query = "",
                contentTypes = setOf(ContentType.NOVEL, ContentType.VIDEO),
                sourceTypes = setOf(SourceType.JSON_LEGADO),
            ),
        )
    }

    @Test
    fun `query is combined with type filters and ignores case`() {
        assertEquals(
            listOf(options[2]),
            filterSuggestionSourceOptions(
                options = options,
                query = "video",
                contentTypes = setOf(ContentType.VIDEO),
                sourceTypes = emptySet(),
            ),
        )
    }
}
