@file:JvmName("ContentParsersUtils")

package com.mangaverse.app.parsers.util

import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.model.ContentListFilter
import kotlin.contracts.contract

public fun ContentListFilter?.isNullOrEmpty(): Boolean {
	contract {
		returns(false) implies (this@isNullOrEmpty != null)
	}
	return this == null || this.isEmpty()
}

public fun Collection<ContentChapter>.findById(chapterId: Long): ContentChapter? = find { x ->
	x.id == chapterId
}
