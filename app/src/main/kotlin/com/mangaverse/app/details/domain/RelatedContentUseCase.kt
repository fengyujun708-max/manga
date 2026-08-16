package com.mangaverse.app.details.domain

import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.util.runCatchingCancellable
import javax.inject.Inject

class RelatedContentUseCase @Inject constructor(
	private val mangaRepositoryFactory: ContentRepository.Factory,
) {

	suspend operator fun invoke(seed: Content) = runCatchingCancellable {
		mangaRepositoryFactory.create(seed.source).getRelated(seed)
	}.onFailure {
		it.printStackTraceDebug()
	}.getOrNull()
}
