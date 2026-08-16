package com.mangaverse.app.download.ui.worker

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.hilt.work.WorkerAssistedFactory
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.await
import dagger.Reusable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.internal.closeQuietly
import okio.IOException
import okio.buffer
import okio.sink
import okio.source
import okio.use
import com.mangaverse.app.R
import com.mangaverse.app.BuildConfig
import com.mangaverse.app.core.image.BitmapDecoderCompat
import com.mangaverse.app.core.model.getContentType
import com.mangaverse.app.core.model.ids
import com.mangaverse.app.core.model.isLocal
import com.mangaverse.app.core.network.ContentHttpClient
import com.mangaverse.app.core.network.imageproxy.ImageProxyInterceptor
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.parser.requireAvailableRepository
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.DownloadFormat
import com.mangaverse.app.core.util.MimeTypes
import com.mangaverse.app.core.util.Throttler
import com.mangaverse.app.core.util.ext.MimeType
import com.mangaverse.app.core.util.ext.awaitFinishedWorkInfosByTag
import com.mangaverse.app.core.util.ext.awaitUpdateWork
import com.mangaverse.app.core.util.ext.awaitWorkInfosByTag
import com.mangaverse.app.core.util.ext.deleteAwait
import com.mangaverse.app.core.util.ext.deleteWork
import com.mangaverse.app.core.util.ext.deleteWorks
import com.mangaverse.app.core.util.ext.ensureSuccess
import com.mangaverse.app.core.util.ext.getDisplayMessage
import com.mangaverse.app.core.util.ext.getWorkInputData
import com.mangaverse.app.core.util.ext.getWorkSpec
import com.mangaverse.app.core.util.ext.isFileUri
import com.mangaverse.app.core.util.ext.isZipUri
import com.mangaverse.app.core.util.ext.openSource
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.core.util.ext.toFileOrNull
import com.mangaverse.app.core.util.ext.toFileNameSafe
import com.mangaverse.app.core.util.ext.toMimeType
import com.mangaverse.app.core.util.ext.toMimeTypeOrNull
import com.mangaverse.app.core.util.ext.use
import com.mangaverse.app.core.util.ext.withTicker
import com.mangaverse.app.core.util.ext.writeAllCancellable
import com.mangaverse.app.core.util.progress.RealtimeEtaEstimator
import com.mangaverse.app.core.model.unwrap
import com.mangaverse.app.download.domain.DownloadProgress
import com.mangaverse.app.download.domain.DownloadState
import com.mangaverse.app.local.data.LocalMangaRepository
import com.mangaverse.app.local.data.LocalStorageCache
import com.mangaverse.app.local.data.LocalStorageChanges
import com.mangaverse.app.local.data.PageCache
import com.mangaverse.app.local.data.TempFileFilter
import com.mangaverse.app.local.data.input.LocalContentParser
import com.mangaverse.app.local.data.output.LocalContentOutput
import com.mangaverse.app.local.data.output.LocalContentDirOutput
import com.mangaverse.app.local.domain.ContentLock
import com.mangaverse.app.local.domain.model.LocalContent
import com.mangaverse.app.parsers.exception.TooManyRequestExceptions
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.model.ContentPage
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.NovelChapterContent
import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.parsers.util.ifNullOrEmpty
import com.mangaverse.app.parsers.util.mapToSet
import com.mangaverse.app.parsers.util.requireBody
import com.mangaverse.app.parsers.util.await
import com.mangaverse.app.parsers.util.runCatchingCancellable
import com.mangaverse.app.reader.domain.ReaderSuperResolutionManager
import com.mangaverse.app.reader.domain.PageLoader
import com.mangaverse.app.reader.translate.domain.ReaderPageTranslationProcessor
import org.jsoup.Jsoup
import java.io.File
import java.net.URLDecoder
import java.util.UUID
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile
import javax.inject.Inject

