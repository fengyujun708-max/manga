package com.mangaverse.app.core.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiManga(
    @SerialName("id") val id: String? = null,
    @SerialName("title") val title: String,
    @SerialName("title_alt") val titleAlt: String? = null,
    @SerialName("author") val author: String? = null,
    @SerialName("artist") val artist: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("cover_hash") val coverHash: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("genres") val genres: List<String> = emptyList(),
    @SerialName("language") val language: String? = null,
    @SerialName("heat_score") val heatScore: Float = 0f,
    @SerialName("rating_plot") val ratingPlot: Float = 0f,
    @SerialName("rating_art") val ratingArt: Float = 0f,
    @SerialName("rating_update") val ratingUpdate: Float = 0f,
    @SerialName("chapter_count") val chapterCount: Int = 0,
    @SerialName("last_updated") val lastUpdated: String? = null,
    @SerialName("match_sources") val matchSources: List<String> = emptyList(),
)

@Serializable
data class ApiRoute(
    @SerialName("route_number") val routeNumber: Int = 1,
    @SerialName("source_id") val sourceId: String = "",
    @SerialName("source_language") val sourceLanguage: String? = null,
    @SerialName("source_category") val sourceCategory: String? = null,
    @SerialName("chapter_count") val chapterCount: Int = 0,
    @SerialName("last_updated") val lastUpdated: String? = null,
    @SerialName("update_speed") val updateSpeed: String = "normal",
    @SerialName("completeness") val completeness: Float = 0f,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("source_manga_url") val sourceMangaUrl: String = "",
    @SerialName("source_manga_id") val sourceMangaId: String = "",
)

@Serializable
data class ApiMangaWithRoutes(
    @SerialName("manga") val manga: ApiManga,
    @SerialName("routes") val routes: List<ApiRoute> = emptyList(),
    @SerialName("route_count") val routeCount: Int = 0,
)

@Serializable
data class ApiChapter(
    @SerialName("id") val id: String? = null,
    @SerialName("manga_id") val mangaId: String = "",
    @SerialName("route_number") val routeNumber: Int = 1,
    @SerialName("chapter_number") val chapterNumber: Float = 0f,
    @SerialName("title") val title: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("pages") val pages: List<String> = emptyList(),
    @SerialName("uploaded_at") val uploadedAt: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("is_downloaded") val isDownloaded: Boolean = false,
)

@Serializable
data class ApiSearchResult(
    @SerialName("manga") val manga: ApiMangaWithRoutes,
    @SerialName("relevance_score") val relevanceScore: Float = 0f,
    @SerialName("matched_query") val matchedQuery: String = "",
)

@Serializable
data class ApiSearchResponse(
    @SerialName("query") val query: String = "",
    @SerialName("results") val results: List<ApiSearchResult> = emptyList(),
    @SerialName("total") val total: Int = 0,
    @SerialName("page") val page: Int = 1,
    @SerialName("has_next") val hasNext: Boolean = false,
    @SerialName("source_category") val sourceCategory: String? = null,
)

@Serializable
data class ApiHomePageData(
    @SerialName("continue_reading") val continueReading: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    @SerialName("ai_recommendation") val aiRecommendation: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    @SerialName("because_you_read") val becauseYouRead: List<ApiManga> = emptyList(),
    @SerialName("weekly_hot") val weeklyHot: List<ApiManga> = emptyList(),
    @SerialName("tracking_updates") val trackingUpdates: List<ApiManga> = emptyList(),
    @SerialName("hidden_gems") val hiddenGems: List<ApiManga> = emptyList(),
)

@Serializable
data class ApiHotListResponse(
    @SerialName("manga_list") val mangaList: List<ApiMangaWithRoutes> = emptyList(),
    @SerialName("total") val total: Int = 0,
)

@Serializable
data class ApiAuthResponse(
    @SerialName("user_id") val userId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("token") val token: String = "",
)

@Serializable
data class ApiAccountInfo(
    @SerialName("user_id") val userId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class ApiLoginRequest(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
)

@Serializable
data class ApiRegisterRequest(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
)

@Serializable
data class ApiUserInfo(
    @SerialName("user_id") val userId: String = "",
    @SerialName("nickname") val nickname: String = "漫界用户",
    @SerialName("avatar") val avatar: String? = null,
    @SerialName("is_vip") val isVip: Boolean = false,
    @SerialName("vip_expire") val vipExpire: String? = null,
    @SerialName("library_count") val libraryCount: Int = 0,
    @SerialName("reading_history_count") val readingHistoryCount: Int = 0,
)

@Serializable
data class ApiAdEntitlement(
    @SerialName("user_id") val userId: String = "",
    @SerialName("free_minutes_remaining") val freeMinutesRemaining: Int = 30,
    @SerialName("total_earned_minutes") val totalEarnedMinutes: Int = 0,
    @SerialName("ads_watched_today") val adsWatchedToday: Int = 0,
    @SerialName("max_ads_per_day") val maxAdsPerDay: Int = 3,
    @SerialName("last_ad_time") val lastAdTime: String? = null,
    @SerialName("cooldown_remaining") val cooldownRemaining: Int = 0,
    @SerialName("is_vip") val isVip: Boolean = false,
)

@Serializable
data class ApiUniverseServer(
    @SerialName("server_id") val serverId: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("tagline") val tagline: String = "",
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("category") val category: String = "",
    @SerialName("language") val language: String = "",
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    @SerialName("priority") val priority: Int = 2,
)

@Serializable
data class ApiUniverseCategory(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("description") val description: String = "",
)

@Serializable
data class ApiUniverseResponse(
    @SerialName("servers") val servers: List<ApiUniverseServer> = emptyList(),
    @SerialName("total") val total: Int = 0,
    @SerialName("categories") val categories: List<ApiUniverseCategory> = emptyList(),
)
