package com.mangaverse.app.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.mangaverse.app.bookmarks.data.BookmarkEntity
import com.mangaverse.app.bookmarks.data.BookmarksDao
import com.mangaverse.app.core.db.dao.ChaptersDao
// import com.mangaverse.app.core.db.dao.EpubChapterDao
import com.mangaverse.app.core.db.dao.EpubChapterMappingDao
import com.mangaverse.app.core.db.dao.ExternalExtensionRepoDao
import com.mangaverse.app.core.db.dao.JsonSourceDao
import com.mangaverse.app.core.db.dao.MangaDao
import com.mangaverse.app.core.db.dao.MangaSourcesDao
import com.mangaverse.app.core.db.dao.PreferencesDao
import com.mangaverse.app.core.db.dao.TagsDao
import com.mangaverse.app.core.db.dao.TrackLogsDao
import com.mangaverse.app.core.db.entity.ChapterEntity
// import com.mangaverse.app.core.db.entity.EpubChapterEntity
import com.mangaverse.app.core.db.entity.EpubChapterMappingEntity
import com.mangaverse.app.core.db.entity.ExternalExtensionRepoEntity
import com.mangaverse.app.core.db.entity.JsonSourceEntity
import com.mangaverse.app.core.db.entity.MangaEntity
import com.mangaverse.app.core.db.entity.MangaPrefsEntity
import com.mangaverse.app.core.db.entity.MangaSourceEntity
import com.mangaverse.app.core.db.entity.MangaTagsEntity
import com.mangaverse.app.core.db.entity.TagEntity
import com.mangaverse.app.core.db.migrations.Migration10To11
import com.mangaverse.app.core.db.migrations.Migration11To12
import com.mangaverse.app.core.db.migrations.Migration12To13
import com.mangaverse.app.core.db.migrations.Migration13To14
import com.mangaverse.app.core.db.migrations.Migration14To15
import com.mangaverse.app.core.db.migrations.Migration15To16
import com.mangaverse.app.core.db.migrations.Migration16To17
import com.mangaverse.app.core.db.migrations.Migration17To18
import com.mangaverse.app.core.db.migrations.Migration18To19
import com.mangaverse.app.core.db.migrations.Migration19To20
import com.mangaverse.app.core.db.migrations.Migration34To35
import com.mangaverse.app.core.db.migrations.Migration37To38
import com.mangaverse.app.core.db.migrations.Migration38To39
import com.mangaverse.app.core.db.migrations.Migration39To40
import com.mangaverse.app.core.db.migrations.Migration40To41
import com.mangaverse.app.core.db.migrations.Migration41To42
import com.mangaverse.app.core.db.migrations.Migration42To43
import com.mangaverse.app.core.db.migrations.Migration43To44
import com.mangaverse.app.core.db.migrations.Migration44To45
import com.mangaverse.app.core.db.migrations.Migration45To46
import com.mangaverse.app.core.db.migrations.Migration46To47
import com.mangaverse.app.core.db.migrations.Migration47To48
import com.mangaverse.app.core.db.migrations.Migration48To49
import com.mangaverse.app.core.db.migrations.Migration54To55
import com.mangaverse.app.core.db.migrations.Migration55To56
import com.mangaverse.app.core.db.migrations.Migration56To57
import com.mangaverse.app.core.db.migrations.Migration57To58
import com.mangaverse.app.core.db.migrations.Migration58To59
import com.mangaverse.app.core.db.migrations.Migration59To60
import com.mangaverse.app.core.db.migrations.Migration60To61
import com.mangaverse.app.core.db.migrations.Migration61To62
import com.mangaverse.app.core.db.migrations.Migration62To63
import com.mangaverse.app.core.db.migrations.Migration63To64
import com.mangaverse.app.core.db.migrations.Migration64To65
import com.mangaverse.app.core.db.migrations.Migration65To66
import com.mangaverse.app.core.db.migrations.Migration66To67
import com.mangaverse.app.core.db.migrations.Migration67To68
import com.mangaverse.app.core.db.migrations.Migration68To69
import com.mangaverse.app.core.db.migrations.Migration69To70
import com.mangaverse.app.core.db.migrations.Migration70To71
import com.mangaverse.app.core.db.migrations.Migration71To72
import com.mangaverse.app.core.db.migrations.Migration72To73
import com.mangaverse.app.core.db.migrations.Migration73To74
import com.mangaverse.app.core.db.migrations.Migration74To75
import com.mangaverse.app.core.db.migrations.Migration75To76
import com.mangaverse.app.core.db.migrations.Migration76To77
import com.mangaverse.app.core.db.migrations.Migration1To2
import com.mangaverse.app.core.db.migrations.Migration20To21
import com.mangaverse.app.core.db.migrations.Migration21To22
import com.mangaverse.app.core.db.migrations.Migration22To23
import com.mangaverse.app.core.db.migrations.Migration23To24
import com.mangaverse.app.core.db.migrations.Migration24To23
import com.mangaverse.app.core.db.migrations.Migration24To25
import com.mangaverse.app.core.db.migrations.Migration25To26
import com.mangaverse.app.core.db.migrations.Migration26To27
import com.mangaverse.app.core.db.migrations.Migration27To28
import com.mangaverse.app.core.db.migrations.Migration28To29
import com.mangaverse.app.core.db.migrations.Migration29To30
import com.mangaverse.app.core.db.migrations.Migration30To31
import com.mangaverse.app.core.db.migrations.Migration31To32
import com.mangaverse.app.core.db.migrations.Migration32To33
import com.mangaverse.app.core.db.migrations.Migration33To34
import com.mangaverse.app.core.db.migrations.Migration2To3
import com.mangaverse.app.core.db.migrations.Migration3To4
import com.mangaverse.app.core.db.migrations.Migration4To5
import com.mangaverse.app.core.db.migrations.Migration5To6
import com.mangaverse.app.core.db.migrations.Migration6To7
import com.mangaverse.app.core.db.migrations.Migration7To8
import com.mangaverse.app.core.db.migrations.Migration8To9
import com.mangaverse.app.core.db.migrations.Migration9To10
import com.mangaverse.app.core.util.ext.processLifecycleScope
import com.mangaverse.app.entitygraph.data.EntityBindingRecord
import com.mangaverse.app.entitygraph.data.EntityGraphDao
import com.mangaverse.app.entitygraph.data.EntityPrefsRecord
import com.mangaverse.app.entitygraph.data.EntityRecord
import com.mangaverse.app.entitygraph.data.RelationRecord
import com.mangaverse.app.favourites.data.FavouriteCategoriesDao
import com.mangaverse.app.favourites.data.FavouriteCategoryEntity
import com.mangaverse.app.favourites.data.FavouriteEntity
import com.mangaverse.app.favourites.data.FavouritesDao
import com.mangaverse.app.favourites.data.WorkFavouriteEntity
import com.mangaverse.app.favourites.data.WorkFavouritesDao
import com.mangaverse.app.history.data.HistoryDao
import com.mangaverse.app.history.data.HistoryEntity
import com.mangaverse.app.history.data.WorkHistoryDao
import com.mangaverse.app.history.data.WorkHistoryEntity
import com.mangaverse.app.local.data.index.LocalContentIndexDao
import com.mangaverse.app.local.data.index.LocalContentIndexEntity
import com.mangaverse.app.readingrecord.data.ReadingJumpPointEntity
import com.mangaverse.app.readingrecord.data.ReadingRecordDao
import com.mangaverse.app.readingrecord.data.ReadingRecordEntity
import com.mangaverse.app.tracker.data.TrackEntity
import com.mangaverse.app.tracker.data.TrackLogEntity
import com.mangaverse.app.tracker.data.TracksDao
import com.mangaverse.app.work.data.WorkMigrationLedgerDao
import com.mangaverse.app.work.data.WorkMigrationLedgerEntity

