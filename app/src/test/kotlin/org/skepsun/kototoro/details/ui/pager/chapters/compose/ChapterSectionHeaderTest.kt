package com.mangaverse.app.details.ui.pager.chapters.compose

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import com.mangaverse.app.list.ui.model.CollapsibleListHeader
import com.mangaverse.app.list.ui.model.ListHeader

class ChapterSectionHeaderTest : StringSpec({

	"regular volume headers are chapter section headers" {
		ListHeader("Volume 1").isChapterSectionHeader() shouldBe true
	}

	"collapsible EPUB headers are chapter section headers" {
		CollapsibleListHeader(
			text = "Book 1",
			isCollapsible = true,
			groupId = "book_1",
		).isChapterSectionHeader() shouldBe true
	}
})
