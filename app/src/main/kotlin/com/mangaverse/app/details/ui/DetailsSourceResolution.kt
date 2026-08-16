package com.mangaverse.app.details.ui

import com.mangaverse.app.core.model.ContentSourceInfo
import com.mangaverse.app.core.model.resolvedContentTypeForSnapshot
import com.mangaverse.app.parsers.model.ContentSource

internal fun selectResolvedDetailsSource(
	original: ContentSource,
	enabledSources: List<ContentSourceInfo>,
	pipelineResolved: ContentSource,
): ContentSource {
	enabledSources.firstOrNull { it.mangaSource.name == original.name }?.mangaSource?.let { return it }
	return pipelineResolved.takeIf {
		it.resolvedContentTypeForSnapshot() != null || it.locale.isNotBlank()
	} ?: original
}
