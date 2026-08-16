package com.mangaverse.app.tracker.data

import androidx.room.ColumnInfo

data class NewChaptersCountEntry(
	@ColumnInfo(name = "manga_id") val mangaId: Long,
	@ColumnInfo(name = "chapters_new") val count: Int,
)
