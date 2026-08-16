package com.mangaverse.app.core.nav

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.work.domain.WorkResolver
import com.mangaverse.app.space.domain.SpaceRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppRouterEntryPoint {

    val settings: AppSettings
    val contentDataRepository: ContentDataRepository
    val mangaRepositoryFactory: ContentRepository.Factory
    val workResolver: WorkResolver
    val jsonSourceManager: com.mangaverse.app.core.jsonsource.JsonSourceManager
    val spaceFeatureFlagsRepository: SpaceFeatureFlagsRepository
    val spaceRepository: SpaceRepository
}