import com.mangaverse.app.explore.data.SourcePresetEntity
import com.mangaverse.app.explore.data.SourcePresetsDao

const val DATABASE_VERSION = 77

@Database(
	entities = [
		MangaEntity::class, TagEntity::class, HistoryEntity::class, WorkHistoryEntity::class, MangaTagsEntity::class, ChapterEntity::class,
		FavouriteCategoryEntity::class, FavouriteEntity::class, WorkFavouriteEntity::class, MangaPrefsEntity::class, TrackEntity::class,
		TrackLogEntity::class, SuggestionEntity::class, BookmarkEntity::class,
		MangaSourceEntity::class, StatsEntity::class, WorkStatsEntity::class, LocalContentIndexEntity::class, EpubChapterMappingEntity::class,
		JsonSourceEntity::class, ExternalExtensionRepoEntity::class,
		SourcePresetEntity::class,
		EntityRecord::class, EntityBindingRecord::class, RelationRecord::class, EntityPrefsRecord::class,
		WorkMigrationLedgerEntity::class,
		ReadingRecordEntity::class, ReadingJumpPointEntity::class,
		SpaceSessionEntity::class, SpaceNavigationEntryEntity::class, SpaceRoutePreferencesEntity::class,
		SpaceDefinitionEntity::class,
		// EpubChapterEntity::class,
	],
	version = DATABASE_VERSION,
)
abstract class MangaDatabase : RoomDatabase() {

	abstract fun getHistoryDao(): HistoryDao

	abstract fun getWorkHistoryDao(): WorkHistoryDao

	abstract fun getTagsDao(): TagsDao

	abstract fun getMangaDao(): MangaDao

	abstract fun getFavouritesDao(): FavouritesDao

	abstract fun getWorkFavouritesDao(): WorkFavouritesDao

