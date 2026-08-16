package com.mangaverse.app.mangaupdate.domain

import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import com.mangaverse.app.list.domain.ListFilterOption
import com.mangaverse.app.tracker.domain.TrackingRepository
import com.mangaverse.app.tracker.domain.model.ContentTracking
import javax.inject.Inject

/**
 * MangaVerse 本地漫画更新仓库。
 *
 * 提供"追更/更新"数据：哪些漫画有新章节、新章节数量、最近更新列表。
 * 数据源为本地 tracks 表，不依赖任何外部追踪服务或第三方账号。
 */
@Reusable
class MangaUpdateRepository @Inject constructor(
	private val delegate: TrackingRepository,
) {

	fun observeUpdatedContentCount(): Flow<Int> = delegate.observeUpdatedContentCount()

	fun observeUnreadUpdatesCount(): Flow<Int> = delegate.observeUnreadUpdatesCount()

	fun observeUpdatedContent(limit: Int, filterOptions: Set<ListFilterOption>): Flow<List<ContentTracking>> {
		return delegate.observeUpdatedContent(limit, filterOptions)
	}

	suspend fun getNewChaptersCount(mangaId: Long): Int = delegate.getNewChaptersCount(mangaId)

	suspend fun getNewChaptersCounts(mangaIds: Collection<Long>): Map<Long, Int> {
		return delegate.getNewChaptersCounts(mangaIds)
	}

	fun observeNewChaptersCount(mangaId: Long): Flow<Int> = delegate.observeNewChaptersCount(mangaId)

	suspend fun getTracks(offset: Int, limit: Int): List<ContentTracking> {
		return delegate.getTracks(offset, limit)
	}
}
