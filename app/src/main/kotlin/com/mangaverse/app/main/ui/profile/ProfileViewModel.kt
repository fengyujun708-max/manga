package com.mangaverse.app.main.ui.profile

import androidx.lifecycle.ViewModel
import com.mangaverse.app.core.api.MangaVerseSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 「我的」页 ViewModel —— 只透传会话状态与登出动作。
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val session: MangaVerseSession,
) : ViewModel() {

    val sessionState = session.state

    fun logout() {
        session.logout()
    }
}
