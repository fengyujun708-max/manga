package com.mangaverse.app.main.ui.universe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mangaverse.app.core.api.data.MangaVerseRepository
import com.mangaverse.app.core.api.model.ApiUniverseCategory
import com.mangaverse.app.core.api.model.ApiUniverseServer
import com.mangaverse.app.core.api.model.ApiUniverseResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UniverseUiState(
    val isLoading: Boolean = true,
    val servers: List<ApiUniverseServer> = emptyList(),
    val categories: List<ApiUniverseCategory> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class UniverseViewModel @Inject constructor(
    private val repository: MangaVerseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UniverseUiState(isLoading = true))
    val uiState: StateFlow<UniverseUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val response = repository.getUniverseServers().getOrNull()
            if (response == null) {
                _uiState.value = UniverseUiState(isLoading = false, error = "无法连接漫画宇宙")
                return@launch
            }
            _uiState.value = UniverseUiState(
                isLoading = false,
                servers = response.servers,
                categories = response.categories,
            )
        }
    }
}
