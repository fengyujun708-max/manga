package com.mangaverse.app.space.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.observeAsFlow
import com.mangaverse.app.core.util.ext.processLifecycleScope
import com.mangaverse.app.space.domain.SpaceFeatureFlags
import com.mangaverse.app.space.domain.SpaceFeatureFlagsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSpaceFeatureFlagsRepository @Inject constructor(
	private val settings: AppSettings,
) : SpaceFeatureFlagsRepository {

	override val flags: StateFlow<SpaceFeatureFlags> = combine(
		settings.observeAsFlow(AppSettings.KEY_ENTITY_SPACE_ENABLED) { isEntitySpaceEnabled },
		settings.observeAsFlow(AppSettings.KEY_SPACE_SWITCHER_ENABLED) { isSpaceSwitcherEnabled },
		settings.observeAsFlow(AppSettings.KEY_SPACE_PERSISTENT_NAVIGATION_ENABLED) {
			isSpacePersistentNavigationEnabled
		},
		settings.observeAsFlow(AppSettings.KEY_SPACE_IMMERSIVE_SWITCH_ENABLED) { isSpaceImmersiveSwitchEnabled },
		settings.observeAsFlow(AppSettings.KEY_SPACE_ROUTE_PREFERENCES_ENABLED) { isSpaceRoutePreferencesEnabled },
	) { entitySpace, switcher, persistentNavigation, immersiveSwitch, routePreferences ->
		SpaceFeatureFlags(
			entitySpaceEnabled = entitySpace,
			spaceSwitcherEnabled = switcher,
			spacePersistentNavigationEnabled = persistentNavigation,
			spaceImmersiveSwitchEnabled = immersiveSwitch,
			spaceRoutePreferencesEnabled = routePreferences,
		)
	}.flowOn(Dispatchers.Default).stateIn(
		scope = processLifecycleScope,
		started = SharingStarted.Eagerly,
		initialValue = settings.readSpaceFeatureFlags(),
	)

	private fun AppSettings.readSpaceFeatureFlags() = SpaceFeatureFlags(
		entitySpaceEnabled = isEntitySpaceEnabled,
		spaceSwitcherEnabled = isSpaceSwitcherEnabled,
		spacePersistentNavigationEnabled = isSpacePersistentNavigationEnabled,
		spaceImmersiveSwitchEnabled = isSpaceImmersiveSwitchEnabled,
		spaceRoutePreferencesEnabled = isSpaceRoutePreferencesEnabled,
	)
}
