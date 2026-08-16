package com.mangaverse.app.main.ui.compose

import androidx.annotation.IdRes
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.mangaverse.app.R
import com.mangaverse.app.main.ui.navigation3.DiscoverNavKey
import com.mangaverse.app.main.ui.navigation3.ExploreNavKey
import com.mangaverse.app.main.ui.navigation3.FavoritesNavKey
import com.mangaverse.app.main.ui.navigation3.FeedNavKey
import com.mangaverse.app.main.ui.navigation3.HistoryNavKey
import com.mangaverse.app.main.ui.navigation3.HomeNavKey
import com.mangaverse.app.main.ui.navigation3.LocalNavKey
import com.mangaverse.app.main.ui.navigation3.TopLevelNavKey
import com.mangaverse.app.main.ui.navigation3.UpdatedNavKey
import com.mangaverse.app.main.ui.navigation3.EnergyNavKey
import com.mangaverse.app.main.ui.navigation3.ProfileNavKey

object AppRouteNames {
    const val MAIN_SHELL = "main_shell"
    const val HOME = "home"
    const val DISCOVER = "discover"
    const val HISTORY = "history"
    const val FAVORITES = "favorites"
    const val ENTITY_ORGANIZE = "entity_organize"
    const val EXPLORE = "explore"
    const val FEED = "feed"
    const val LOCAL = "local"
    const val UPDATED = "updated"
    const val SEARCH = "search"
    const val CONTENT_LIST = "content_list"
    const val DETAILS = "details"
    const val UNIVERSE = "universe"
    const val ACCOUNT = "account"
    const val ENERGY = "energy"
    const val PROFILE = "profile"
}

internal const val ENTITY_ORGANIZE_RESULT_REFRESH_KEY = "entity_organize_result_refresh"
internal const val ENTITY_ORGANIZE_RESULT_MESSAGE_KEY = "entity_organize_result_message"

internal fun consumeEntityOrganizeRefreshResult(savedStateHandle: SavedStateHandle): Boolean {
    val shouldRefresh = savedStateHandle.get<Boolean>(ENTITY_ORGANIZE_RESULT_REFRESH_KEY) == true
    if (shouldRefresh) {
        savedStateHandle[ENTITY_ORGANIZE_RESULT_REFRESH_KEY] = false
    }
    return shouldRefresh
}

internal fun consumeEntityOrganizeMessageResult(savedStateHandle: SavedStateHandle): String? {
    val message = savedStateHandle.get<String>(ENTITY_ORGANIZE_RESULT_MESSAGE_KEY)
        ?.takeIf { it.isNotBlank() }
    if (message != null) {
        savedStateHandle[ENTITY_ORGANIZE_RESULT_MESSAGE_KEY] = null
    }
    return message
}

@Serializable
@SerialName(AppRouteNames.MAIN_SHELL)
data object MainShellRoute

@Serializable
@SerialName(AppRouteNames.HOME)
data object HomeRoute

@Serializable
@SerialName(AppRouteNames.DISCOVER)
data object DiscoverRoute

@Serializable
@SerialName(AppRouteNames.HISTORY)
data object HistoryRoute

@Serializable
@SerialName(AppRouteNames.FAVORITES)
data object FavoritesRoute

@Serializable
@SerialName(AppRouteNames.ENTITY_ORGANIZE)
data class EntityOrganizeRoute(
    val selectedContentIds: String = "",
)

fun encodeEntityOrganizeSelection(ids: Set<Long>): String {
    return ids.sorted().joinToString(separator = ",")
}

fun parseEntityOrganizeSelection(value: String): Set<Long> {
    return value
        .split(',')
        .asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapNotNull(String::toLongOrNull)
        .toSet()
}

@Serializable
@SerialName(AppRouteNames.EXPLORE)
data object ExploreRoute

@Serializable
@SerialName(AppRouteNames.FEED)
data object FeedRoute

@Serializable
@SerialName(AppRouteNames.LOCAL)
data object LocalRoute

@Serializable
@SerialName(AppRouteNames.UPDATED)
data object UpdatedRoute

