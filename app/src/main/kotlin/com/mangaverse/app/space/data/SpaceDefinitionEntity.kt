package com.mangaverse.app.space.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mangaverse.app.core.db.TABLE_SPACE_DEFINITION

@Entity(tableName = TABLE_SPACE_DEFINITION)
data class SpaceDefinitionEntity(
    @PrimaryKey
    @ColumnInfo(name = "space_id") val spaceId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "sort_key") val sortKey: Int,
    @ColumnInfo(name = "enabled") val enabled: Boolean,
    @ColumnInfo(name = "content_types") val contentTypes: String,
    @ColumnInfo(name = "source_languages") val sourceLanguages: String,
    @ColumnInfo(name = "source_kinds") val sourceKinds: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long,
)
