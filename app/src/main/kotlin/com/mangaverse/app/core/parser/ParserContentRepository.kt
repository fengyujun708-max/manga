package com.mangaverse.app.core.parser

import kotlinx.coroutines.Dispatchers
import okhttp3.Interceptor
import okhttp3.Response
import com.mangaverse.app.core.cache.MemoryContentCache
import com.mangaverse.app.core.exceptions.CloudFlareProtectedException
import com.mangaverse.app.core.exceptions.InteractiveActionRequiredException
import com.mangaverse.app.core.exceptions.ProxyConfigException
import com.mangaverse.app.core.prefs.SourceSettings
import com.mangaverse.app.parsers.ContentParser
import com.mangaverse.app.parsers.CategorizedFavoritesProvider
import com.mangaverse.app.parsers.FavoritesProvider
import com.mangaverse.app.parsers.FavoritesSyncProvider
import com.mangaverse.app.parsers.ContentParserAuthProvider
import com.mangaverse.app.parsers.config.ConfigKey
import com.mangaverse.app.parsers.exception.AuthRequiredException
import com.mangaverse.app.parsers.model.Favicons
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentListFilterCapabilities
import com.mangaverse.app.parsers.model.ContentListFilterOptions
import com.mangaverse.app.parsers.model.ContentPage
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.SortOrder
import com.mangaverse.app.parsers.util.runCatchingCancellable
import com.mangaverse.app.parsers.util.suspendlazy.suspendLazy

