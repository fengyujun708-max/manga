package com.mangaverse.app.favourites.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import com.mangaverse.app.core.model.toChipModel
import com.mangaverse.app.list.domain.ListFilterOption
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentState
import com.mangaverse.app.parsers.model.ContentTag
import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.scrobbling.common.domain.model.ScrobblingStatus

class FavoritesQuickFilterGroupsTest {

	@Test
	fun `fixed meta filters are grouped while auto filters stay independent`() {
		val tagOption = ListFilterOption.Tag(
			ContentTag(
				title = "Action",
				key = "action",
				source = testSource,
			),
		)
		val options = listOf(
			ListFilterOption.Downloaded,
			ListFilterOption.Macro.NEW_CHAPTERS,
			*ScrobblingStatus.entries.map { ListFilterOption.ReadingStatus(it) }.toTypedArray(),
			ListFilterOption.PublicationState(ContentState.ONGOING),
			ListFilterOption.PublicationState(ContentState.FINISHED),
			ListFilterOption.SFW,
			ListFilterOption.Macro.NSFW,
			ListFilterOption.Macro.MULTI_PROJECTION,
			ListFilterOption.Macro.BROKEN_PROJECTION,
			tagOption,
		)

		val filter = buildFavoritesQuickFilter(options.map { it.toChipModel(isChecked = false) })

		assertEquals(
			listOf(
				"READING_STATUS",
				"PUBLICATION_STATUS",
				"CONTENT_RATING",
				"WORK_RELATIONS",
			),
			filter.groups.map { it.key },
		)
		assertEquals(listOf(6, 2, 2, 2), filter.groups.map { it.items.size })
		assertEquals(3, filter.items.size)
		assertSame(tagOption, filter.items.last().data)
	}

	private val testSource = object : ContentSource {
		override val name = "TEST"
		override val locale = ""
		override val contentType = ContentType.MANGA
	}
}