@Serializable
@SerialName(AppRouteNames.CONTENT_LIST)
data class ContentListRoute(
    val sourceName: String,
)

@Serializable
@SerialName(AppRouteNames.DETAILS)
data object DetailsRoute

@Serializable
@SerialName(AppRouteNames.UNIVERSE)
data object UniverseRoute

@Serializable
@SerialName(AppRouteNames.ACCOUNT)
data object AccountRoute

@Serializable
@SerialName("server")
data class ServerRoute(
    val serverId: String,
    val serverName: String,
)

@Serializable
@SerialName(AppRouteNames.ENERGY)
data object EnergyRoute

@Serializable
@SerialName(AppRouteNames.PROFILE)
data object ProfileRoute

fun routeForBottomNavItem(@IdRes itemId: Int): Any = when (itemId) {
    R.id.nav_home -> HomeRoute
    R.id.nav_history -> HistoryRoute
    R.id.nav_favorites -> FavoritesRoute
    R.id.nav_explore -> ExploreRoute
    R.id.nav_discover -> DiscoverRoute
    R.id.nav_feed -> FeedRoute
    R.id.nav_local -> LocalRoute
    R.id.nav_updated -> UpdatedRoute
    R.id.nav_energy -> EnergyRoute
    R.id.nav_profile -> ProfileRoute
    else -> HomeRoute
}

fun topLevelKeyForBottomNavItem(@IdRes itemId: Int): TopLevelNavKey = when (itemId) {
    R.id.nav_home -> HomeNavKey
    R.id.nav_history -> HistoryNavKey
    R.id.nav_favorites -> FavoritesNavKey
    R.id.nav_explore -> ExploreNavKey
    R.id.nav_discover -> DiscoverNavKey
    R.id.nav_feed -> FeedNavKey
    R.id.nav_local -> LocalNavKey
    R.id.nav_updated -> UpdatedNavKey
    R.id.nav_energy -> EnergyNavKey
    R.id.nav_profile -> ProfileNavKey
    else -> HomeNavKey
}

fun routeForTopLevelKey(key: TopLevelNavKey): Any = when (key) {
    HomeNavKey,
    HistoryNavKey,
    FavoritesNavKey,
    ExploreNavKey,
    DiscoverNavKey,
    FeedNavKey,
    LocalNavKey,
    UpdatedNavKey,
    EnergyNavKey,
    ProfileNavKey,
    -> MainShellRoute
}

fun bottomNavItemIdForTopLevelKey(key: TopLevelNavKey): Int = when (key) {
    HomeNavKey -> R.id.nav_home
    HistoryNavKey -> R.id.nav_history
    FavoritesNavKey -> R.id.nav_favorites
    ExploreNavKey -> R.id.nav_explore
    DiscoverNavKey -> R.id.nav_discover
    FeedNavKey -> R.id.nav_feed
    LocalNavKey -> R.id.nav_local
    UpdatedNavKey -> R.id.nav_updated
    EnergyNavKey -> R.id.nav_energy
    ProfileNavKey -> R.id.nav_profile
}

fun topLevelKeyForDestination(destination: NavDestination?): TopLevelNavKey? = when {
    destination?.hasRoute<HomeRoute>() == true -> HomeNavKey
    destination?.hasRoute<HistoryRoute>() == true -> HistoryNavKey
    destination?.hasRoute<FavoritesRoute>() == true -> FavoritesNavKey
    destination?.hasRoute<ExploreRoute>() == true -> ExploreNavKey
    destination?.hasRoute<DiscoverRoute>() == true -> DiscoverNavKey
    destination?.hasRoute<FeedRoute>() == true -> FeedNavKey
    destination?.hasRoute<LocalRoute>() == true -> LocalNavKey
    destination?.hasRoute<UpdatedRoute>() == true -> UpdatedNavKey
    destination?.hasRoute<EnergyRoute>() == true -> EnergyNavKey
    destination?.hasRoute<ProfileRoute>() == true -> ProfileNavKey
    else -> null
}
