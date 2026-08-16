package com.mangaverse.app.entitygraph.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.hilt.work.WorkerAssistedFactory
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.entitygraph.data.EntityGraphRepository
import com.mangaverse.app.entitygraph.data.computeNameHash
import com.mangaverse.app.entitygraph.data.isActiveBinding
import com.mangaverse.app.favourites.domain.FavouritesRepository
import com.mangaverse.app.work.domain.WorkResolver

@HiltWorker
class EntityGraphMigrationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val db: MangaDatabase,
    private val entityGraphRepository: EntityGraphRepository,
    private val favouritesRepository: FavouritesRepository,
    private val workResolver: WorkResolver,
    private val settings: AppSettings,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // Build bindings from every legacy favourite projection first. The aggregate
            // display projection is intentionally only one item per Work and is not a
            // complete migration input.
            favouritesRepository.ensureLegacyFavouriteProjectionsForMigration()
            favouritesRepository.getAllContent().forEach { content ->
                workResolver.ensureForProjection(
                    content = content,
                    provenance = com.mangaverse.app.work.domain.WorkIdentityProvenance.MIGRATION,
                )
            }
            normalizeReadingRecordAnchors()
            entityGraphRepository.pruneRedundantProjectionMetadataSelections()

            // 3. Backfill name_hash for entities that still use the migration placeholder (name_hash = id).
            //    After Migration50To51, existing entities had name_hash set to row-id as a temporary value.
            //    This step recomputes the true normalised name hash.
            backfillNameHashes()
            settings.isLegacyFavouriteProjectionMigrationCompleted = true

            Result.success()
        } catch (e: Throwable) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun normalizeReadingRecordAnchors() {
        val entityDao = db.getEntityGraphDao()
        val readingDao = db.getReadingRecordDao()
        val bindingsByEntity = entityDao.dumpBindings()
            .filter { it.isActiveBinding() }
            .filter { it.source == "local_manga" || it.source == "0" }
            .groupBy { it.entityId }

        bindingsByEntity.forEach { (entityId, bindings) ->
            val localIds = bindings.mapNotNull { it.externalId.toLongOrNull() }.distinct()
            if (localIds.size < 2) {
                return@forEach
            }
            val preferredLocalId = workResolver.selectPreferredProjection(entityId)
                ?: localIds.firstOrNull()
                ?: return@forEach
            val sourceIds = localIds.filter { it != preferredLocalId }
            if (sourceIds.isEmpty()) {
                return@forEach
            }
            val sessions = readingDao.findSessions(sourceIds)
            if (sessions.isNotEmpty()) {
                sessions.forEach { session ->
                    readingDao.insertSession(session.copy(id = 0L, mangaId = preferredLocalId))
                }
                readingDao.clearSessions(sourceIds)
            }
            val jumpPoints = readingDao.findJumpPoints(sourceIds, Int.MAX_VALUE)
            if (jumpPoints.isNotEmpty()) {
                jumpPoints.forEach { jumpPoint ->
                    readingDao.insertJumpPoint(jumpPoint.copy(id = 0L, mangaId = preferredLocalId))
                }
                readingDao.clearJumpPoints(sourceIds)
            }
        }
    }

    private suspend fun backfillNameHashes() {
        val dao = db.getEntityGraphDao()
        val entities = dao.dumpEntities()
        for (record in entities) {
            val computedHash = computeNameHash(record.primaryName)
            if (record.nameHash != computedHash && record.nameHash == record.id) {
                // Only fix entities that still have the migration placeholder (name_hash == id).
                // Entities created after the migration will already have correct name_hash.
                dao.upsertEntityRecord(record.copy(nameHash = computedHash))
            }
        }
    }

    @AssistedFactory
    interface Factory : WorkerAssistedFactory<EntityGraphMigrationWorker>
}
