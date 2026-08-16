package com.mangaverse.app.core.exceptions

import com.mangaverse.app.core.model.UnknownContentSource
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.network.CloudFlareHelper

class CloudFlareBlockedException(
	override val url: String,
	source: ContentSource?,
) : CloudFlareException("Blocked by CloudFlare", CloudFlareHelper.PROTECTION_BLOCKED) {

	override val source: ContentSource = source ?: UnknownContentSource
}
