package com.mangaverse.app.space

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.mangaverse.app.space.domain.DefaultSpaceContentPolicy
import com.mangaverse.app.space.data.AppSettingsSpaceLocalDataSource
import com.mangaverse.app.space.data.DefaultSpaceFeatureFlagsRepository
import com.mangaverse.app.space.data.DefaultSpaceRepository
import com.mangaverse.app.space.data.DefaultSpaceRoutePreferencesRepository
import com.mangaverse.app.space.data.DefaultSpaceSessionRepository
import com.mangaverse.app.space.data.DefaultSpaceSessionValidator
import com.mangaverse.app.space.data.DefaultSpaceSourceAvailability
import com.mangaverse.app.space.data.DefaultSpaceSwitchCoordinator
import com.mangaverse.app.space.data.DefaultSpaceCatalogRepository
import com.mangaverse.app.space.data.LogcatSpaceDiagnostics
import com.mangaverse.app.space.data.SpaceDiagnostics
import com.mangaverse.app.space.data.SpaceLocalDataSource
import com.mangaverse.app.space.domain.SpaceContentPolicy
import com.mangaverse.app.space.domain.SpaceFeatureFlagsRepository
import com.mangaverse.app.space.domain.SpaceRepository
import com.mangaverse.app.space.domain.SpaceRoutePreferencesRepository
import com.mangaverse.app.space.domain.SpaceSessionRepository
import com.mangaverse.app.space.domain.SpaceSessionValidator
import com.mangaverse.app.space.domain.SpaceSourceAvailability
import com.mangaverse.app.space.domain.SpaceSwitchCoordinator
import com.mangaverse.app.space.domain.SpaceCatalogRepository

@Module
@InstallIn(SingletonComponent::class)
interface SpaceModule {

	@Binds
	fun bindSpaceContentPolicy(impl: DefaultSpaceContentPolicy): SpaceContentPolicy

	@Binds
	fun bindSpaceCatalogRepository(impl: DefaultSpaceCatalogRepository): SpaceCatalogRepository

	@Binds
	fun bindSpaceRepository(impl: DefaultSpaceRepository): SpaceRepository

	@Binds
	fun bindSpaceRoutePreferencesRepository(
		impl: DefaultSpaceRoutePreferencesRepository,
	): SpaceRoutePreferencesRepository

	@Binds
	fun bindSpaceLocalDataSource(impl: AppSettingsSpaceLocalDataSource): SpaceLocalDataSource

	@Binds
	fun bindSpaceDiagnostics(impl: LogcatSpaceDiagnostics): SpaceDiagnostics

	@Binds
	fun bindSpaceFeatureFlagsRepository(impl: DefaultSpaceFeatureFlagsRepository): SpaceFeatureFlagsRepository

	@Binds
	fun bindSpaceSessionRepository(impl: DefaultSpaceSessionRepository): SpaceSessionRepository

	@Binds
	fun bindSpaceSessionValidator(impl: DefaultSpaceSessionValidator): SpaceSessionValidator

	@Binds
	fun bindSpaceSourceAvailability(impl: DefaultSpaceSourceAvailability): SpaceSourceAvailability

	@Binds
	fun bindSpaceSwitchCoordinator(impl: DefaultSpaceSwitchCoordinator): SpaceSwitchCoordinator
}
