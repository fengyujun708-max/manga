package com.mangaverse.app.settings.userdata.storage

import android.annotation.SuppressLint
import android.webkit.WebStorage
import androidx.webkit.WebStorageCompat
import androidx.webkit.WebViewFeature
import coil3.ImageLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runInterruptible
import okhttp3.Cache
import com.mangaverse.app.R
import com.mangaverse.app.core.model.LocalMangaSource
import com.mangaverse.app.core.network.cookies.MutableCookieJar
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.ui.util.ReversibleAction
import com.mangaverse.app.core.util.ext.MutableEventFlow
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.local.data.CacheDir
import com.mangaverse.app.local.data.LocalStorageManager
import com.mangaverse.app.local.data.StorageContentKind
import com.mangaverse.app.local.data.LocalMangaRepository
import com.mangaverse.app.local.domain.DeleteReadChaptersUseCase
import com.mangaverse.app.search.domain.ContentSearchRepository
import com.mangaverse.app.tracker.domain.TrackingRepository
import java.util.EnumMap
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class DataCleanupSettingsViewModel @Inject constructor(
    private val storageManager: LocalStorageManager,
    private val httpCache: Cache,
    private val searchRepository: ContentSearchRepository,
    private val trackingRepository: TrackingRepository,
    private val cookieJar: MutableCookieJar,
    private val deleteReadChaptersUseCase: DeleteReadChaptersUseCase,
    private val localContentRepository: LocalMangaRepository,
    private val mangaDataRepositoryProvider: Provider<ContentDataRepository>,
	    private val coil: ImageLoader,
) : BaseViewModel() {

    data class LocalContentCleanupResult(
        val kind: StorageContentKind,
        val removedCount: Int,
        val bytesFreed: Long,
    )

    val onActionDone = MutableEventFlow<ReversibleAction>()
    val onStorageChanged = MutableEventFlow<Unit>()
    val onLocalContentCleanedUp = MutableEventFlow<LocalContentCleanupResult>()
    val loadingKeys = MutableStateFlow(emptySet<String>())

    val searchHistoryCount = MutableStateFlow(-1)
    val feedItemsCount = MutableStateFlow(-1)
    val httpCacheSize = MutableStateFlow(-1L)
    val cacheSizes = EnumMap<CacheDir, MutableStateFlow<Long>>(CacheDir::class.java)

    val onChaptersCleanedUp = MutableEventFlow<Pair<Int, Long>>()

    val isBrowserDataCleanupEnabled: Boolean
        get() = WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA)

    init {
        CacheDir.entries.forEach {
            cacheSizes[it] = MutableStateFlow(-1L)
        }
        launchJob(Dispatchers.Default) {
            searchHistoryCount.value = searchRepository.getSearchHistoryCount()
        }
        launchJob(Dispatchers.Default) {
            feedItemsCount.value = trackingRepository.getLogsCount()
        }
        CacheDir.entries.forEach { cache ->
            launchJob(Dispatchers.Default) {
                checkNotNull(cacheSizes[cache]).value = storageManager.computeCacheSize(cache)
            }
        }
        launchJob(Dispatchers.Default) {
            httpCacheSize.value = runInterruptible { httpCache.size() }
        }
    }

    fun clearCache(key: String, vararg caches: CacheDir) {
        launchJob(Dispatchers.Default) {
            try {
                loadingKeys.update { it + key }
                for (cache in caches) {
                    storageManager.clearCache(cache)
                    checkNotNull(cacheSizes[cache]).value = storageManager.computeCacheSize(cache)
                    if (cache == CacheDir.THUMBS) {
                        coil.memoryCache?.clear()
                    }
                }
                onStorageChanged.call(Unit)
            } finally {
                loadingKeys.update { it - key }
            }
        }
    }

    fun clearTorrentCache() {
        launchJob(Dispatchers.Default) {
            try {
                loadingKeys.update { it + AppSettings.KEY_TORRENT_CACHE_CLEAR }
                storageManager.clearCache(CacheDir.TORRENT)
                checkNotNull(cacheSizes[CacheDir.TORRENT]).value = storageManager.computeCacheSize(CacheDir.TORRENT)
                onStorageChanged.call(Unit)
            } finally {
                loadingKeys.update { it - AppSettings.KEY_TORRENT_CACHE_CLEAR }
            }
        }
    }

    fun clearHttpCache() {
        launchJob(Dispatchers.Default) {
            try {
                loadingKeys.update { it + AppSettings.KEY_HTTP_CACHE_CLEAR }
                val size = runInterruptible(Dispatchers.IO) {
                    httpCache.evictAll()
                    httpCache.size()
                }
                httpCacheSize.value = size
                onStorageChanged.call(Unit)
            } finally {
                loadingKeys.update { it - AppSettings.KEY_HTTP_CACHE_CLEAR }
            }
        }
    }

    fun clearSearchHistory() {
        launchJob(Dispatchers.Default) {
            searchRepository.clearSearchHistory()
            searchHistoryCount.value = searchRepository.getSearchHistoryCount()
            onActionDone.call(ReversibleAction(R.string.search_history_cleared, null))
        }
    }

    fun clearCookies() {
        launchJob {
            cookieJar.clear()
            onActionDone.call(ReversibleAction(R.string.cookies_cleared, null))
        }
    }

    @SuppressLint("RequiresFeature")
    fun clearBrowserData() {
        launchJob {
            try {
                loadingKeys.update { it + AppSettings.KEY_WEBVIEW_CLEAR }
                val storage = WebStorage.getInstance()
                suspendCoroutine { cont ->
                    WebStorageCompat.deleteBrowsingData(storage) {
                        cont.resume(Unit)
                    }
                }
                onActionDone.call(ReversibleAction(R.string.updates_feed_cleared, null))
                onStorageChanged.call(Unit)
            } finally {
                loadingKeys.update { it - AppSettings.KEY_WEBVIEW_CLEAR }
            }
        }
    }

    fun clearUpdatesFeed() {
        launchJob(Dispatchers.Default) {
            try {
                loadingKeys.update { it + AppSettings.KEY_UPDATES_FEED_CLEAR }
                trackingRepository.clearLogs()
                feedItemsCount.value = trackingRepository.getLogsCount()
                onActionDone.call(ReversibleAction(R.string.updates_feed_cleared, null))
            } finally {
                loadingKeys.update { it - AppSettings.KEY_UPDATES_FEED_CLEAR }
            }
        }
    }

    fun clearContentData() {
        launchJob(Dispatchers.Default) {
            try {
                loadingKeys.update { it + AppSettings.KEY_CLEAR_MANGA_DATA }
                trackingRepository.gc()
                val repository = mangaDataRepositoryProvider.get()
                repository.cleanupLocalContent()
                repository.cleanupDatabase()
                onActionDone.call(ReversibleAction(R.string.updates_feed_cleared, null))
                onStorageChanged.call(Unit)
            } finally {
                loadingKeys.update { it - AppSettings.KEY_CLEAR_MANGA_DATA }
            }
        }
    }

    fun clearLocalMangaContent() = clearLocalContent(
        key = AppSettings.KEY_LOCAL_MANGA_CLEAR,
        kind = StorageContentKind.MANGA,
        sourceName = LocalMangaSource.name,
    )

    fun clearLocalNovelContent() = clearLocalContent(
        key = AppSettings.KEY_LOCAL_NOVELS_CLEAR,
        kind = StorageContentKind.NOVEL,
        sourceName = LocalMangaSource.name,
    )

    fun clearLocalVideoContent() = clearLocalContent(
        key = AppSettings.KEY_LOCAL_VIDEOS_CLEAR,
        kind = StorageContentKind.VIDEO,
        sourceName = LocalMangaSource.name,
    )

    fun cleanupChapters() {
        launchJob(Dispatchers.Default) {
            try {
                loadingKeys.update { it + AppSettings.KEY_CHAPTERS_CLEAR }
                val oldSize = storageManager.computeStorageSize()
                val chaptersCount = deleteReadChaptersUseCase.invoke()
                val newSize = storageManager.computeStorageSize()
                onChaptersCleanedUp.call(chaptersCount to oldSize - newSize)
                onStorageChanged.call(Unit)
            } finally {
                loadingKeys.update { it - AppSettings.KEY_CHAPTERS_CLEAR }
            }
        }
    }

    private fun clearLocalContent(
        key: String,
        kind: StorageContentKind,
        sourceName: String,
    ) {
        launchJob(Dispatchers.Default) {
            try {
                loadingKeys.update { it + key }
                val beforeSize = storageManager.computeStorageSize(kind)
                val localItems = localContentRepository.getRawListAsFlow()
                    .toList()
                    .filter { it.manga.source?.name == sourceName }
                var removedCount = 0
                localItems.forEach { item ->
                    if (localContentRepository.delete(item.manga)) {
                        removedCount++
                    }
                }
                mangaDataRepositoryProvider.get().cleanupLocalContent()
                val afterSize = storageManager.computeStorageSize(kind)
                onLocalContentCleanedUp.call(
                    LocalContentCleanupResult(
                        kind = kind,
                        removedCount = removedCount,
                        bytesFreed = (beforeSize - afterSize).coerceAtLeast(0L),
                    ),
                )
                onStorageChanged.call(Unit)
            } finally {
                loadingKeys.update { it - key }
            }
        }
    }
}
