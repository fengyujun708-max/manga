package com.mangaverse.app.core.api.data

import com.mangaverse.app.core.api.MangaVerseApiClient
import com.mangaverse.app.core.api.model.ApiMangaWithRoutes
import com.mangaverse.app.core.api.model.ApiSearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MangaVerse API 业务仓库。
 *
 * 为客户端 UI 提供对 MangaVerse 后端（认证/首页/搜索/漫画详情/用户/广告）
 * 的访问能力，与后端 /api 路由一一对应。
 */
@Singleton
class MangaVerseRepository @Inject constructor(
    private val apiClient: MangaVerseApiClient,
) {

    suspend fun login(username: String, password: String): Result<com.mangaverse.app.core.api.model.ApiAuthResponse> =
        runCatching {
            apiClient.login(username, password)
        }

    suspend fun register(username: String, password: String): Result<com.mangaverse.app.core.api.model.ApiAuthResponse> =
        runCatching {
            apiClient.register(username, password)
        }

    suspend fun getAccount(token: String): Result<com.mangaverse.app.core.api.model.ApiAccountInfo> = runCatching {
        apiClient.me(token)
    }

    fun observeHomepage(userId: String): Flow<com.mangaverse.app.core.api.model.ApiHomePageData> = flow {
        emit(apiClient.getHomepage(userId))
    }.flowOn(Dispatchers.IO)

    suspend fun getHotList(category: String = "all", limit: Int = 20): Result<List<ApiMangaWithRoutes>> = runCatching {
        apiClient.getHotList(category, limit).mangaList
    }

    suspend fun search(
        query: String,
        category: String? = null,
        page: Int = 1,
    ): Result<ApiSearchResponse> = runCatching {
        apiClient.search(query, category, page)
    }

    suspend fun getMangaDetail(sourceId: String, sourceMangaId: String): Result<ApiMangaWithRoutes> = runCatching {
        apiClient.getMangaDetail(sourceId, sourceMangaId)
    }

    suspend fun getChapters(sourceId: String, sourceMangaId: String): Result<List<com.mangaverse.app.core.api.model.ApiChapter>> = runCatching {
        apiClient.getChapters(sourceId, sourceMangaId)
    }

    suspend fun getChapterPages(
        sourceId: String,
        sourceMangaId: String,
        chapterId: String,
    ): Result<List<String>> = runCatching {
        apiClient.getChapterPages(sourceId, sourceMangaId, chapterId)
    }

    suspend fun getUserInfo(userId: String): Result<com.mangaverse.app.core.api.model.ApiUserInfo> = runCatching {
        apiClient.getUserInfo(userId)
    }

    suspend fun getAdEntitlement(userId: String): Result<com.mangaverse.app.core.api.model.ApiAdEntitlement> = runCatching {
        apiClient.getAdEntitlement(userId)
    }

    suspend fun getUniverseServers(): Result<com.mangaverse.app.core.api.model.ApiUniverseResponse> = runCatching {
        apiClient.getUniverseServers()
    }
}
