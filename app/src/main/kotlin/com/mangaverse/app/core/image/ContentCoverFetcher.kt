package com.mangaverse.app.core.image

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.network.HttpException
import coil3.network.NetworkHeaders
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import coil3.request.Options
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import com.mangaverse.app.core.model.unwrap
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.parser.getAvailableRepositoryOrNull
import com.mangaverse.app.core.util.ext.mangaSourceKey
import com.mangaverse.app.core.util.ext.toMimeTypeOrNull
import com.mangaverse.app.local.data.CacheDir
import com.mangaverse.app.local.data.LocalStorageCache
import com.mangaverse.app.parsers.util.requireBody
import javax.inject.Inject

class ContentCoverFetcher(
	private val imageUrl: String,
	private val options: Options,
	private val imageClient: OkHttpClient,
	private val repo: ContentRepository,
	private val cacheDir: FileSystem,
) : Fetcher {

	override suspend fun fetch(): FetchResult? {
		val request = repo.createCoverRequest(imageUrl)

		val response = try {
			imageClient.newCall(request).execute()
		} catch (e: com.mangaverse.app.core.exceptions.CloudFlareException) {
			// Do not let InteractiveActionRequiredException bubble up to Coil EventListener
			// because it will trigger CaptchaHandler and potentially an unsolvable CDN CF loop.
			throw HttpException(
				NetworkResponse(
					code = 403,
					requestMillis = System.currentTimeMillis(),
					responseMillis = System.currentTimeMillis(),
					headers = NetworkHeaders.Builder().build(),
					body = null,
					delegate = okhttp3.Response.Builder()
						.request(request)
						.protocol(okhttp3.Protocol.HTTP_1_1)
						.message("CloudFlare Protected CDN")
						.code(403)
						.build()
				)
			)
		}

		if (!response.isSuccessful) {
			response.close()
			throw HttpException(response.toNetworkResponse())
		}

		val mimeType = response.mimeType?.toMimeTypeOrNull()
		// Since Coil manages its own disk caching for covers, we just stream it to Coil's SourceFetchResult.
		// NetworkResponseBody handles caching if needed by Coil's internal disk cache.
		return SourceFetchResult(
			source = ImageSource(
				source = response.requireBody().source(),
				fileSystem = FileSystem.SYSTEM
			),
			mimeType = mimeType?.toString(),
			dataSource = DataSource.NETWORK,
		)
	}

	private val okhttp3.Response.mimeType: String?
		get() = header("Content-Type") ?: body?.contentType()?.toString()

	private fun okhttp3.Response.toNetworkResponse() = NetworkResponse(
		code = code,
		requestMillis = sentRequestAtMillis,
		responseMillis = receivedResponseAtMillis,
		headers = headers.toNetworkHeaders(),
		body = body?.source()?.let(::NetworkResponseBody),
		delegate = this,
	)

	private fun Headers.toNetworkHeaders(): NetworkHeaders {
		val headers = NetworkHeaders.Builder()
		for ((key, values) in this) {
			headers.add(key, values)
		}
		return headers.build()
	}

	class Factory @Inject constructor(
		private val mangaRepositoryFactory: ContentRepository.Factory,
	) : Fetcher.Factory<String> {

		override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
			if (!data.startsWith("http")) return null
			
			val mangaSource = options.extras[mangaSourceKey]?.unwrap() ?: return null
			val repo = mangaRepositoryFactory.createWithDiagnostics(mangaSource).getAvailableRepositoryOrNull(
				tag = "ContentCoverFetcher",
				prefix = "repository_unavailable",
			) ?: return null
			
			val imageClient = repo.getImageClient() ?: return null
			
			return ContentCoverFetcher(
				imageUrl = data,
				options = options,
				imageClient = imageClient,
				repo = repo,
				cacheDir = options.fileSystem
			)
		}
	}
}
