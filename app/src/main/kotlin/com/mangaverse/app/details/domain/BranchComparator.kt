package com.mangaverse.app.details.domain

import com.mangaverse.app.core.util.LocaleStringComparator
import com.mangaverse.app.details.ui.model.ContentBranch

class BranchComparator : Comparator<ContentBranch> {

	private val delegate = LocaleStringComparator()

	override fun compare(o1: ContentBranch, o2: ContentBranch): Int = delegate.compare(o1.name, o2.name)
}
