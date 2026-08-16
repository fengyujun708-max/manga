package com.mangaverse.app.history.data

import dagger.Reusable
import com.mangaverse.app.history.domain.model.ContentWithHistory
import com.mangaverse.app.local.data.index.LocalContentIndex
import com.mangaverse.app.local.domain.LocalObserveMapper
import kotlinx.coroutines.flow.Flow
import com.mangaverse.app.parsers.model.Content
import javax.inject.Inject

@Reusable
class HistoryLocalObserver @Inject constructor(
	localContentIndex: LocalContentIndex,
) : LocalObserveMapper<ContentWithHistory, ContentWithHistory>(localContentIndex) {

	fun observe(source: Flow<Collection<ContentWithHistory>>) = source.mapToLocal()

	override fun toContent(e: ContentWithHistory) = e.manga

	override fun toResult(e: ContentWithHistory, manga: Content) = ContentWithHistory(
		manga = manga,
		history = e.history,
		entityId = e.entityId,
		preferredLocalMangaId = e.preferredLocalMangaId,
	)
}
