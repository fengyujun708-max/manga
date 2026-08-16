package com.mangaverse.app.details.ui.model

import com.mangaverse.app.parsers.model.Content

sealed class LocalSearchState {
    object Loading : LocalSearchState()
    data class Loaded(val items: List<Content>) : LocalSearchState()
    data class Error(val throwable: Throwable) : LocalSearchState()
}
