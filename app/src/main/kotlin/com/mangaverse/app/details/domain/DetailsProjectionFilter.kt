package com.mangaverse.app.details.domain

import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.work.domain.isWorkContentTypeCompatibleWith

/**
 * A work details page may only expose projections from the same content-type
 * family as the currently selected projection and, when present, its Space.
 * Unknown types are rejected so legacy data cannot widen the result set.
 */
internal fun isDetailsProjectionAllowed(
	currentType: ContentType?,
	projectionType: ContentType?,
	spaceAllowedTypes: Set<ContentType>?,
): Boolean {
	return currentType.isWorkContentTypeCompatibleWith(projectionType) &&
		(spaceAllowedTypes == null || projectionType in spaceAllowedTypes)
}
