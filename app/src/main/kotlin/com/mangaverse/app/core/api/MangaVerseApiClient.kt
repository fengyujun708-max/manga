package com.mangaverse.app.core.api

import com.mangaverse.app.core.api.model.ApiAccountInfo
import com.mangaverse.app.core.api.model.ApiAdEntitlement
import com.mangaverse.app.core.api.model.ApiAuthResponse
import com.mangaverse.app.core.api.model.ApiHomePageData
import com.mangaverse.app.core.api.model.ApiHotListResponse
import com.mangaverse.app.core.api.model.ApiLoginRequest
import com.mangaverse.app.core.api.model.ApiMangaWithRoutes
import com.mangaverse.app.core.api.model.ApiRegisterRequest
import com.mangaverse.app.core.api.model.ApiSearchResponse
import com.mangaverse.app.core.api.model.ApiUserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

/**
 * MangaVerse API 客户端。
 *
 * 基于 OkHttp + kotlinx.serialization，提供对 MangaVerse 后端
 * （auth / home / manga / search / user / ads）的访问能力。
 */
class MangaVerseApiClient(
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val json: Json,
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun login(username: String, password: String): ApiAuthResponse =
        post(
            "/api/auth/login",
            ApiLoginRequest(username, password),
            ApiLoginRequest.serializer(),
            ApiAuthResponse.serializer(),
        )

    suspend fun register(username: String, password: String): ApiAuthResponse =
        post(
            "/api/auth/register",
            ApiRegisterRequest(username, password),
            ApiRegisterRequest.serializer(),
            ApiAuthResponse.serializer(),
        )

    suspend fun me(token: String): ApiAccountInfo =
        get("/api/auth/me", ApiAccountInfo.serializer()) { builder ->
            builder.addQueryParameter("token", token)
        }

    suspend fun getHomepage(userId: String): ApiHomePageData =
        get("/api/home", ApiHomePageData.serializer()) { builder ->
            builder.addQueryParameter("user_id", userId)
        }

    suspend fun getHotList(category: String = "all", limit: Int = 20): ApiHotListResponse =
        get("/api/home/hot", ApiHotListResponse.serializer()) { builder ->
            builder.addQueryParameter("category", category)
            builder.addQueryParameter("limit", limit.toString())
        }

    suspend fun search(query: String, category: String? = null, page: Int = 1): ApiSearchResponse =
        get("/api/search", ApiSearchResponse.serializer()) { builder ->
            builder.addQueryParameter("q", query)
            category?.let { builder.addQueryParameter("category", it) }
            builder.addQueryParameter("page", page.toString())
        }

    suspend fun getMangaDetail(sourceId: String, sourceMangaId: String): ApiMangaWithRoutes =
        get("/api/manga/$sourceId/detail", ApiMangaWithRoutes.serializer()) { builder ->
            builder.addQueryParameter("source_manga_id", sourceMangaId)
        }

    suspend fun getMangaRoutes(sourceId: String, sourceMangaId: String): List<com.mangaverse.app.core.api.model.ApiRoute> =
        get(
            "/api/manga/$sourceId/routes",
            kotlinx.serialization.builtins.ListSerializer(com.mangaverse.app.core.api.model.ApiRoute.serializer()),
        ) { builder ->
            builder.addQueryParameter("source_manga_id", sourceMangaId)
        }

    suspend fun getChapters(
        sourceId: String,
        sourceMangaId: String,
    ): List<com.mangaverse.app.core.api.model.ApiChapter> =
        get(
            "/api/manga/$sourceId/chapters",
            kotlinx.serialization.builtins.ListSerializer(com.mangaverse.app.core.api.model.ApiChapter.serializer()),
        ) { builder ->
            builder.addQueryParameter("source_manga_id", sourceMangaId)
        }

    suspend fun getChapterPages(
        sourceId: String,
        sourceMangaId: String,
        chapterId: String,
    ): List<String> =
        get(
            "/api/manga/$sourceId/chapters/$chapterId/pages",
            kotlinx.serialization.builtins.ListSerializer(String.serializer()),
        ) { builder ->
            builder.addQueryParameter("source_manga_id", sourceMangaId)
        }

    suspend fun getUserInfo(userId: String): ApiUserInfo =
        get("/api/user/$userId", ApiUserInfo.serializer())

    suspend fun getAdEntitlement(userId: String): ApiAdEntitlement =
        get("/api/ads/entitlement", ApiAdEntitlement.serializer()) { builder ->
            builder.addQueryParameter("user_id", userId)
        }

    suspend fun getUniverseServers(): com.mangaverse.app.core.api.model.ApiUniverseResponse =
        get(
            "/api/universe/servers",
            com.mangaverse.app.core.api.model.ApiUniverseResponse.serializer(),
        )

    private suspend fun <T> get(
        path: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        configure: (okhttp3.HttpUrl.Builder) -> Unit = {},
    ): T = withContext(Dispatchers.IO) {
        val base = baseUrl.toHttpUrlOrNull()
            ?: throw IOException("Invalid MangaVerse API base URL: $baseUrl")
        val urlBuilder = base.newBuilder().apply { addPathSegments(path.trimStart('/')) }
        configure(urlBuilder)
        val request = Request.Builder()
            .url(urlBuilder.build())
            .get()
            .build()
        execute(request, serializer)
    }

    private suspend fun <B, T> post(
        path: String,
        body: B,
        bodySerializer: kotlinx.serialization.KSerializer<B>,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T = withContext(Dispatchers.IO) {
        val base = baseUrl.toHttpUrlOrNull()
            ?: throw IOException("Invalid MangaVerse API base URL: $baseUrl")
        val url = base.newBuilder().addPathSegments(path.trimStart('/')).build()
        val bodyBytes = json.encodeToString(bodySerializer, body)
        val request = Request.Builder()
            .url(url)
            .post(bodyBytes.toRequestBody(jsonMediaType))
            .build()
        execute(request, serializer)
    }

    private fun <T> execute(
        request: Request,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T {
        client.newCall(request).execute().use { response: Response ->
            if (!response.isSuccessful) {
                throw IOException("MangaVerse API error: HTTP ${response.code} for ${request.url}")
            }
            val text = response.body?.string() ?: throw IOException("Empty response body")
            return json.decodeFromString(serializer, text)
        }
    }
}
