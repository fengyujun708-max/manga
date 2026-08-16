package com.mangaverse.app.alternatives.ui

import com.mangaverse.app.core.model.chaptersCount
import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.list.ui.model.ContentGridModel
import com.mangaverse.app.parsers.model.Content

data class ContentAlternativeModel(
	val mangaModel: ContentGridModel,
	private val referenceChapters: Int,
) : ListModel {

	val manga: Content
		get() = mangaModel.manga

	val chaptersCount = manga.chaptersCount()

	val chaptersDiff: Int
		get() = if (referenceChapters == 0 || chaptersCount == 0) 0 else chaptersCount - referenceChapters

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is ContentAlternativeModel && other.manga.id == manga.id
	}

	override fun getChangePayload(previousState: ListModel): Any? = if (previousState is ContentAlternativeModel) {
		mangaModel.getChangePayload(previousState.mangaModel)
	} else {
		null
	}
}
