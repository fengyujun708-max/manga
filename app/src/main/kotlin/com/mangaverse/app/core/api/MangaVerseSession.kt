package com.mangaverse.app.core.api

import com.mangaverse.app.core.api.data.MangaVerseRepository
import com.mangaverse.app.core.prefs.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MangaVerse 账号会话管理器（单例）。
 *
 * 负责登录 / 注册 / 登出，并将 token 与用户信息持久化到 [AppSettings]。
 * 通过 [state] 暴露当前会话状态，供 UI 订阅。
 */
@Singleton
class MangaVerseSession @Inject constructor(
    private val repository: MangaVerseRepository,
    private val settings: AppSettings,
) {

    data class SessionState(
        val isLoggedIn: Boolean = false,
        val userId: String = "",
        val username: String = "",
        val token: String = "",
    )

    private val _state = MutableStateFlow(
        SessionState(
            isLoggedIn = settings.isMangaVerseLoggedIn,
            userId = settings.mangaVerseAuthUserId,
            username = settings.mangaVerseAuthUsername,
            token = settings.mangaVerseAuthToken,
        ),
    )
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val isLoggedIn: Boolean
        get() = _state.value.isLoggedIn

    val token: String
        get() = _state.value.token

    /**
     * 使用用户名/密码登录。成功时持久化会话并返回 null，失败时返回错误信息。
     */
    suspend fun login(username: String, password: String): String? {
        val result = repository.login(username, password)
        val auth = result.getOrNull()
        if (auth == null || auth.token.isBlank()) {
            return result.exceptionOrNull()?.message ?: "登录失败"
        }
        persist(auth)
        return null
    }

    /**
     * 注册新账号并自动登录。成功时持久化会话并返回 null，失败时返回错误信息。
     */
    suspend fun register(username: String, password: String): String? {
        val result = repository.register(username, password)
        val auth = result.getOrNull()
        if (auth == null || auth.token.isBlank()) {
            return result.exceptionOrNull()?.message ?: "注册失败"
        }
        persist(auth)
        return null
    }

    /**
     * 登出并清除本地会话。
     */
    fun logout() {
        settings.clearMangaVerseSession()
        _state.value = SessionState()
    }

    private fun persist(auth: com.mangaverse.app.core.api.model.ApiAuthResponse) {
        settings.mangaVerseAuthToken = auth.token
        settings.mangaVerseAuthUsername = auth.username
        settings.mangaVerseAuthUserId = auth.userId.ifBlank { auth.username }
        _state.value = SessionState(
            isLoggedIn = true,
            userId = auth.userId.ifBlank { auth.username },
            username = auth.username,
            token = auth.token,
        )
    }
}