	abstract fun getPreferencesDao(): PreferencesDao

	abstract fun getFavouriteCategoriesDao(): FavouriteCategoriesDao

	abstract fun getTracksDao(): TracksDao

	abstract fun getTrackLogsDao(): TrackLogsDao

	abstract fun getSuggestionDao(): SuggestionDao

	abstract fun getBookmarksDao(): BookmarksDao

	abstract fun getSourcesDao(): MangaSourcesDao

	abstract fun getStatsDao(): StatsDao

	abstract fun getWorkStatsDao(): WorkStatsDao

	abstract fun getLocalContentIndexDao(): LocalContentIndexDao

	abstract fun getChaptersDao(): ChaptersDao

	abstract fun getEpubChapterMappingDao(): EpubChapterMappingDao

	abstract fun getJsonSourceDao(): JsonSourceDao

	abstract fun getExternalExtensionRepoDao(): ExternalExtensionRepoDao

	abstract fun getSourcePresetsDao(): SourcePresetsDao

	abstract fun getEntityGraphDao(): EntityGraphDao

	abstract fun getWorkMigrationLedgerDao(): WorkMigrationLedgerDao

	abstract fun getReadingRecordDao(): ReadingRecordDao

	abstract fun getSpaceSessionDao(): SpaceSessionDao

	abstract fun getSpaceRoutePreferencesDao(): SpaceRoutePreferencesDao

	abstract fun getSpaceDefinitionDao(): SpaceDefinitionDao

	// abstract fun getEpubChapterDao(): EpubChapterDao
}

fun getDatabaseMigrations(context: Context): Array<Migration> = arrayOf(
	Migration1To2(),
	Migration2To3(),
	Migration3To4(),
	Migration4To5(),
	Migration5To6(),
	Migration6To7(),
	Migration7To8(),
	Migration8To9(),
	Migration9To10(),
	Migration10To11(),
	Migration11To12(),
	Migration12To13(),
	Migration13To14(),
	Migration14To15(),
	Migration15To16(),
	Migration16To17(context),
	Migration17To18(),
	Migration18To19(),
	Migration19To20(),
	Migration20To21(),
	Migration21To22(),
	Migration22To23(),
	Migration23To24(),
	Migration24To23(),
	Migration24To25(),
	Migration25To26(),
	Migration26To27(),
	Migration27To28(),
	Migration28To29(),
	Migration29To30(),
	Migration30To31(),
	Migration31To32(),
	Migration32To33(),
	Migration33To34(),
	Migration34To35(),
	com.mangaverse.app.core.db.migrations.Migration35To36(),
	com.mangaverse.app.core.db.migrations.Migration36To37(),
	Migration37To38(),
	Migration38To39(),
	Migration39To40(),
	Migration40To41(),
	Migration41To42(),
	Migration42To43(),
	Migration43To44(),
	Migration44To45(),
	Migration45To46(),
	Migration46To47(),
	Migration47To48(),
	Migration48To49(),
	com.mangaverse.app.core.db.migrations.Migration49To50(),
	com.mangaverse.app.core.db.migrations.Migration50To51(),
	com.mangaverse.app.core.db.migrations.Migration51To52(),
	com.mangaverse.app.core.db.migrations.Migration52To53(),
	com.mangaverse.app.core.db.migrations.Migration53To54(),
	com.mangaverse.app.core.db.migrations.Migration54To55(),
	com.mangaverse.app.core.db.migrations.Migration55To56(),
	com.mangaverse.app.core.db.migrations.Migration56To57(),
	com.mangaverse.app.core.db.migrations.Migration57To58(),
	com.mangaverse.app.core.db.migrations.Migration58To59(),
	com.mangaverse.app.core.db.migrations.Migration59To60(),
	Migration60To61(),
	Migration61To62(),
	Migration62To63(),
	Migration63To64(),
	Migration64To65(),
	Migration65To66(),
	Migration66To67(),
	Migration67To68(),
	Migration68To69(),
	Migration69To70(),
	Migration70To71(),
	Migration71To72(),
	Migration72To73(),
	Migration73To74(),
	Migration74To75(),
	Migration75To76(),
	Migration76To77(),
)

fun MangaDatabase(context: Context): MangaDatabase = Room
	.databaseBuilder(context, MangaDatabase::class.java, "kototoro-db")
	.addMigrations(*getDatabaseMigrations(context))
	.fallbackToDestructiveMigrationOnDowngrade()
	.addCallback(DatabasePrePopulateCallback(context.resources))
	.build()

fun InvalidationTracker.removeObserverAsync(observer: InvalidationTracker.Observer) {
	val scope = processLifecycleScope
	if (scope.isActive) {
		processLifecycleScope.launch(Dispatchers.Default, CoroutineStart.ATOMIC) {
			removeObserver(observer)
		}
	}
}
