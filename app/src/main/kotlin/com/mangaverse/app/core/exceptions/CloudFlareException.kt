package com.mangaverse.app.core.exceptions

import okio.IOException
import com.mangaverse.app.parsers.model.ContentSource

abstract class CloudFlareException(
	message: String,
	val state: Int,
) : IOException(message) {

	abstract val url: String

	abstract val source: ContentSource
}
