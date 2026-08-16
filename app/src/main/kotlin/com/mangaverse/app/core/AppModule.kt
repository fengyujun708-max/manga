package com.mangaverse.app.core

import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.SearchRecentSuggestions
import android.text.Html
import androidx.collection.arraySetOf
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.room.InvalidationTracker
import androidx.work.Configuration
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowRgb565
import coil3.svg.SvgDecoder
import coil3.util.DebugLogger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import com.mangaverse.app.BuildConfig
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.exceptions.resolve.CaptchaHandler
import com.mangaverse.app.core.image.AvifImageDecoder
import com.mangaverse.app.core.image.CbzFetcher
import com.mangaverse.app.core.image.ContentSourceHeaderInterceptor
import com.mangaverse.app.core.image.ImageFailureSuppressingInterceptor
import com.mangaverse.app.core.image.SuppressingCoilLogger
import com.mangaverse.app.core.network.ContentHttpClient
import com.mangaverse.app.core.network.imageproxy.ImageProxyInterceptor
import com.mangaverse.app.core.os.AppShortcutManager
import com.mangaverse.app.core.os.NetworkState
import com.mangaverse.app.core.parser.ContentLoaderContextImpl
import com.mangaverse.app.core.parser.favicon.FaviconFetcher
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.ui.image.CoilImageGetter
import com.mangaverse.app.core.ui.util.ActivityRecreationHandle
import com.mangaverse.app.core.ui.util.ForegroundActivityHolder
import com.mangaverse.app.core.util.AcraScreenLogger
import com.mangaverse.app.core.util.FileSize
import com.mangaverse.app.core.util.ext.connectivityManager
import com.mangaverse.app.core.util.ext.isLowRamDevice
import com.mangaverse.app.details.ui.pager.pages.ContentPageFetcher
import com.mangaverse.app.details.ui.pager.pages.ContentPageKeyer
import com.mangaverse.app.local.data.CacheDir
import com.mangaverse.app.local.data.FaviconCache
import com.mangaverse.app.local.data.LocalStorageCache
import com.mangaverse.app.local.data.LocalStorageChanges
import com.mangaverse.app.local.data.NovelCache
import com.mangaverse.app.local.data.PageCache
import com.mangaverse.app.local.domain.model.LocalContent
import com.mangaverse.app.main.domain.CoverRestoreInterceptor
import com.mangaverse.app.main.ui.protect.AppProtectHelper
import com.mangaverse.app.main.ui.protect.ScreenshotPolicyHelper
import com.mangaverse.app.parsers.ContentLoaderContext
import com.mangaverse.app.search.ui.ContentSuggestionsProvider
import javax.inject.Provider
import javax.inject.Singleton
import com.mangaverse.app.backups.domain.BackupObserver

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {

	@Binds
	fun bindContentLoaderContext(mangaLoaderContextImpl: ContentLoaderContextImpl): ContentLoaderContext

	@Binds
	fun bindImageGetter(coilImageGetter: CoilImageGetter): Html.ImageGetter

	companion object {

		@Provides
		@LocalizedAppContext
		fun provideLocalizedContext(
			@ApplicationContext context: Context,
		): Context = ContextCompat.getContextForLanguage(context)

		@Provides
		@Singleton
		fun provideNetworkState(
			@ApplicationContext context: Context,
			settings: AppSettings,
		) = NetworkState(context.connectivityManager, settings)

		@Provides
		@Singleton
		fun provideMangaDatabase(
			@ApplicationContext context: Context,
		): MangaDatabase = MangaDatabase(context)

		@Provides
		@Singleton
		fun provideJsonSourceDao(database: MangaDatabase): com.mangaverse.app.core.db.dao.JsonSourceDao {
			return database.getJsonSourceDao()
		}

		@Provides
		@Singleton
		fun provideExternalExtensionRepoDao(database: MangaDatabase): com.mangaverse.app.core.db.dao.ExternalExtensionRepoDao {
			return database.getExternalExtensionRepoDao()
		}

		@Provides
		@Singleton
		fun provideEpubChapterMappingDao(database: MangaDatabase): com.mangaverse.app.core.db.dao.EpubChapterMappingDao {
			return database.getEpubChapterMappingDao()
		}

		@Provides
		@Singleton
		fun provideJson(): kotlinx.serialization.json.Json = kotlinx.serialization.json.Json {
			ignoreUnknownKeys = true
			isLenient = true
			encodeDefaults = true
			prettyPrint = true
			coerceInputValues = true
		}

		@Provides
		@Singleton
		fun provideCoil(
			@LocalizedAppContext context: Context,
			@ContentHttpClient okHttpClientProvider: Provider<OkHttpClient>,
			faviconFetcherFactory: FaviconFetcher.Factory,
			imageProxyInterceptor: ImageProxyInterceptor,
			pageFetcherFactory: ContentPageFetcher.Factory,
			coverFetcherFactory: com.mangaverse.app.core.image.ContentCoverFetcher.Factory,
			coverRestoreInterceptor: CoverRestoreInterceptor,
			networkStateProvider: Provider<NetworkState>,
			captchaHandler: CaptchaHandler,
			settings: AppSettings,
		): ImageLoader {
			val diskCacheFactory = {
				val rootDir = context.externalCacheDir ?: context.cacheDir
				DiskCache.Builder()
					.directory(rootDir.resolve(CacheDir.THUMBS.dir))
					.maxSizeBytes(FileSize.MEGABYTES.convert(settings.thumbsCacheSizeMb.toLong(), FileSize.BYTES))
					.build()
			}
			val okHttpClientLazy = lazy {
				okHttpClientProvider.get().newBuilder().cache(null).build()
			}
			return ImageLoader.Builder(context)
				.interceptorCoroutineContext(Dispatchers.Default)
				.diskCache(diskCacheFactory)
				.logger(if (BuildConfig.DEBUG) SuppressingCoilLogger() else null)
				.allowRgb565(context.isLowRamDevice())
				.eventListener(captchaHandler)
				.components {
					add(ImageFailureSuppressingInterceptor())
					// Register our custom cover fetcher before OkHttpNetworkFetcherFactory so it can intercept string URLs for Mihon sources
					add(coverFetcherFactory)
					add(
						OkHttpNetworkFetcherFactory(
							callFactory = okHttpClientLazy::value,
							connectivityChecker = { networkStateProvider.get() },
						),
					)
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
						add(AnimatedImageDecoder.Factory())
					} else {
						add(GifDecoder.Factory())
					}
					add(SvgDecoder.Factory())
					add(CbzFetcher.Factory())
					add(AvifImageDecoder.Factory())
					add(faviconFetcherFactory)
					add(ContentPageKeyer())
					add(pageFetcherFactory)
					add(imageProxyInterceptor)
					add(coverRestoreInterceptor)
					add(ContentSourceHeaderInterceptor())
				}.build()
		}

		@Provides
		fun provideSearchSuggestions(
			@ApplicationContext context: Context,
		): SearchRecentSuggestions = ContentSuggestionsProvider.createSuggestions(context)

		@Provides
		@ElementsIntoSet
		fun provideDatabaseObservers(
			appShortcutManager: AppShortcutManager,
			backupObserver: BackupObserver,
		): Set<@JvmSuppressWildcards InvalidationTracker.Observer> = arraySetOf(
			appShortcutManager,
			backupObserver,
		)

		@Provides
		@ElementsIntoSet
		fun provideActivityLifecycleCallbacks(
			appProtectHelper: AppProtectHelper,
			activityRecreationHandle: ActivityRecreationHandle,
			acraScreenLogger: AcraScreenLogger,
			screenshotPolicyHelper: ScreenshotPolicyHelper,
			foregroundActivityHolder: ForegroundActivityHolder,
			interstitialAdLifecycleCallbacks: com.mangaverse.app.commercial.data.InterstitialAdLifecycleCallbacks,
		): Set<@JvmSuppressWildcards Application.ActivityLifecycleCallbacks> = arraySetOf(
			appProtectHelper,
			activityRecreationHandle,
			acraScreenLogger,
			screenshotPolicyHelper,
			foregroundActivityHolder,
			interstitialAdLifecycleCallbacks,
		)

		@Provides
		@Singleton
		@LocalStorageChanges
		fun provideMutableLocalStorageChangesFlow(): MutableSharedFlow<LocalContent?> = MutableSharedFlow()

		@Provides
		@LocalStorageChanges
		fun provideLocalStorageChangesFlow(
			@LocalStorageChanges flow: MutableSharedFlow<LocalContent?>,
		): SharedFlow<LocalContent?> = flow.asSharedFlow()

		@Provides
		fun provideWorkManager(
			@ApplicationContext context: Context,
			workerFactory: HiltWorkerFactory,
		): WorkManager {
			return runCatching {
				WorkManager.getInstance(context)
			}.getOrElse {
				WorkManager.initialize(
					context,
					Configuration.Builder()
						.setWorkerFactory(workerFactory)
						.build(),
				)
				WorkManager.getInstance(context)
			}
		}

		@Provides
		@Singleton
		@PageCache
		fun providePageCache(
			@ApplicationContext context: Context,
			settings: AppSettings,
		) = LocalStorageCache(
			context = context,
			dir = CacheDir.PAGES,
			defaultSize = FileSize.MEGABYTES.convert(settings.pagesCacheSizeMb.toLong(), FileSize.BYTES),
			minSize = FileSize.MEGABYTES.convert(20, FileSize.BYTES),
		)

		@Provides
		@Singleton
		@FaviconCache
		fun provideFaviconCache(
			@ApplicationContext context: Context,
			settings: AppSettings,
		) = LocalStorageCache(
			context = context,
			dir = CacheDir.FAVICONS,
			defaultSize = FileSize.MEGABYTES.convert(settings.faviconCacheSizeMb.toLong(), FileSize.BYTES),
			minSize = FileSize.MEGABYTES.convert(2, FileSize.BYTES),
		)

		@Provides
		@Singleton
		@NovelCache
		fun provideNovelCache(
			@ApplicationContext context: Context,
			settings: AppSettings,
		) = LocalStorageCache(
			context = context,
			dir = CacheDir.NOVELS,
			defaultSize = FileSize.MEGABYTES.convert(settings.novelCacheSizeMb.toLong(), FileSize.BYTES),
			minSize = FileSize.MEGABYTES.convert(10, FileSize.BYTES),
		)
	}
}
