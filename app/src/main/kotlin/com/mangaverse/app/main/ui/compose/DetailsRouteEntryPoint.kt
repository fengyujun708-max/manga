package com.mangaverse.app.main.ui.compose

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import com.mangaverse.app.core.os.AppShortcutManager
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.reader.ui.PageSaveHelper

@EntryPoint
@InstallIn(ActivityComponent::class)
interface DetailsRouteEntryPoint {
    fun settings(): AppSettings
    fun pageSaveHelperFactory(): PageSaveHelper.Factory
    fun appShortcutManager(): AppShortcutManager
}
