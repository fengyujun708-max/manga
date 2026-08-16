package com.mangaverse.app.stats.data

import androidx.room.Dao
import androidx.room.Query

@Dao
abstract class StatsDao {

	@Query("DELETE FROM stats")
	abstract suspend fun clear()
}
