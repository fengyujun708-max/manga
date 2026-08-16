package com.mangaverse.app.favourites.ui.migration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.mangaverse.app.favourites.ui.migration.compose.EntityOrganizeEntryMode
import com.mangaverse.app.favourites.ui.migration.compose.WorkbenchSortMode
import com.mangaverse.app.favourites.ui.migration.compose.WorkbenchStatusFilter
import com.mangaverse.app.favourites.ui.migration.compose.resolveEntityOrganizeEntryMode
import com.mangaverse.app.favourites.ui.migration.compose.resolveEntityOrganizeWorkbenchDefaults

class EntityOrganizeEntryModeTest {

    @Test
    fun `positive selected count resolves to manual selection mode`() {
        assertEquals(
            EntityOrganizeEntryMode.MANUAL_SELECTION,
            resolveEntityOrganizeEntryMode(selectedCount = 3),
        )
    }

    @Test
    fun `zero selected count resolves to all favorites mode`() {
        assertEquals(
            EntityOrganizeEntryMode.ALL_FAVORITES,
            resolveEntityOrganizeEntryMode(selectedCount = 0),
        )
    }

    @Test
    fun `manual selection defaults to selected and best match ordering`() {
        val defaults = resolveEntityOrganizeWorkbenchDefaults(
            entryMode = EntityOrganizeEntryMode.MANUAL_SELECTION,
        )

        assertEquals(WorkbenchStatusFilter.SELECTED, defaults.statusFilter)
        assertEquals(WorkbenchSortMode.MATCH_SCORE, defaults.sortMode)
    }

    @Test
    fun `all favorites defaults to all and action first ordering`() {
        val defaults = resolveEntityOrganizeWorkbenchDefaults(
            entryMode = EntityOrganizeEntryMode.ALL_FAVORITES,
        )

        assertEquals(WorkbenchStatusFilter.ALL, defaults.statusFilter)
        assertEquals(WorkbenchSortMode.ACTION_FIRST, defaults.sortMode)
    }
}
