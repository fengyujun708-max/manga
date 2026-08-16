package com.mangaverse.app.core.parser

import android.net.Uri
import coil3.request.CachePolicy
import dagger.Reusable
import com.mangaverse.app.core.model.ContentSource
import com.mangaverse.app.core.model.UnknownContentSource
import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.core.util.ext.isHttpUrl
import com.mangaverse.app.parsers.ContentLoaderContext
import com.mangaverse.app.parsers.exception.NotFoundException
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.util.almostEquals
import com.mangaverse.app.parsers.util.ifNullOrEmpty
import com.mangaverse.app.parsers.util.levenshteinDistance
import com.mangaverse.app.parsers.util.runCatchingCancellable
import javax.inject.Inject

@Reusable
class ContentLinkResolver @Inject constructor(
	private val repositoryFactory: ContentRepository.Factory,
	private val dataRepository: ContentDataRepository,
	private val context: ContentLoaderContext,
) {

	suspend fun resolve(uri: Uri): Content {
		return if (uri.scheme == "kototoro" || uri.host == "kototoro.app") {
			resolveAppLink(uri)
		} else {
			resolveExternalLink(uri.toString())
		} ?: throw NotFoundException("Cannot resolve link", uri.toString())
	}

	private suspend fun resolveAppLink(uri: Uri): Content? {
		require(uri.pathSegments.singleOrNull() == "manga") { "Invalid url" }
		uri.getQueryParameter("id")?.let { mangaId ->
			// short url
			return dataRepository.findPreferredLocalContentById(mangaId.toLong(), withChapters = false)
				?: dataRepository.findContentById(mangaId.toLong(), withChapters = false)
		}
		val sourceName = requireNotNull(uri.getQueryParameter("source")) { "Source is not specified" }
		val source = ContentSource(sourceName)
		require(source != UnknownContentSource) { "Content source $sourceName is not supported" }
		val repo = repositoryFactory.createWithDiagnostics(source).requireAvailableRepository(
			tag = "ContentLinkResolver",
			prefix = "app_link_repository_unavailable",
		) { "Content source $sourceName is not available" }
		return repo.findExact(
			url = uri.getQueryParameter("url"),
			title = uri.getQueryParameter("name"),
		)
	}

	private suspend fun resolveExternalLink(uri: String): Content? {
		dataRepository.findContentByPublicUrl(uri)?.let {
			return it
		}
		return context.newLinkResolver(uri).getContent()
	}

	private suspend fun ContentRepository.findExact(url: String?, title: String?): Content? {
		if (!title.isNullOrEmpty()) {
			val list = getList(0, null, ContentListFilter(query = title))
			if (url != null) {
				list.find { it.url == url }?.let {
					return it
				}
			}
			list.minByOrNull { it.title.levenshteinDistance(title) }
				?.takeIf { it.title.almostEquals(title, 0.2f) }
				?.let { return it }
		}
		val seed = getDetailsNoCache(
			getSeedContent(source, url ?: return null, title),
		)
		return runCatchingCancellable {
			val seedTitle = seed.title.ifEmpty {
				seed.altTitle
			}.ifNullOrEmpty {
				seed.author
			} ?: return@runCatchingCancellable null
			val seedList = getList(0, null, ContentListFilter(query = seedTitle))
			seedList.first { x -> x.url == url }
		}.getOrThrow()
	}

	private suspend fun ContentRepository.getDetailsNoCache(manga: Content): Content = if (this is CachingContentRepository) {
		getDetails(manga, CachePolicy.READ_ONLY)
	} else {
		getDetails(manga)
	}

	private fun getSeedContent(source: ContentSource, url: String, title: String?) = Content(
		id = run {
			var h = 1125899906842597L
			source.name.forEach { c ->
				h = 31 * h + c.code
			}
			url.forEach { c ->
				h = 31 * h + c.code
			}
			h
		},
		title = title.orEmpty(),
		altTitle = null,
		url = url,
		publicUrl = "",
		rating = 0.0f,
		isNsfw = source.isNsfw(),
		coverUrl = "",
		tags = emptySet(),
		state = null,
		author = null,
		largeCoverUrl = null,
		description = null,
		chapters = null,
		source = source,
	)

	companion object {

		fun isValidLink(str: String): Boolean {
			return str.isHttpUrl() || str.startsWith("kototoro://", ignoreCase = true)
		}
	}
}
