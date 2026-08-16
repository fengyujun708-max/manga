package com.mangaverse.app.core.parser

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.mangaverse.app.core.cache.MemoryContentCache
import com.mangaverse.app.core.db.entity.JsonSourceType
import com.mangaverse.app.core.javascript.BrowserLauncher
import com.mangaverse.app.core.javascript.JavaScriptEngine
import com.mangaverse.app.core.jsonsource.JsonContentSource
import com.mangaverse.app.core.network.jsonsource.LegadoHttpClient
import com.mangaverse.app.core.network.jsonsource.PersistentCookieJar
import com.mangaverse.app.core.parser.legado.LegadoRepository
import com.mangaverse.app.parsers.model.ContentSource
import javax.inject.Inject

class JsonContentRepositoryProvider @Inject constructor(
	@ApplicationContext private val context: Context,
	private val contentCache: MemoryContentCache,
	private val legadoHttpClient: LegadoHttpClient,
	private val jsEngine: JavaScriptEngine,
	private val loaderContext: ContentLoaderContextImpl,
) : ContentRepositoryProvider {

	override fun supports(source: ContentSource): Boolean = source is JsonContentSource

	override fun create(source: ContentSource): ContentRepository? {
		if (source !is JsonContentSource) return null
		return when (source.entity.type) {
			JsonSourceType.LEGADO -> {
				val browserLauncher = BrowserLauncher(
					context = context,
					cookieJar = PersistentCookieJar(legadoHttpClient.getCookieJar()),
				)
				val legadoPrefs = context.getSharedPreferences("legado_source_store", Context.MODE_PRIVATE)
				LegadoRepository(
					source = source,
					httpClient = legadoHttpClient,
					jsEngine = jsEngine,
					memoryCache = contentCache,
					browserLauncher = browserLauncher,
					legadoPrefs = legadoPrefs,
				)
			}
			JsonSourceType.JS -> JsContentRepository(source, loaderContext)
		}
	}
}