@HiltWorker
class DownloadWorker @AssistedInject constructor(
	@Assisted appContext: Context,
	@Assisted params: WorkerParameters,
	@ContentHttpClient private val okHttp: OkHttpClient,
	@PageCache private val cache: LocalStorageCache,
	private val localContentRepository: LocalMangaRepository,
	private val mangaLock: ContentLock,
	private val mangaDataRepository: ContentDataRepository,
	private val mangaRepositoryFactory: ContentRepository.Factory,
	private val settings: AppSettings,
	@LocalStorageChanges private val localStorageChanges: MutableSharedFlow<LocalContent?>,
	private val slowdownDispatcher: DownloadSlowdownDispatcher,
	private val imageProxyInterceptor: ImageProxyInterceptor,
	notificationFactoryFactory: DownloadNotificationFactory.Factory,
	private val mangaDatabase: com.mangaverse.app.core.db.MangaDatabase,
	private val localStorageManager: com.mangaverse.app.local.data.LocalStorageManager,
	private val translationProcessor: ReaderPageTranslationProcessor,
	private val superResolutionManager: ReaderSuperResolutionManager,
) : CoroutineWorker(appContext, params) {

	private data class DownloadExecutionContext(
		val executionManga: Content,
		val displayMangaId: Long,
	)

	private data class DownloadResolvedContent(
		val executionManga: Content,
		val executionDetails: Content,
	)

	private val task = DownloadTask(params.inputData)
	private val notificationFactory = notificationFactoryFactory.create(uuid = params.id, isSilent = task.isSilent)
	private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

	@Volatile
	private var lastPublishedState: DownloadState? = null
	private val currentState: DownloadState
		get() = checkNotNull(lastPublishedState)

	private val etaEstimator = RealtimeEtaEstimator()
	private val notificationThrottler = Throttler(400)

	private suspend fun resolveExecutionContext(executionManga: Content): DownloadExecutionContext {
		val displayMangaId = task.displayMangaId
			?: mangaDataRepository.findDisplayContentById(executionManga.id, withChapters = false)?.id
			?: executionManga.id
		return DownloadExecutionContext(
			executionManga = executionManga,
			displayMangaId = displayMangaId,
		)
	}

	private suspend fun resolveExecutionContent(executionManga: Content): DownloadResolvedContent {
		if (executionManga.isLocal) {
			val remoteExecutionManga = localContentRepository.getRemoteContent(executionManga)
				?: error("Cannot obtain remote manga instance")
			val repo = mangaRepositoryFactory.createWithDiagnostics(remoteExecutionManga.source).requireAvailableRepository(
				tag = "DownloadWorker",
				prefix = "resolveExecutionContent_repository_unavailable",
			) { "Download source ${remoteExecutionManga.source.name} is not available" }
			val executionDetails = if (
				remoteExecutionManga.chapters.isNullOrEmpty() ||
				remoteExecutionManga.description.isNullOrEmpty()
			) {
				repo.getDetails(remoteExecutionManga)
			} else {
				remoteExecutionManga
			}
			return DownloadResolvedContent(
				executionManga = remoteExecutionManga,
				executionDetails = executionDetails,
			)
		}
		val executionDetails = if (
			executionManga.chapters.isNullOrEmpty() ||
			executionManga.description.isNullOrEmpty()
		) {
			val repo = mangaRepositoryFactory.createWithDiagnostics(executionManga.source).requireAvailableRepository(
				tag = "DownloadWorker",
				prefix = "resolveExecutionContent_repository_unavailable",
			) { "Download source ${executionManga.source.name} is not available" }
			repo.getDetails(executionManga)
		} else {
			executionManga
		}
		return DownloadResolvedContent(
			executionManga = executionManga,
			executionDetails = executionDetails,
		)
	}

	override suspend fun doWork(): Result = withContext(com.mangaverse.app.core.parser.legado.RequestPriority(com.mangaverse.app.core.parser.legado.RequestPriority.BACKGROUND)) {
		setForeground(getForegroundInfo())
		val executionManga = mangaDataRepository.findContentById(task.executionMangaId, withChapters = true) ?: return@withContext Result.failure()
		val executionContext = resolveExecutionContext(executionManga)
		publishState(
			DownloadState(
				manga = executionContext.executionManga,
				displayMangaId = executionContext.displayMangaId,
				isIndeterminate = true,
				taskKind = task.kind,
			).also { lastPublishedState = it },
		)
		Log.i(
			"DownloadWorker",
			"doWork start: workId=$id mangaId=${executionContext.executionManga.id} title=${executionContext.executionManga.title} " +
				"displayMangaId=${executionContext.displayMangaId} kind=${task.kind} " +
				"chapters=${executionContext.executionManga.chapters?.size ?: 0} taskChapters=${task.executionChapterIds?.size ?: -1}",
		)

		ActiveDownloadRegistry.register(id, isPaused = task.isPaused)

		val pausingHandle = PausingHandle()
		if (task.isPaused) {
			Log.i("DownloadWorker", "doWork start paused: workId=$id mangaId=${executionContext.executionManga.id}")
			pausingHandle.pause()
		}

		val pausingReceiver = PausingReceiver(id, pausingHandle)
		ContextCompat.registerReceiver(
			applicationContext,
			pausingReceiver,
			PausingReceiver.createIntentFilter(id),
			ContextCompat.RECEIVER_NOT_EXPORTED,
		)

		try {
			withContext(pausingHandle) {
				checkIsPaused()
				when (task.kind) {
					DownloadTaskKind.DOWNLOAD -> {
						val resolvedContent = resolveExecutionContent(executionContext.executionManga)
						val storedExecutionDetails = mangaDataRepository.storeContentAndReturn(
							resolvedContent.executionDetails,
							replaceExisting = true,
						)
						val storedResolvedContent = resolvedContent.copy(executionDetails = storedExecutionDetails)
						publishExecutionDetailsState(storedExecutionDetails)
						Log.i("DownloadWorker", "doWork before downloadContentImpl: workId=$id mangaId=${executionContext.executionManga.id}")
						val downloadedIds = getDoneChapters(storedExecutionDetails)
						Log.i(
							"DownloadWorker",
							"doWork after getDoneChapters: downloadedIds=${downloadedIds.size} workId=$id mangaId=${executionContext.executionManga.id}",
						)
						downloadContentImpl(
							subject = executionContext.executionManga,
							resolvedContent = storedResolvedContent,
							task = task,
							excludedIds = downloadedIds,
						)
						Log.i("DownloadWorker", "doWork after downloadContentImpl: workId=$id mangaId=${executionContext.executionManga.id}")
					}

					DownloadTaskKind.PREPARE_TRANSLATION,
					DownloadTaskKind.PREPARE_SUPER_RESOLUTION -> {
						Log.i(
							"DownloadWorker",
							"doWork before prepareContentImpl: workId=$id mangaId=${executionContext.executionManga.id} kind=${task.kind}",
						)
						prepareContentImpl(executionContext.executionManga, task)
						Log.i(
							"DownloadWorker",
							"doWork after prepareContentImpl: workId=$id mangaId=${executionContext.executionManga.id} kind=${task.kind}",
						)
					}
				}
			}
			Result.success(currentState.toWorkData())
		} catch (_: CancellationException) {
			withContext(NonCancellable) {
				val notification = notificationFactory.create(currentState.copy(isStopped = true))
				notificationManager.notify(id.hashCode(), notification)
			}
			Result.failure(
				currentState.copy(eta = -1L, isStuck = false).toWorkData(),
			)
		} catch (e: Exception) {
			Log.e(
				"DownloadWorker",
				"doWork failed: workId=$id mangaId=${task.executionMangaId} error=${e.javaClass.simpleName} msg=${e.message}",
				e,
			)
			e.printStackTraceDebug()
			if (settings.isDownloadAutoRetryOnNetworkError && e is IOException) {
				Log.w("DownloadWorker", "Retrying work due to IOException: ${e.message}", e)
				return@withContext Result.retry()
			}
			Result.failure(
				currentState.copy(
					error = e,
					errorMessage = e.getDisplayMessage(applicationContext.resources),
					eta = -1L,
					isStuck = false,
				).toWorkData(),
			)
		} finally {
			ActiveDownloadRegistry.unregister(id)
			try {
				applicationContext.unregisterReceiver(pausingReceiver)
			} catch (_: Exception) {
			}
			notificationManager.cancel(id.hashCode())
		}
	}

	override suspend fun getForegroundInfo() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		ForegroundInfo(
			id.hashCode(),
			notificationFactory.create(lastPublishedState),
			ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
		)
	} else {
		ForegroundInfo(
			id.hashCode(),
			notificationFactory.create(lastPublishedState),
		)
	}

	private suspend fun downloadContentImpl(
		subject: Content,
		resolvedContent: DownloadResolvedContent,
		task: DownloadTask,
		excludedIds: Set<Long>,
	) {
		Log.d("DownloadWorker", "downloadContentImpl start: mangaId=${subject.id} title=${subject.title} excluded=${excludedIds.size}")
		val chaptersToSkip = excludedIds.toMutableSet()
		mangaLock.withLock(subject) {
			var destination = localContentRepository.getOutputDir(subject, task.destination)
			checkNotNull(destination) { applicationContext.getString(R.string.cannot_find_available_storage) }
			Log.d("DownloadWorker", "downloadContentImpl outputDir=${destination.absolutePath}")
			var output: LocalContentOutput? = null
			try {
				val executionManga = resolvedContent.executionManga
				val executionDetails = resolvedContent.executionDetails
				val repo = mangaRepositoryFactory.createWithDiagnostics(executionManga.source).requireAvailableRepository(
					tag = "DownloadWorker",
					prefix = "downloadContentImpl_repository_unavailable",
				) { "Download source ${executionManga.source.name} is not available" }
				Log.d("DownloadWorker", "downloadContentImpl repo=${repo.source.name}")
				Log.d("DownloadWorker", "downloadContentImpl detailsChapters=${executionDetails.chapters?.size ?: 0}")
				val contentType = executionDetails.source.getContentType()

				// 如果包含EPUB章节，强制使用MULTIPLE_CBZ格式
				val downloadFormat = task.format ?: settings.preferredDownloadFormat
				Log.d("DownloadWorker", "downloadContentImpl format=$downloadFormat")

				output = LocalContentOutput.getOrCreate(
					root = destination,
					manga = executionDetails,
					format = downloadFormat,
				)
				val coverUrl = executionDetails.largeCoverUrl.ifNullOrEmpty { executionDetails.coverUrl }
				if (!coverUrl.isNullOrEmpty()) {
					downloadFile(repo, coverUrl, destination, isCover = true).let { file ->
						output.addCover(file, getMediaType(coverUrl, file))
						file.deleteAwait()
					}
				}
				processStandardChapters(executionDetails, task, repo, destination, chaptersToSkip, output)
				publishState(currentState.copy(isIndeterminate = true, eta = -1L, isStuck = false))
				output.mergeWithExisting()
				output.finish()
				val localContent = LocalContentParser(output.rootFile).getContent(withDetails = true)
				// 刷新缓存
				localContentRepository.findSavedContent(executionDetails)
				localStorageChanges.emit(localContent)
				publishState(currentState.copy(localContent = localContent, eta = -1L, isStuck = false, isCompleted = true))
			} catch (e: Exception) {
				Log.e(
					"DownloadWorker",
					"downloadContentImpl failed: mangaId=${subject.id} title=${subject.title} error=${e.javaClass.simpleName} msg=${e.message}",
					e,
				)
				if (e !is CancellationException) {
					publishState(
						currentState.copy(
							error = e,
							errorMessage = e.getDisplayMessage(applicationContext.resources),
						),
					)
				}
				throw e
			} finally {
				withContext(NonCancellable) {
					output?.closeQuietly()
					output?.cleanup()
					val tempFiles = destination.listFiles(TempFileFilter())
					if (tempFiles != null) {
						for (file in tempFiles) {
							runCatchingCancellable { file.deleteAwait() }
						}
					}
				}
			}
		}
	}

	private suspend fun prepareContentImpl(
		subject: Content,
		task: DownloadTask,
	) {
		require(task.kind != DownloadTaskKind.DOWNLOAD) { "Prepare flow cannot use DOWNLOAD task kind" }
		val contentType = subject.source.getContentType()
		mangaLock.withLock(subject) {
			when (contentType) {
				ContentType.NOVEL, ContentType.HENTAI_NOVEL -> {
					error("Novel content is not supported")
				}

				ContentType.VIDEO, ContentType.HENTAI_VIDEO -> {
					error("Video content does not support preparation tasks")
				}

				else -> {
					prepareMangaContent(subject, task)
				}
			}
		}
	}

	private suspend fun prepareMangaContent(
		manga: Content,
		task: DownloadTask,
	) {
		val chapters = getChapters(manga, task)
		for ((chapterIndex, chapter) in chapters.withIndex()) {
			checkIsPaused()
			val pages = loadLocalPages(chapter.value)
			check(pages.isNotEmpty()) { "No local pages found for chapter ${chapter.value.title ?: chapter.value.id}" }
			for ((pageIndex, page) in pages.withIndex()) {
				checkIsPaused()
				publishState(
					currentState.copy(
						totalChapters = chapters.size,
						currentChapter = chapterIndex,
						totalPages = pages.size,
						currentPage = pageIndex,
						isIndeterminate = false,
						eta = -1L,
						isStuck = false,
					),
				)
				val sourceUri = resolvePreparationPageUri(page)
					?: error("Cannot resolve page uri for ${page.url}")
				when (task.kind) {
					DownloadTaskKind.PREPARE_TRANSLATION -> {
						val translationInputUri = prepareTranslationInputUri(sourceUri)
						translationProcessor.process(
							page = page,
							sourceUri = translationInputUri,
							forceEnabled = true,
						)
					}

					DownloadTaskKind.PREPARE_SUPER_RESOLUTION -> {
						superResolutionManager.processImage(
							originalUri = sourceUri,
							modelId = getSuperResolutionModelId(),
							noiseLevel = settings.readerSuperResolutionNoiseLevel,
							cacheLimitMb = settings.readerSuperResolutionCacheLimitMb,
						)
					}

					DownloadTaskKind.DOWNLOAD -> Unit
				}
			}
			publishState(
				currentState.copy(
					downloadedChapters = currentState.downloadedChapters + 1,
				),
			)
		}
		publishState(
			currentState.copy(
				isIndeterminate = false,
				eta = -1L,
				isStuck = false,
				isCompleted = true,
			),
		)
	}

	private suspend fun loadLocalPages(chapter: ContentChapter): List<ContentPage> {
		check(isPreparationChapterEligible(chapter)) {
			"Chapter ${chapter.id} is not downloaded or local"
		}
		return LocalContentParser(chapter.url.toUri()).getPages(chapter)
	}

	private fun isPreparationChapterEligible(chapter: ContentChapter): Boolean {
		return chapter.source.isLocal || isLocalChapterUrl(chapter.url)
	}

	private fun isLocalChapterUrl(url: String): Boolean {
		return url.startsWith("file:") ||
			url.startsWith("zip:") ||
			url.startsWith("file+zip:") ||
			url.startsWith("content:") ||
			url.startsWith("epub:") ||
			url.startsWith("localepub:")
	}

	private suspend fun resolvePreparationPageUri(page: ContentPage): Uri? {
		val uri = page.url.toUri()
		return when {
			uri.isFileUri() -> uri
			uri.isZipUri() -> cacheZipPage(uri)
			uri.scheme == "content" || uri.scheme == "android.resource" -> cacheContentUri(page.url, uri)
			uri.scheme == "data" -> cacheDataUri(page.url)
			else -> null
		}
	}

	private suspend fun cacheZipPage(uri: Uri): Uri? {
		cache[uri.toString()]?.let { return it.toUri() }
		return runCatching {
			val zipFile = when (uri.scheme) {
				"file+zip" -> File(uri.host.orEmpty() + uri.path.orEmpty())
				else -> File(uri.schemeSpecificPart)
			}
			ZipFile(zipFile).use { zip ->
				val entry = zip.getEntry(uri.fragment) ?: return@runCatching null
				BitmapDecoderCompat.decode(
					zip.getInputStream(entry),
					MimeTypes.getMimeTypeFromExtension(entry.name),
				)
			}.use { image ->
				cache.set(uri.toString(), image).toUri()
			}
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrNull()
	}

	private suspend fun cacheContentUri(cacheKey: String, uri: Uri): Uri? {
		cache[cacheKey]?.let { return it.toUri() }
		return runCatching {
			val type = applicationContext.contentResolver.getType(uri)?.toMimeTypeOrNull()
			applicationContext.contentResolver.openInputStream(uri)?.use { input ->
				cache.set(cacheKey, input.source(), type).toUri()
			}
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrNull()
	}

	private suspend fun cacheDataUri(dataUrl: String): Uri? {
		cache[dataUrl]?.let { return it.toUri() }
		return runCatching {
			val commaIndex = dataUrl.indexOf(',')
			check(commaIndex != -1) { "Invalid data URL" }
			val header = dataUrl.substring(0, commaIndex)
			val data = dataUrl.substring(commaIndex + 1)
			val isBase64 = header.contains(";base64")
			val contentType = header.substringAfter("data:").substringBefore(";").toMimeTypeOrNull()
			val bytes = if (isBase64) {
				Base64.getDecoder().decode(data)
			} else {
				URLDecoder.decode(data, "UTF-8").toByteArray()
			}
			cache.set(dataUrl, bytes.inputStream().source(), contentType).toUri()
		}.onFailure {
			it.printStackTraceDebug()
		}.getOrNull()
	}

	private suspend fun prepareTranslationInputUri(sourceUri: Uri): Uri {
		if (!settings.isReaderSuperResolutionEnabled) {
			return sourceUri
		}
		return superResolutionManager.processImage(
			originalUri = sourceUri,
			modelId = getSuperResolutionModelId(),
			noiseLevel = settings.readerSuperResolutionNoiseLevel,
			cacheLimitMb = settings.readerSuperResolutionCacheLimitMb,
		) ?: sourceUri
	}

	private fun getSuperResolutionModelId(): String {
		return if (settings.readerSuperResolutionEngine == "ANIME4K") {
			settings.readerSuperResolutionAnime4kMode
		} else {
			settings.readerSuperResolutionModel
		}
	}

	private suspend fun processStandardChapters(
		mangaDetails: Content,
		task: DownloadTask,
		repo: ContentRepository,
		destination: File,
		chaptersToSkip: MutableSet<Long>,
		output: LocalContentOutput,
	) {
		val chapters = getChapters(mangaDetails, task)
		Log.d("DownloadWorker", "processStandardChapters total=${chapters.size} mangaId=${mangaDetails.id}")
		for ((chapterIndex, chapter) in chapters.withIndex()) {
			checkIsPaused()

			val fullChapters = mangaDetails.chapters ?: emptyList()
			val currentInFull = fullChapters.indexOfFirst { it.id == chapter.value.id }
			val nextChapterUrl = if (currentInFull != -1) fullChapters.getOrNull(currentInFull + 1)?.url else null
			val pages = runFailsafe {
				repo.getPages(chapter.value, nextChapterUrl)
			} ?: continue
			if (pages.isEmpty()) {
				Log.d("DownloadWorker", "processStandardChapters empty pages: idx=$chapterIndex title=${chapter.value.title}")
			}

			println("DownloadWorker: Chapter ${chapter.index}: ${chapter.value.title}")
			println("DownloadWorker: Pages count: ${pages.size}")
			if (pages.isNotEmpty()) {
				println("DownloadWorker: First page preview: ${pages[0].preview}")
				println("DownloadWorker: First page url: ${pages[0].url}")
			}

			val isEpubChapter = pages.size == 1 && pages[0].preview == "EPUB"
			if (!isEpubChapter && chaptersToSkip.remove(chapter.value.id)) {
				println("DownloadWorker: Skipping already downloaded chapter")
				publishState(currentState.copy(downloadedChapters = currentState.downloadedChapters + 1))
				continue
			}

			val tempDir = File(destination, "tmp_${chapter.value.id}")
			if (!tempDir.exists()) {
				tempDir.mkdirs()
			}

			val pageCounter = AtomicInteger(0)
			val successCounter = AtomicInteger(0)
			channelFlow {
				val downloadThreads = if (settings.isDownloadAlignedWithReader) {
					settings.readerThreads
				} else {
					settings.downloadThreads
				}
				val semaphore = Semaphore(downloadThreads)
				for ((pageIndex, page) in pages.withIndex()) {
					checkIsPaused()
					launch {
						semaphore.withPermit {
							val success = runFailsafe {
								val prefix = String.format("%04d.", pageIndex)
								val existingFile = tempDir.listFiles { _, name -> name.startsWith(prefix) }?.firstOrNull()
								val file = if (existingFile != null && existingFile.length() > 0) {
									existingFile
								} else {
									val url = repo.getPageUrl(page)
									val downloadedFile = cache[url]
										?: downloadFile(repo, url, destination, page = page)
									val ext = downloadedFile.extension.takeIf { it != "tmp" } ?: "jpg"
									val targetFile = File(tempDir, prefix + ext)
									downloadedFile.copyTo(targetFile, overwrite = true)
									if (downloadedFile.extension == "tmp") {
										downloadedFile.deleteAwait()
									}
									targetFile
								}
								output.addPage(
									chapter = chapter,
									file = file,
									pageNumber = pageIndex,
									type = getMediaType(file.name, file),
								)
								true
							} ?: false
							if (success) {
								successCounter.incrementAndGet()
								send(pageIndex)
							}
						}
					}
				}
			}.map {
				DownloadProgress(
					totalChapters = chapters.size,
					currentChapter = chapterIndex,
					totalPages = pages.size,
					currentPage = pageCounter.getAndIncrement(),
				)
			}.withTicker(2L, TimeUnit.SECONDS).collect { progress ->
				publishState(
					currentState.copy(
						totalChapters = progress.totalChapters,
						currentChapter = progress.currentChapter,
						totalPages = progress.totalPages,
						currentPage = progress.currentPage,
						isIndeterminate = false,
						eta = etaEstimator.getEta(),
						isStuck = etaEstimator.isStuck(),
					),
				)
			}
			if (successCounter.get() == 0) {
				throw IOException("No pages downloaded for chapter: ${chapter.value.title ?: chapter.value.id}")
			}
			if (output.flushChapter(chapter.value)) {
				tempDir.deleteRecursively()
				runCatchingCancellable {
					localStorageChanges.emit(LocalContentParser(output.rootFile).getContent(withDetails = false))
				}.onFailure(Throwable::printStackTraceDebug)
			}
			publishState(currentState.copy(downloadedChapters = currentState.downloadedChapters + 1))
		}
	}

	private suspend fun <R> runFailsafe(
		block: suspend () -> R,
	): R? {
		checkIsPaused()
		val maxAttempts = settings.downloadRetryCount
		var countDown = maxAttempts
		failsafe@ while (true) {
			try {
				return block()
			} catch (e: IOException) {
				val retryDelay = if (e is TooManyRequestExceptions) {
					e.getRetryDelay()
				} else {
					settings.downloadRetryDelayMs.toLong()
				}
				Log.w(
					"DownloadWorker",
					"runFailsafe failed: ${e.javaClass.simpleName} msg=${e.message} retryDelay=$retryDelay remaining=$countDown",
					e,
				)
				if (settings.isDownloadAutoRetryOnNetworkError && e !is TooManyRequestExceptions && countDown <= 0) {
					throw e
				}
				if (countDown <= 0 || retryDelay < 0 || retryDelay > MAX_RETRY_DELAY) {
					val pausingHandle = PausingHandle.current()
					if (pausingHandle.skipAllErrors()) {
						return null
					}
					publishState(
						currentState.copy(
							isPaused = true,
							error = e,
							errorMessage = e.getDisplayMessage(applicationContext.resources),
							eta = -1L,
							isStuck = false,
						),
					)
					countDown = maxAttempts
					pausingHandle.pause()
					try {
						pausingHandle.awaitResumed()
						if (pausingHandle.skipCurrentError()) {
							return null
						}
					} finally {
						publishState(currentState.copy(isPaused = false, error = null, errorMessage = null))
					}
				} else {
					countDown--
					delay(retryDelay)
				}
			}
		}
	}

	private suspend fun checkIsPaused() {
		val pausingHandle = PausingHandle.current()
		while (true) {
			if (pausingHandle.isPaused) {
				publishState(currentState.copy(isPaused = true, eta = -1L, isStuck = false))
				try {
					pausingHandle.awaitResumed()
				} finally {
					publishState(currentState.copy(isPaused = false))
				}
			}
			val limit = settings.downloadMaxActiveSeries
			if (ActiveDownloadRegistry.isTurn(id, limit)) {
				break
			}
			delay(1000)
		}
	}

	private suspend fun getMediaType(url: String, file: File): MimeType? = runInterruptible(Dispatchers.IO) {
		BitmapDecoderCompat.probeMimeType(file)?.let {
			return@runInterruptible it
		}
		MimeTypes.getMimeTypeFromUrl(url)
	}

	/**
	 * 小说章节下载：复用漫画的输出格式（单本/多本 CBZ），章节内写入 HTML + 插图。
	 */

	private suspend fun downloadFile(
		repo: ContentRepository,
		url: String,
		destination: File,
		useProxy: Boolean = true,
		headers: Map<String, String> = emptyMap(),
		page: ContentPage? = null,
		isCover: Boolean = false,
	): File {
		if (url.startsWith("data:", ignoreCase = true)) {
			val data = url.removePrefix("data:")
			val commaIndex = data.indexOf(',')
			require(commaIndex >= 0) { "Invalid data URL: missing comma separator" }
			val meta = data.substring(0, commaIndex)
			val contentPart = data.substring(commaIndex + 1)
			val isBase64 = meta.contains(";base64", ignoreCase = true)
			val mimeType = meta.substringBefore(';').takeIf { it.isNotBlank() }?.toMimeTypeOrNull()
			val ext = MimeTypes.getExtension(mimeType)
			val bytes = if (isBase64) {
				Base64.getDecoder().decode(contentPart)
			} else {
				URLDecoder.decode(contentPart, "UTF-8").toByteArray(Charsets.UTF_8)
			}
			val file = destination.createTempFile(ext)
			file.sink(append = false).buffer().use { sink ->
				sink.write(bytes)
			}
			return file
		}
		if (url.startsWith("content:", ignoreCase = true) || url.startsWith("file:", ignoreCase = true)) {
			val uri = url.toUri()
			val cr = applicationContext.contentResolver
			val ext = uri.toFileOrNull()?.let {
				MimeTypes.getNormalizedExtension(it.name)
			} ?: cr.getType(uri)?.toMimeTypeOrNull()?.let { MimeTypes.getExtension(it) }
			val file = destination.createTempFile(ext)
			try {
				cr.openSource(uri).use { input ->
					file.sink(append = false).buffer().use {
						it.writeAllCancellable(input)
					}
				}
			} catch (e: Exception) {
				file.delete()
				throw e
			}
			return file
		}
		if (url.startsWith("zip:", ignoreCase = true) || url.startsWith("file+zip:", ignoreCase = true)) {
			val uri = url.toUri()
			val zipFile = when (uri.scheme) {
				"zip" -> File(uri.schemeSpecificPart)
				"file+zip" -> File(uri.host.orEmpty() + uri.path.orEmpty())
				else -> throw IllegalArgumentException("Unsupported scheme: ${uri.scheme}")
			}
			val fragment = uri.fragment ?: ""
			val ext = MimeTypes.getNormalizedExtension(fragment)
			val file = destination.createTempFile(ext)
			try {
				runInterruptible(Dispatchers.IO) {
					java.util.zip.ZipFile(zipFile).use { zip ->
						val entry = checkNotNull(zip.getEntry(fragment)) {
							"Zip entry not found: $fragment in ${zipFile.absolutePath}"
						}
						zip.getInputStream(entry).use { input ->
							file.outputStream().use { output ->
								input.copyTo(output)
							}
						}
					}
				}
			} catch (e: Exception) {
				file.delete()
				throw e
			}
			return file
		}

		val request = when {
			page != null -> repo.createPageRequest(url, page)
			isCover -> repo.createCoverRequest(url)
			else -> com.mangaverse.app.reader.domain.PageLoader.createPageRequest(url, repo.source)
		}

		val requestBuilder = request.newBuilder()
		headers.forEach { (k, v) -> requestBuilder.header(k, v) }
		val finalRequest = requestBuilder.build()

		slowdownDispatcher.delay(repo.source)
		val response = if (useProxy) {
			imageProxyInterceptor.interceptPageRequest(finalRequest, okHttp)
		} else {
			okHttp.newCall(finalRequest).await()
		}
		return response
			.ensureSuccess()
			.use { response ->
				var file: File? = null
				try {
					val body = response.body ?: error("Response body is null")
					body.use {
						file = destination.createTempFile(
							ext = MimeTypes.getExtension(body.contentType()?.toMimeType())
						)
						file.sink(append = false).buffer().use { sink ->
							sink.writeAllCancellable(body.source())
						}
					}
				} catch (e: Exception) {
					file?.delete()
					throw e
				}
				checkNotNull(file)
			}
	}

	private fun File.createTempFile(ext: String?): File {
		// Ensure parent directory exists
		if (!exists()) {
			mkdirs()
		}
		return File(
			this,
			buildString {
				append(UUID.randomUUID().toString())
				if (!ext.isNullOrEmpty()) {
					append('.')
					append(ext)
				}
				append(".tmp")
			},
		)
	}

	/**
	 * 下载EPUB章节
	 * 
	 * EPUB本质上是ZIP格式，保存为.epub文件以符合标准
	 * 
	 * 特殊处理：
	 * - 对于LocalContentDirOutput：使用addEpubChapter直接保存EPUB
	 * - 对于LocalContentZipOutput：会导致ZIP嵌套（暂不支持）
	 * 
	 * Requirements: 1.1, 1.2, 1.3, 1.4
	 * - 1.1: Save with .epub extension
	 * - 1.2: Preserve EPUB format without converting to CBZ
	 * - 1.3: Store in dedicated EPUB directory
	 * - 1.4: Generate unique filename using parent chapter ID
	 */
	private suspend fun publishState(state: DownloadState) {
		val previousState = currentState
		lastPublishedState = state
		if (previousState.isParticularProgress && state.isParticularProgress) {
			etaEstimator.onProgressChanged(state.progress, state.max)
		} else {
			etaEstimator.reset()
			notificationThrottler.reset()
		}
		val notification = notificationFactory.create(state)
		if (state.isFinalState) {
			if (!notificationFactory.isSilent) {
				notificationManager.notify(id.toString(), id.hashCode(), notification)
			}
		} else if (notificationThrottler.throttle()) {
			notificationManager.notify(id.hashCode(), notification)
		} else {
			return
		}
		setProgress(state.toWorkData())
	}

	private suspend fun publishExecutionDetailsState(executionDetails: Content) {
		val state = currentState
		if (state.manga == executionDetails) {
			return
		}
		publishState(
			state.copy(
				manga = executionDetails,
			),
		)
	}

	private fun scanDownloadedFile(file: File) {
		runCatching {
			MediaScannerConnection.scanFile(
				applicationContext,
				arrayOf(file.absolutePath),
				null,
				null,
			)
		}.onFailure { e ->
			Log.w("DownloadWorker", "scanDownloadedFile failed: ${file.absolutePath}", e)
		}
	}

	private suspend fun getDoneChapters(manga: Content) = runCatchingCancellable {
		val start = System.currentTimeMillis()
		val result = withTimeoutOrNull(3000L) {
			localContentRepository.getDetails(manga).chapters
				?.filter { it.source.isLocal }
				?.ids()
		}
		if (result == null) {
			Log.w(
				"DownloadWorker",
				"getDoneChapters timeout: mangaId=${manga.id} title=${manga.title}",
			)
			emptySet()
		} else {
			Log.i(
				"DownloadWorker",
				"getDoneChapters success: mangaId=${manga.id} took=${System.currentTimeMillis() - start}ms count=${result.size}",
			)
			result
		}
	}.onFailure { e ->
		Log.w(
			"DownloadWorker",
			"getDoneChapters failed: mangaId=${manga.id} title=${manga.title} error=${e.javaClass.simpleName} msg=${e.message}",
			e,
		)
	}.getOrNull().orEmpty()

	private fun getChapters(
		manga: Content,
		task: DownloadTask,
	): List<IndexedValue<ContentChapter>> {
		val chapters = checkNotNull(manga.chapters) { "Chapters list must not be null" }
		val requestedChapterIds = task.executionChapterIds
		val chaptersIdsSet = requestedChapterIds?.toMutableSet()
		val result = ArrayList<IndexedValue<ContentChapter>>((chaptersIdsSet ?: chapters).size)
		val counters = HashMap<String?, Int>()
		for (chapter in chapters) {
			val index = counters[chapter.branch] ?: 0
			counters[chapter.branch] = index + 1
			if (chaptersIdsSet != null && !chaptersIdsSet.remove(chapter.id)) {
				continue
			}
			result.add(IndexedValue(index, chapter))
		}
		if (chaptersIdsSet != null) {
			resolveMissingExecutionChapters(
				chapters = chapters,
				requestedChapterIds = requestedChapterIds ?: LongArray(0),
				requestedChapterRefs = task.executionChapterRefs.orEmpty(),
				missingChapterIds = chaptersIdsSet,
				result = result,
				counters = counters,
			)
			check(chaptersIdsSet.isEmpty()) {
				"${chaptersIdsSet.size} of ${task.executionChapterIds?.size ?: 0} requested chapters not found in manga"
			}
		}
		check(result.isNotEmpty()) { "Chapters list must not be empty" }
		return result.sortedWith(compareBy<IndexedValue<ContentChapter>> { it.index }.thenBy { it.value.number }.thenBy { it.value.id })
	}

	private fun resolveMissingExecutionChapters(
		chapters: List<ContentChapter>,
		requestedChapterIds: LongArray,
		requestedChapterRefs: List<ExecutionChapterRef>,
		missingChapterIds: MutableSet<Long>,
		result: MutableList<IndexedValue<ContentChapter>>,
		counters: MutableMap<String?, Int>,
	) {
		if (missingChapterIds.isEmpty()) {
			return
		}
		val usedChapterIds = result.mapTo(mutableSetOf()) { it.value.id }
		val requestedChapterRefsById = requestedChapterRefs.associateBy { it.id }
		for (requestedChapterId in requestedChapterIds) {
			if (!missingChapterIds.contains(requestedChapterId)) {
				continue
			}
			val requestedChapter = requestedChapterRefsById[requestedChapterId] ?: continue
			val matchedChapter = chapters.firstOrNull { candidate ->
				candidate.id !in usedChapterIds && chapterExecutionIdentityMatches(requestedChapter, candidate)
			} ?: continue
			val branchIndex = counters.getOrPut(matchedChapter.branch) { 0 }
			counters[matchedChapter.branch] = branchIndex + 1
			result.add(IndexedValue(branchIndex, matchedChapter))
			usedChapterIds += matchedChapter.id
			missingChapterIds.remove(requestedChapterId)
			Log.w(
				"DownloadWorker",
				"getChapters: remapped executionChapterId=$requestedChapterId to chapterId=${matchedChapter.id} " +
					"title=${matchedChapter.title} branch=${matchedChapter.branch}",
			)
		}
	}

	private fun chapterExecutionIdentityMatches(
		requested: ExecutionChapterRef,
		candidate: ContentChapter,
	): Boolean {
		if (requested.branch != candidate.branch) {
			return false
		}
		if (requested.url.isNotBlank() && requested.url == candidate.url) {
			return true
		}
		val sameTitle = requested.title?.takeIf { it.isNotBlank() } == candidate.title?.takeIf { it.isNotBlank() }
		if (sameTitle && requested.number > 0f && candidate.number > 0f && requested.number == candidate.number) {
			return true
		}
		if (sameTitle && requested.volume > 0 && candidate.volume > 0 && requested.volume == candidate.volume) {
			return true
		}
		return requested.number > 0f &&
			candidate.number > 0f &&
			requested.number == candidate.number &&
			requested.volume == candidate.volume
	}

	@Reusable
	class Scheduler @Inject constructor(
		@ApplicationContext private val context: Context,
		private val mangaDataRepository: ContentDataRepository,
		private val workManager: WorkManager,
	) {

		fun observeWorks(): Flow<List<WorkInfo>> = workManager
			.getWorkInfosByTagFlow(TAG)

		@SuppressLint("RestrictedApi")
		suspend fun getInputData(id: UUID): Data? {
			val spec = workManager.getWorkSpec(id) ?: return null
			return Data.Builder()
				.putAll(spec.input)
				.putLong(DownloadState.DATA_TIMESTAMP, spec.scheduleRequestedAt)
				.build()
		}

		suspend fun getTask(workId: UUID): DownloadTask? {
			return workManager.getWorkInputData(workId)?.let { DownloadTask(it) }
		}

		suspend fun cancel(id: UUID) {
			workManager.cancelWorkById(id).await()
		}

		suspend fun cancelAll() {
			workManager.cancelAllWorkByTag(TAG).await()
		}

		fun pause(id: UUID) = context.sendBroadcast(
			PausingReceiver.getPauseIntent(context, id),
		)

		fun resume(id: UUID) = context.sendBroadcast(
			PausingReceiver.getResumeIntent(context, id),
		)

		fun skip(id: UUID) = context.sendBroadcast(
			PausingReceiver.getSkipIntent(context, id),
		)

		fun skipAll(id: UUID) = context.sendBroadcast(
			PausingReceiver.getSkipAllIntent(context, id),
		)

		suspend fun delete(id: UUID) {
			workManager.cancelWorkById(id).await()
			workManager.deleteWorks(listOf(id))
		}

		suspend fun delete(ids: Collection<UUID>) {
			val wm = workManager
			ids.forEach { id -> wm.cancelWorkById(id).await() }
			workManager.deleteWorks(ids)
		}

		suspend fun removeCompleted() {
			val finishedWorks = workManager.awaitFinishedWorkInfosByTag(TAG)
			workManager.deleteWorks(finishedWorks.mapToSet { it.id })
		}

		suspend fun updateConstraints(allowMeteredNetwork: Boolean) {
			val constraints = createConstraints(allowMeteredNetwork)
			val works = workManager.awaitWorkInfosByTag(TAG)
			for (work in works) {
				if (work.state.isFinished) {
					continue
				}
				val request = OneTimeWorkRequestBuilder<DownloadWorker>()
					.setConstraints(constraints)
					.addTag(TAG)
					.setId(work.id)
					.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
					.build()
				workManager.awaitUpdateWork(request)
			}
		}

		suspend fun schedule(tasks: Collection<Pair<Content, DownloadTask>>) {
			if (tasks.isEmpty()) {
				return
			}
			val requests = tasks.map { (manga, task) ->
				val storedManga = mangaDataRepository.storeContentAndReturn(manga, replaceExisting = true)
				val currentManga = mangaDataRepository.findContentById(storedManga.id, withChapters = true) ?: storedManga
				val displayManga = if (task.displayMangaId != null && task.displayMangaId != task.executionMangaId) {
					mangaDataRepository.findDisplayContentById(task.displayMangaId, withChapters = false)
				} else {
					mangaDataRepository.findDisplayContentById(currentManga.id, withChapters = false)
				}
				val storedDisplayManga = displayManga
					?.takeIf { it.id != currentManga.id }
					?.let { representativeManga ->
						mangaDataRepository.storeContentAndReturn(representativeManga, replaceExisting = false)
					}
				val displayMangaId = storedDisplayManga?.id ?: displayManga?.id ?: currentManga.id
				val normalizedTask = DownloadTask.createExecutionTask(
					executionMangaId = currentManga.id,
					displayMangaId = displayMangaId,
					isPaused = task.isPaused,
					isSilent = task.isSilent,
					executionChapterIds = task.executionChapterIds,
					executionChapterRefs = task.executionChapterRefs,
					destination = task.destination,
					format = task.format,
					allowMeteredNetwork = task.allowMeteredNetwork,
					preferredQuality = task.preferredQuality,
					kind = task.kind,
				)
				OneTimeWorkRequestBuilder<DownloadWorker>()
					.setConstraints(createConstraints(task.allowMeteredNetwork))
					.addTag(TAG)
					.keepResultsForAtLeast(30, TimeUnit.DAYS)
					.setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
					.setInputData(normalizedTask.toData())
					.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
					.build()
			}
			workManager.enqueue(requests).await()
		}

		private fun createConstraints(allowMeteredNetwork: Boolean) = Constraints.Builder()
			.setRequiredNetworkType(if (allowMeteredNetwork) NetworkType.CONNECTED else NetworkType.UNMETERED)
			.build()
	}

	/**
	 * 验证文件是否为有效的EPUB/ZIP文件
	 * EPUB文件本质上是ZIP格式，magic bytes应该是 PK (0x50 0x4B)
	 */
	private fun isValidEpubFile(file: File): Boolean {
		if (!file.exists() || file.length() < 4) {
			return false
		}
		
		return try {
			file.inputStream().use { input ->
				val header = ByteArray(4)
				val read = input.read(header)
				if (read < 2) return false
				
				// ZIP/EPUB magic bytes: PK\x03\x04 (0x50 0x4B 0x03 0x04)
				header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
			}
		} catch (e: Exception) {
			false
		}
	}

	/**
	 * 读取文件头部用于调试
	 */
	private fun readFileHead(file: File, maxBytes: Int): String {
		if (!file.exists()) return "[File does not exist]"
		
		return try {
			file.inputStream().use { input ->
				val bytes = ByteArray(minOf(maxBytes, file.length().toInt()))
				input.read(bytes)
				
				// 尝试作为文本读取（如果是HTML错误页）
				val text = String(bytes, Charsets.UTF_8)
				if (text.contains("<!DOCTYPE", ignoreCase = true) || 
				    text.contains("<html", ignoreCase = true)) {
					"[HTML detected] $text"
				} else {
					// 显示hex dump
					bytes.joinToString(" ") { "%02X".format(it) }
				}
			}
		} catch (e: Exception) {
			"[Error reading file: ${e.message}]"
		}
	}

	private companion object {

		const val MAX_RETRY_DELAY = 7_200_000L // 2 hours
		const val TAG = "download"
		private const val PAGE_NAME_PATTERN = "%08d_%04d%04d"
	}

	@AssistedFactory
	interface Factory : WorkerAssistedFactory<DownloadWorker>
}
