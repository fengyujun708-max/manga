package com.mangaverse.app.favourites.domain

import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.work.domain.WorkIdentityProvenance
import com.mangaverse.app.work.domain.WorkProjectionBindingConflict
import com.mangaverse.app.work.domain.WorkProjectionBindingResult
import com.mangaverse.app.work.domain.WorkResolver
import javax.inject.Inject

class AttachReadingSourceToEntityUseCase @Inject constructor(
	private val contentRepositoryFactory: ContentRepository.Factory,
	private val contentDataRepository: ContentDataRepository,
	private val workResolver: WorkResolver,
) {

	suspend operator fun invoke(
		oldContent: Content,
		newContent: Content,
	): WorkProjectionBindingResult {
		val targetEntityId = requireNotNull(
			workResolver.ensureForProjection(
				content = oldContent,
				provenance = WorkIdentityProvenance.USER,
			).entityId,
		)
		return attachToEntity(targetEntityId, newContent)
	}

	suspend fun attachToEntity(
		targetEntityId: Long,
		newContent: Content,
	): WorkProjectionBindingResult {
		if (workResolver.resolveByEntityId(targetEntityId) == null) {
			return WorkProjectionBindingResult.Conflict(
				reason = WorkProjectionBindingConflict.TARGET_ENTITY_MISSING,
				targetEntityId = targetEntityId,
				projectionId = newContent.id.takeIf { it != 0L },
			)
		}
		val details = if (newContent.chapters.isNullOrEmpty()) {
			contentRepositoryFactory.create(newContent.source).getDetails(newContent)
		} else {
			newContent
		}
		val storedProjection = contentDataRepository.storeContentAndReturn(
			manga = details,
			replaceExisting = true,
		)
		return workResolver.bindProjectionToEntity(
			targetEntityId = targetEntityId,
			projection = storedProjection,
		)
	}
}