class ParserContentRepository(
	private val parser: ContentParser,
	private val mirrorSwitcher: MirrorSwitcher,
	cache: MemoryContentCache,
) : CachingContentRepository(cache), Interceptor {

	private val filterOptionsLazy = suspendLazy(Dispatchers.Default) {
		withMirrors {
			parser.getFilterOptions()
		}
	}

	override val source: ContentSource
		get() = parser.source

	override val sortOrders: Set<SortOrder>
		get() = parser.availableSortOrders

	override val filterCapabilities: ContentListFilterCapabilities
		get() = parser.filterCapabilities

	override var defaultSortOrder: SortOrder
		get() = getConfig().defaultSortOrder ?: sortOrders.first()
		set(value) {
			getConfig().defaultSortOrder = value
		}

	var domain: String
		get() = parser.domain
		set(value) {
			getConfig()[parser.configKeyDomain] = value
		}

	val domains: Array<out String>
		get() = parser.configKeyDomain.presetValues

	override fun intercept(chain: Interceptor.Chain): Response = parser.intercept(chain)

	override suspend fun getList(offset: Int, order: SortOrder?, filter: ContentListFilter?): List<Content> {
		return withMirrors {
			parser.getList(offset, order ?: defaultSortOrder, filter ?: ContentListFilter.EMPTY)
		}
	}

	override suspend fun getPagesImpl(
		chapter: ContentChapter,
		nextChapterUrl: String?
	): List<ContentPage> = withMirrors {
		parser.getPages(chapter)
	}

	override suspend fun getPageUrl(page: ContentPage): String = withMirrors {
		parser.getPageUrl(page).also { result ->
			check(result.isNotEmpty()) { "Page url is empty" }
		}
	}

	override suspend fun getChapterContent(chapter: ContentChapter, nextChapterUrl: String?): com.mangaverse.app.parsers.model.NovelChapterContent? {
		return runCatching {
			withMirrors { parser.getChapterContent(chapter) ?: throw IllegalStateException("Chapter content is null") }
		}.getOrNull()
	}

	override suspend fun getFilterOptions(): ContentListFilterOptions = filterOptionsLazy.get()

	suspend fun getFavicons(): Favicons = withMirrors {
		parser.getFavicons()
	}

	override suspend fun getRelatedContentImpl(seed: Content): List<Content> = parser.getRelatedContent(seed)

	override suspend fun getDetailsImpl(manga: Content): Content = withMirrors {
		parser.getDetails(manga)
	}

	fun getAuthProvider(): ContentParserAuthProvider? = parser.authorizationProvider

	override fun getRequestHeaders(): Map<String, String> {
		val headers = parser.getRequestHeaders()
		val map = mutableMapOf<String, String>()
		for (i in 0 until headers.size) {
			map[headers.name(i)] = headers.value(i)
		}
		if (map["Referer"] == null) {
			val idn = java.net.IDN.toASCII(parser.domain)
			map["Referer"] = "https://$idn/"
		}
		return map
	}

	override fun createPageRequest(pageUrl: String, page: ContentPage): okhttp3.Request {
		return com.mangaverse.app.reader.domain.PageLoader.createPageRequest(pageUrl, page)
	}

	fun favoritesProvider(): FavoritesProvider? =
		resolveFavoritesProvider(parser)

	fun favoritesSyncProvider(): FavoritesSyncProvider? =
		resolveFavoritesSyncProvider(parser)

	fun categorizedFavoritesProvider(): CategorizedFavoritesProvider? =
		resolveCategorizedFavoritesProvider(parser)

	private fun allFields(clazz: Class<*>): Sequence<java.lang.reflect.Field> = sequence {
		var current: Class<*>? = clazz
		while (current != null && current != Any::class.java) {
			current.declaredFields.forEach { yield(it) }
			current = current.superclass
		}
	}

	private fun resolveFavoritesProvider(p: ContentParser, visited: MutableSet<Any> = mutableSetOf()): FavoritesProvider? {
		if (p in visited) return null
		visited += p
		if (p is FavoritesProvider) return p
		// 尝试通过反射抓取包装器中的字段
		for (field in allFields(p.javaClass)) {
			field.isAccessible = true
			val value = runCatching { field.get(p) }.getOrNull() ?: continue
			if (value is FavoritesProvider) return value
			if (value is ContentParser) {
				resolveFavoritesProvider(value, visited)?.let { return it }
			}
		}
		return null
	}

	private fun resolveFavoritesSyncProvider(p: ContentParser, visited: MutableSet<Any> = mutableSetOf()): FavoritesSyncProvider? {
		if (p in visited) return null
		visited += p
		if (p is FavoritesSyncProvider) return p
		for (field in allFields(p.javaClass)) {
			field.isAccessible = true
			val value = runCatching { field.get(p) }.getOrNull() ?: continue
			if (value is FavoritesSyncProvider) return value
			if (value is ContentParser) {
				resolveFavoritesSyncProvider(value, visited)?.let { return it }
			}
		}
		return null
	}

	private fun resolveCategorizedFavoritesProvider(p: ContentParser, visited: MutableSet<Any> = mutableSetOf()): CategorizedFavoritesProvider? {
		if (p in visited) return null
		visited += p
		if (p is CategorizedFavoritesProvider) return p
		for (field in allFields(p.javaClass)) {
			field.isAccessible = true
			val value = runCatching { field.get(p) }.getOrNull() ?: continue
			if (value is CategorizedFavoritesProvider) return value
			if (value is ContentParser) {
				resolveCategorizedFavoritesProvider(value, visited)?.let { return it }
			}
		}
		return null
	}

	override suspend fun getConfigKeys(): List<ConfigKey<*>> = ArrayList<ConfigKey<*>>().also {
		parser.onCreateConfig(it)
	}

	fun getAvailableMirrors(): List<String> {
		return parser.configKeyDomain.presetValues.toList()
	}

	override fun isSlowdownEnabled(): Boolean {
		return getConfig().isSlowdownEnabled
	}

	fun getConfig() = parser.config as SourceSettings

	private suspend fun <T : Any> withMirrors(block: suspend () -> T): T {
		if (!mirrorSwitcher.isEnabled) {
			return block()
		}
		val initialResult = runCatchingCancellable { block() }
		if (initialResult.isValidResult()) {
			return initialResult.getOrThrow()
		}
		val newResult = mirrorSwitcher.trySwitchMirror(this, block)
		return newResult ?: initialResult.getOrThrow()
	}

	private fun Result<Any>.isValidResult() = fold(
		onSuccess = {
			when (it) {
				is Collection<*> -> it.isNotEmpty()
				else -> true
			}
		},
		onFailure = {
			when (it.cause) {
				is CloudFlareProtectedException,
				is AuthRequiredException,
				is InteractiveActionRequiredException,
				is ProxyConfigException -> true

				else -> false
			}
		},
	)
}
