package com.mangaverse.app.main.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangaverse.app.R
import com.mangaverse.app.core.api.MangaVerseSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoggedIn: Boolean = false,
    val username: String = "",
    val isLoading: Boolean = false,
    val errorRes: Int? = null,
    // 登录/注册表单
    val tab: AccountTab = AccountTab.LOGIN,
    val formUsername: String = "",
    val formPassword: String = "",
)

enum class AccountTab {
    LOGIN,
    REGISTER,
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val session: MangaVerseSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            isLoggedIn = session.isLoggedIn,
            username = session.state.value.username,
        ),
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // 订阅会话变化，保持 UI 与持久化状态一致
        viewModelScope.launch {
            session.state.collect { s ->
                _uiState.update {
                    it.copy(isLoggedIn = s.isLoggedIn, username = s.username)
                }
            }
        }
    }

    fun setTab(tab: AccountTab) = _uiState.update { it.copy(tab = tab) }

    fun onUsernameChange(value: String) = _uiState.update { it.copy(formUsername = value) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(formPassword = value) }

    fun clearError() = _uiState.update { it.copy(errorRes = null) }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        val username = state.formUsername.trim()
        val password = state.formPassword
        val error = validate(username, password)
        if (error != null) {
            _uiState.update { it.copy(errorRes = error) }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorRes = null) }
        viewModelScope.launch {
            val message = if (state.tab == AccountTab.LOGIN) {
                session.login(username, password)
            } else {
                session.register(username, password)
            }
            if (message == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorRes = null,
                        formPassword = "",
                    )
                }
                onSuccess()
            } else {
                _uiState.update { it.copy(isLoading = false, errorRes = mapServerError(message)) }
            }
        }
    }

    fun logout() {
        session.logout()
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                username = "",
                formUsername = "",
                formPassword = "",
            )
        }
    }

    private fun validate(username: String, password: String): Int? {
        if (username.isBlank() || password.isBlank()) return R.string.auth_error_empty
        if (username.length < 2) return R.string.auth_error_short_username
        if (password.length < 4) return R.string.auth_error_short_password
        return null
    }

    private fun mapServerError(message: String): Int {
        return when {
            message.contains("已存在") -> R.string.auth_error_exists
            message.contains("用户名或密码") -> R.string.auth_error_invalid
            else -> R.string.network_error
        }
    }
}
