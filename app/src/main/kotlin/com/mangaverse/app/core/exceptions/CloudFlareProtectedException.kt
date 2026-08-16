package com.mangaverse.app.core.exceptions

import okhttp3.Headers
import com.mangaverse.app.core.model.UnknownContentSource
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.network.CloudFlareHelper

class CloudFlareProtectedException(
	override val url: String,
	source: ContentSource?,
	@Transient val headers: Headers,
	@Transient val method: String = "GET",
	@Transient val body: String? = null,
	@Transient val contentType: String? = headers["Content-Type"],
) : CloudFlareException("Protected by CloudFlare", CloudFlareHelper.PROTECTION_CAPTCHA) {

	override val source: ContentSource = source ?: UnknownContentSource
}
