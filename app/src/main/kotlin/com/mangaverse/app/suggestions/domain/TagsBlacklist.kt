package com.mangaverse.app.suggestions.domain

import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentTag
import com.mangaverse.app.parsers.util.almostEquals

class TagsBlacklist(
	private val tags: Set<String>,
	private val threshold: Float,
) {

	fun isNotEmpty() = tags.isNotEmpty()

	operator fun contains(manga: Content): Boolean {
		if (tags.isEmpty()) {
			return false
		}
		for (mangaTag in manga.tags) {
			for (tagTitle in tags) {
				if (mangaTag.title.almostEquals(tagTitle, threshold)) {
					return true
				}
			}
		}
		return false
	}

	operator fun contains(tag: ContentTag): Boolean = tags.any {
		it.almostEquals(tag.title, threshold)
	}
}
