package com.mangaverse.app.reader.ui.compose

import android.net.Uri
import com.mangaverse.app.reader.domain.PageLoader
import com.mangaverse.app.reader.ui.pager.ReaderPage
import javax.inject.Inject

/**
 * Temporary bridge used while the Compose-owned image pipeline is implemented.
 * New enhancement work must target [ComposeReaderImagePipeline], not this adapter.
 */
class ComposeReaderPageLoader @Inject constructor(
	private val pageLoader: PageLoader,
) {

	val imageLoader
		get() = pageLoader.imageLoader

	suspend fun load(page: ReaderPage, force: Boolean = false): Uri {
		return pageLoader.loadPage(page.toContentPage(), force)
	}

	fun prefetch(pages: List<ReaderPage>) {
		pageLoader.prefetch(pages)
	}
}
