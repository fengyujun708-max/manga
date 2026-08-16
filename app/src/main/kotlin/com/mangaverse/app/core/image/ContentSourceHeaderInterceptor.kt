package com.mangaverse.app.core.image

import coil3.intercept.Interceptor
import coil3.network.httpHeaders
import coil3.request.ImageResult
import com.mangaverse.app.core.model.unwrap
import com.mangaverse.app.core.network.CommonHeaders
import com.mangaverse.app.core.util.ext.mangaSourceKey
import com.mangaverse.app.core.model.isLocal

class ContentSourceHeaderInterceptor : Interceptor {

	override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
		val mangaSource = chain.request.extras[mangaSourceKey]?.unwrap()
		if (mangaSource == null || mangaSource.isLocal == true || mangaSource is com.mangaverse.app.core.parser.external.ExternalContentSource) {
			return chain.proceed()
		}

		val request = chain.request
		val newHeaders = request.httpHeaders.newBuilder()
			.set(CommonHeaders.MANGA_SOURCE, mangaSource.name)
			.build()
		val newRequest = request.newBuilder()
			.httpHeaders(newHeaders)
			.build()
		return chain.withRequest(newRequest).proceed()
	}
}
