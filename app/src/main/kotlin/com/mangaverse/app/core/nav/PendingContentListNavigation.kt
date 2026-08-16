package com.mangaverse.app.core.nav

import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.SortOrder

object PendingContentListNavigation {

    private var pendingFilter: ContentListFilter? = null
    private var pendingSortOrder: SortOrder? = null

    fun set(
        filter: ContentListFilter?,
        sortOrder: SortOrder?,
    ) {
        pendingFilter = filter
        pendingSortOrder = sortOrder
    }

    fun consumeFilter(): ContentListFilter? = pendingFilter.also { pendingFilter = null }

    fun consumeSortOrder(): SortOrder? = pendingSortOrder.also { pendingSortOrder = null }

    fun clear() {
        pendingFilter = null
        pendingSortOrder = null
    }
}
