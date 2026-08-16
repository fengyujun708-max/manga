package com.mangaverse.app.main.ui.navigation3

import com.mangaverse.app.details.ui.model.DetailsOrigin
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.SortOrder

interface MainNavigator {
    fun openTopLevel(key: TopLevelNavKey)

    fun openContentList(
        source: ContentSource,
        filter: ContentListFilter? = null,
        sortOrder: SortOrder? = null,
    )

    fun openDetails(
        content: Content,
        sharedElementKey: String? = null,
    )

    fun openDetails(
        origin: DetailsOrigin,
        sharedElementKey: String? = null,
    )

    fun openUniverse()

    fun openAccount()

    fun openServer(serverId: String, serverName: String)

    fun openEnergy()

    fun pop(): Boolean
}
