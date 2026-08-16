package com.mangaverse.app.history.ui

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.mangaverse.app.core.prefs.ListMode
import com.mangaverse.app.explore.data.SourcePreset
import com.mangaverse.app.explore.ui.model.BrowseGroupTab
import com.mangaverse.app.explore.ui.model.SourceTag
import com.mangaverse.app.history.domain.model.ContentWithHistory
import com.mangaverse.app.list.domain.ListFilterOption
import com.mangaverse.app.list.domain.ListSortOrder
import com.mangaverse.app.list.ui.model.QuickFilter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryPreviewCache @Inject constructor() {

    private val previewFlow = MutableStateFlow<HistoryPreviewSnapshot?>(null)

    fun observe(): StateFlow<HistoryPreviewSnapshot?> = previewFlow.asStateFlow()

    fun update(snapshot: HistoryPreviewSnapshot) {
        previewFlow.value = snapshot
    }

    @VisibleForTesting
    fun clear() {
        previewFlow.value = null
    }
}

data class HistoryPreviewSnapshot(
    val items: List<ContentWithHistory>,
    val listMode: ListMode,
    val sortOrder: ListSortOrder,
    val isGroupingEnabled: Boolean,
    val isIncognito: Boolean,
    val groupTab: BrowseGroupTab,
    val sourceTags: Set<SourceTag>,
    val preset: SourcePreset?,
    val filters: Set<ListFilterOption>,
    val quickFilter: QuickFilter?,
    val isHistoryExcludeNsfw: Boolean,
)
