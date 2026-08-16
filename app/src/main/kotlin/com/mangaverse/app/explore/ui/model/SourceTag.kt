package com.mangaverse.app.explore.ui.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mangaverse.app.R
import com.mangaverse.app.core.jsonsource.ContentGroup
import com.mangaverse.app.core.jsonsource.OriginGroup

/**
 * Single-select source tags shown in the secondary filter bar.
 *
 * BUILTIN: filter native sources
 * MIHON : filter Mihon-origin sources
 * LEGADO: filter Legado JSON-origin sources
 */
enum class SourceTag(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val id: String,
) {
    BUILTIN(R.string.built_in_sources, R.drawable.ic_source_builtin, "builtin"),
    MIHON(R.string.mihon_sources, R.drawable.ic_source_mihon, "mihon"),
    LEGADO(R.string.source_type_legado, R.drawable.ic_source_legado, "legado"),
    PINNED(R.string.source_pinned, R.drawable.ic_pin, "pinned");

    /**
     * Whether this tag matches the given content and origin group.
     */
    fun matches(contentGroup: ContentGroup, originGroup: OriginGroup): Boolean = when (this) {
        BUILTIN -> originGroup == OriginGroup.NATIVE
        MIHON -> originGroup == OriginGroup.MIHON
        LEGADO -> originGroup == OriginGroup.LEGADO_JSON
        PINNED -> true
    }

    /**
     * Check if this tag supports the given content tab.
     */
    fun supportsContentTab(tab: BrowseGroupTab): Boolean = when (this) {
        BUILTIN -> true
        MIHON -> tab == BrowseGroupTab.Content || tab == BrowseGroupTab.All
        LEGADO -> tab == BrowseGroupTab.Content || tab == BrowseGroupTab.All
        PINNED -> true
    }

    companion object {
        val quickFilterEntries: List<SourceTag> = listOf(
            BUILTIN,
            MIHON,
            LEGADO,
        )

        fun sanitizeQuickFilterSelection(tags: Set<SourceTag>): Set<SourceTag> =
            tags.filterTo(linkedSetOf()) { it in quickFilterEntries || it == PINNED }

        fun fromIds(ids: Collection<String>): Set<SourceTag> =
            ids.mapNotNull { id ->
                when (id) {
                    "json" -> LEGADO
                    else -> entries.find { it.id == id }
                }
            }.toSet()
    }
}
