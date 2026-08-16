@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.mangaverse.app.core.parser.tsuki

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import com.mangaverse.app.parsers.ContentLoaderContext
import com.mangaverse.app.parsers.ContentParserAuthProvider
import com.mangaverse.app.parsers.config.ConfigKey
import com.mangaverse.app.parsers.core.AbstractContentParser
import com.mangaverse.app.parsers.model.*
import com.mangaverse.app.parsers.util.LinkResolver

internal class TsukiContentParserAdapter(
    private val delegate: tsuki.MangaParser,
    source: TsukiContentSource,
    context: ContentLoaderContext,
) : AbstractContentParser(context, source) {

    private val tsukiSource get() = (source as TsukiContentSource).delegate

    override val availableSortOrders: Set<SortOrder> = delegate.availableSortOrders.mapTo(mutableSetOf()) { it.toKototoro() }
    override val filterCapabilities: ContentListFilterCapabilities get() = delegate.filterCapabilities.toKototoro()
    override val configKeyDomain: ConfigKey.Domain get() = delegate.configKeyDomain.toKototoro() as ConfigKey.Domain
    override val authorizationProvider: ContentParserAuthProvider? =
        (delegate as? tsuki.MangaParserAuthProvider)?.let { TsukiAuthProviderAdapter(it) }

    override suspend fun getList(offset: Int, order: SortOrder, filter: ContentListFilter): List<Content> =
        withTsukiExceptions(source) {
            delegate.getList(offset, order.toTsuki(), filter.toTsuki(tsukiSource)).map { it.toKototoro(source) }
        }

    override suspend fun getDetails(manga: Content): Content =
        withTsukiExceptions(source) { delegate.getDetails(manga.toTsuki(tsukiSource)).toKototoro(source) }

    override suspend fun getPages(chapter: ContentChapter): List<ContentPage> =
        withTsukiExceptions(source) {
            delegate.getPages(chapter.toTsuki(tsukiSource)).map { it.toKototoro(source) }
        }

    override suspend fun getPageUrl(page: ContentPage): String =
        withTsukiExceptions(source) { delegate.getPageUrl(page.toTsuki(tsukiSource)) }

    override suspend fun getFilterOptions(): ContentListFilterOptions =
        withTsukiExceptions(source) { delegate.getFilterOptions().toKototoro(source) }

    override suspend fun getFavicons(): Favicons = withTsukiExceptions(source) { delegate.getFavicons().toKototoro() }

    override suspend fun getRelatedContent(seed: Content): List<Content> =
        withTsukiExceptions(source) {
            delegate.getRelatedManga(seed.toTsuki(tsukiSource)).map { it.toKototoro(source) }
        }

    override fun getRequestHeaders() = delegate.getRequestHeaders()

    override fun onCreateConfig(keys: MutableCollection<ConfigKey<*>>) {
        val tsukiKeys = mutableListOf<tsuki.config.ConfigKey<*>>()
        delegate.onCreateConfig(tsukiKeys)
        keys += tsukiKeys.map { it.toKototoro() }
    }

    override suspend fun resolveLink(resolver: LinkResolver, link: HttpUrl): Content? =
        withTsukiExceptions(source) { delegate.resolveLink(link)?.toKototoro(source) }

    override fun intercept(chain: Interceptor.Chain): Response = delegate.intercept(chain)
}

private class TsukiAuthProviderAdapter(
    private val delegate: tsuki.MangaParserAuthProvider,
) : ContentParserAuthProvider {
    override val authUrl: String get() = delegate.authUrl
    override suspend fun isAuthorized(): Boolean = delegate.isAuthorized()
    override suspend fun getUsername(): String = delegate.getUsername().orEmpty()
}

private suspend inline fun <T> withTsukiExceptions(source: ContentSource, block: suspend () -> T): T {
    try {
        return block()
    } catch (e: tsuki.exception.AuthRequiredException) {
        throw com.mangaverse.app.parsers.exception.AuthRequiredException(source, e)
    } catch (e: tsuki.exception.NotFoundException) {
        throw com.mangaverse.app.parsers.exception.NotFoundException(e.message.orEmpty(), e.url)
    } catch (e: tsuki.exception.ContentUnavailableException) {
        throw com.mangaverse.app.parsers.exception.ContentUnavailableException(e.message.orEmpty())
    } catch (e: tsuki.exception.ParseException) {
        throw com.mangaverse.app.parsers.exception.ParseException(e.shortMessage, e.url, e)
    } catch (e: tsuki.exception.TooManyRequestExceptions) {
        throw com.mangaverse.app.parsers.exception.TooManyRequestExceptions(e.url, e.getRetryDelay())
    }
}
