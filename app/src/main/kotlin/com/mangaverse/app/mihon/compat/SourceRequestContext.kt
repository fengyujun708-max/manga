package com.mangaverse.app.mihon.compat

import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.mangaverse.app.mihon.model.MihonMangaSource
import com.mangaverse.app.parsers.model.ContentSource

/** Immutable source authority attached to a single Mihon HTTP request. */
data class SourceRequestContext(
    val source: ContentSource,
    val allowedBrowserOrigins: Set<String> = emptySet(),
) {
	fun allowsBrowserRequest(url: String): Boolean = url.toHttpsOrigin() in allowedBrowserOrigins

    companion object {
        fun from(source: ContentSource): SourceRequestContext {
            val baseOrigin = ((source as? MihonMangaSource)?.catalogueSource as? HttpSource)
                ?.baseUrl
                ?.toHttpsOrigin()
            return SourceRequestContext(
                source = source,
                allowedBrowserOrigins = baseOrigin?.let(::setOf).orEmpty(),
            )
        }

		fun from(source: ContentSource, declaredBaseUrl: String): SourceRequestContext = SourceRequestContext(
			source = source,
			allowedBrowserOrigins = declaredBaseUrl.toHttpsOrigin()?.let(::setOf).orEmpty(),
		)
    }
}

private fun String.toHttpsOrigin(): String? {
    val url = toHttpUrlOrNull()?.takeIf { it.scheme == "https" } ?: return null
    return buildString {
        append(url.scheme)
        append("://")
        append(url.host)
        if (url.port != HttpUrl.defaultPort(url.scheme)) {
            append(':')
            append(url.port)
        }
    }
}
