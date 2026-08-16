package com.mangaverse.app.core.ui

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.mangaverse.app.core.exceptions.resolve.ExceptionResolver
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.ui.theme.OnlineFontLoader
import com.mangaverse.app.core.network.webview.WebViewExecutor

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BaseActivityEntryPoint {

	val settings: AppSettings

	val exceptionResolverFactory: ExceptionResolver.Factory

	val onlineFontLoader: OnlineFontLoader

	val webViewExecutor: WebViewExecutor
}
