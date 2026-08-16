package com.mangaverse.app.suggestions.domain

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.db.entity.toEntities
import com.mangaverse.app.core.db.entity.toEntity
import com.mangaverse.app.core.db.entity.toContent
import com.mangaverse.app.core.db.entity.toContentTagsList
import com.mangaverse.app.core.model.toContentSources
import com.mangaverse.app.core.util.ext.mapItems
import com.mangaverse.app.list.domain.ListFilterOption
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentTag
import com.mangaverse.app.suggestions.data.SuggestionEntity
import com.mangaverse.app.suggestions.data.SuggestionWithContent
import javax.inject.Inject

class SuggestionRepository @Inject constructor(
	private val db: MangaDatabase,
) {

	fun observeAll(): Flow<List<Content>> {
		return db.getSuggestionDao().observeAll().mapItems {
			it.toContent()
		}
	}

	fun observeAll(limit: Int, filterOptions: Set<ListFilterOption>): Flow<List<Content>> {
		return db.getSuggestionDao().observeAll(limit, filterOptions).mapItems {
			it.toContent()
		}
	}

	fun observeCount(): Flow<Int> {
		return db.getSuggestionDao().observeCount()
	}

	suspend fun getRandomList(limit: Int): List<Content> {
		return db.getSuggestionDao().getRandom(limit).map {
			it.toContent()
		}
	}

	suspend fun clear() {
		db.getSuggestionDao().deleteAll()
	}

	suspend fun isEmpty(): Boolean {
		return db.getSuggestionDao().count() == 0
	}

	suspend fun getTopTags(limit: Int): List<ContentTag> {
		return db.getSuggestionDao().getTopTags(limit)
			.toContentTagsList()
	}

	suspend fun getTopSources(limit: Int): List<ContentSource> {
		return db.getSuggestionDao().getTopSources(limit)
			.toContentSources()
	}

	suspend fun replace(suggestions: Iterable<ContentSuggestion>) {
		db.withTransaction {
			db.getSuggestionDao().deleteAll()
			suggestions.forEach { (manga, relevance) ->
				val tags = manga.tags.toEntities()
				db.getTagsDao().upsert(tags)
				db.getMangaDao().upsert(manga.toEntity(), tags)
				db.getSuggestionDao().upsert(
					SuggestionEntity(
						mangaId = manga.id,
						relevance = relevance,
						createdAt = System.currentTimeMillis(),
					),
				)
			}
		}
	}

	private fun SuggestionWithContent.toContent() = manga.toContent(emptySet(), null)
}
