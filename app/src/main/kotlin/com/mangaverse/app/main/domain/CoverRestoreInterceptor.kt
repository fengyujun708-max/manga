package com.mangaverse.app.main.domain

import androidx.collection.ArraySet
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import com.mangaverse.app.bookmarks.domain.Bookmark
import com.mangaverse.app.bookmarks.domain.BookmarksRepository
import com.mangaverse.app.core.model.isLocal
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.util.ext.bookmarkKey
import com.mangaverse.app.core.util.ext.mangaKey
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.util.findById
import com.mangaverse.app.parsers.util.ifNullOrEmpty
import com.mangaverse.app.parsers.util.runCatchingCancellable
import java.util.Collections
import javax.inject.Inject

class CoverRestoreInterceptor @Inject constructor(
	private val dataRepository: ContentDataRepository,
	private val bookmarksRepository: BookmarksRepository,
	private val repositoryFactory: ContentRepository.Factory,
) : Interceptor {

	private val blacklist = Collections.synchronizedSet(ArraySet<String>())

	override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
		val request = chain.request
		val result = chain.proceed()
		if (result is ErrorResult && result.throwable.shouldRestore()) {
			request.extras[bookmarkKey]?.let {
				return if (restoreBookmark(it)) {
					chain.withRequest(request.newBuilder().build()).proceed()
				} else {
					result
				}
			}
			request.extras[mangaKey]?.let {
				return if (restoreContent(it)) {
					chain.withRequest(request.newBuilder().build()).proceed()
				} else {
					result
				}
			}
		}
		return result
	}

	private suspend fun restoreContent(manga: Content): Boolean {
		val key = manga.publicUrl
		if (!blacklist.add(key)) {
			return false
		}
		val restored = runCatchingCancellable {
			restoreContentImpl(manga)
		}.onFailure { e ->
			e.printStackTraceDebug()
		}.getOrDefault(false)
		if (restored) {
			blacklist.remove(key)
		}
		return restored
	}

	private suspend fun restoreContentImpl(manga: Content): Boolean {
		val currentContent = dataRepository.findPreferredLocalContentById(manga.id, withChapters = false)
			?: dataRepository.findContentById(manga.id, withChapters = false)
		if (currentContent == null || manga.isLocal) {
			return false
		}
		val repo = repositoryFactory.create(currentContent.source)
		val fixed = repo.find(currentContent) ?: return false
		return if (fixed != currentContent) {
			val stored = dataRepository.storeContentAndReturn(fixed, replaceExisting = true)
			stored.coverUrl != currentContent.coverUrl
		} else {
			false
		}
	}

	private suspend fun restoreBookmark(bookmark: Bookmark): Boolean {
		val key = bookmark.imageUrl
		if (!blacklist.add(key)) {
			return false
		}
		val restored = runCatchingCancellable {
			restoreBookmarkImpl(bookmark)
		}.onFailure { e ->
			e.printStackTraceDebug()
		}.getOrDefault(false)
		if (restored) {
			blacklist.remove(key)
		}
		return restored
	}

	private suspend fun restoreBookmarkImpl(bookmark: Bookmark): Boolean {
		if (bookmark.manga.isLocal) {
			return false
		}
		val repo = repositoryFactory.create(bookmark.manga.source)
		val chapter = repo.getDetails(bookmark.manga).chapters?.findById(bookmark.chapterId) ?: return false
		val page = repo.getPages(chapter)[bookmark.page]
		val imageUrl = page.preview.ifNullOrEmpty { page.url }
		return if (imageUrl != bookmark.imageUrl) {
			bookmarksRepository.updateBookmark(bookmark, imageUrl)
			true
		} else {
			false
		}
	}

	private fun Throwable.shouldRestore(): Boolean {
		return this is Exception // any Exception but not Error
	}
}
