package com.mangaverse.app.entitygraph.ui.details

import androidx.annotation.StringRes
import com.mangaverse.app.entitygraph.domain.EntityType

data class EntityRelationSection(
    @StringRes val titleRes: Int? = null,
    val title: String? = null,
    val items: List<EntityRelationItem>,
)

data class EntityRelationItem(
    val stableKey: String,
    val name: String,
    val coverUrl: String?,
    val entityId: Long? = null,
    val type: EntityType? = null,
    val subtitle: String? = null,
    val supportingText: String? = null,
    val detailLines: List<String> = emptyList(),
    val remoteId: Long? = null,
    val url: String? = null,
)
