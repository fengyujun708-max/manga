package com.mangaverse.app.core.network

import com.mangaverse.app.core.exceptions.CloudFlareProtectedException

data class CloudFlareHandlingPolicy(
	val allowBlockedResponse: Boolean = false,
	val allowCaptchaResponse: Boolean = false,
	val onCaptchaDetected: ((CloudFlareProtectedException) -> Unit)? = null,
)
