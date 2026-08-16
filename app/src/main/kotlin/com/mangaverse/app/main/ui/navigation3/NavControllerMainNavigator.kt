package com.mangaverse.app.main.ui.navigation3

import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.mangaverse.app.core.nav.PendingContentListNavigation
import com.mangaverse.app.core.nav.PendingDetailsNavigation
import com.mangaverse.app.details.ui.model.DetailsOrigin
import com.mangaverse.app.main.ui.MainActivity
import com.mangaverse.app.main.ui.compose.ContentListRoute
import com.mangaverse.app.main.ui.compose.DetailsRoute
import com.mangaverse.app.main.ui.compose.MainShellRoute
import com.mangaverse.app.main.ui.compose.UniverseRoute
import com.mangaverse.app.main.ui.compose.AccountRoute
import com.mangaverse.app.main.ui.compose.ServerRoute
import com.mangaverse.app.main.ui.compose.EnergyRoute
import com.mangaverse.app.main.ui.compose.routeForTopLevelKey
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.SortOrder

class NavControllerMainNavigator(
    private val navController: NavHostController,
    private val mainActivity: MainActivity?,
    private val mainNavState: MainNavState? = null,
    private val onDetailsTransitionRequested: () -> Unit = {},
) : MainNavigator {

    override fun openTopLevel(key: TopLevelNavKey) {
        mainNavState?.navigateTopLevel(key)
        if (navController.currentDestination?.hasRoute<MainShellRoute>() == true) {
            return
        }
        navController.navigate(routeForTopLevelKey(key)) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = false
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    override fun openContentList(
        source: ContentSource,
        filter: ContentListFilter?,
        sortOrder: SortOrder?,
    ) {
        onDetailsTransitionRequested()
        mainNavState?.push(ContentListNavKey(sourceName = source.name))
        PendingContentListNavigation.set(filter = filter, sortOrder = sortOrder)
        navController.navigate(ContentListRoute(sourceName = source.name)) {
            launchSingleTop = true
        }
    }

    override fun openDetails(
        content: Content,
        sharedElementKey: String?,
    ) {
        onDetailsTransitionRequested()
        mainActivity?.resolveDetailsOriginForContent(content) { origin ->
            mainNavState?.push(origin.toDetailsNavKey())
            PendingDetailsNavigation.set(origin, sharedElementKey)
            navController.navigate(DetailsRoute)
        } ?: run {
            mainNavState?.push(DetailsNavKey(requestedProjectionId = content.id))
            PendingDetailsNavigation.set(content, sharedElementKey)
            navController.navigate(DetailsRoute)
        }
    }

    override fun openDetails(
        origin: DetailsOrigin,
        sharedElementKey: String?,
    ) {
        onDetailsTransitionRequested()
        mainNavState?.push(origin.toDetailsNavKey())
        PendingDetailsNavigation.set(origin, sharedElementKey)
        navController.navigate(DetailsRoute)
    }

    override fun openUniverse() {
        onDetailsTransitionRequested()
        navController.navigate(UniverseRoute) {
            launchSingleTop = true
        }
    }

    override fun openAccount() {
        onDetailsTransitionRequested()
        navController.navigate(AccountRoute) {
            launchSingleTop = true
        }
    }

    override fun openServer(serverId: String, serverName: String) {
        onDetailsTransitionRequested()
        navController.navigate(ServerRoute(serverId = serverId, serverName = serverName)) {
            launchSingleTop = true
        }
    }

    override fun openEnergy() {
        onDetailsTransitionRequested()
        navController.navigate(EnergyRoute) {
            launchSingleTop = true
        }
    }

    override fun pop(): Boolean {
        val popped = navController.popBackStack()
        if (popped) {
            mainNavState?.pop()
        }
        return popped
    }
}

private fun DetailsOrigin.toDetailsNavKey(): DetailsNavKey = when (this) {
    is DetailsOrigin.EntityGraph -> DetailsNavKey(
        entityId = entityId,
        requestedProjectionId = initialProjectionLocalMangaId ?: preferredLocalMangaId,
    )
    is DetailsOrigin.LocalMangaId -> DetailsNavKey(requestedProjectionId = mangaId)
    is DetailsOrigin.LocalMangaContent -> DetailsNavKey(requestedProjectionId = manga.id)
    is DetailsOrigin.TrackingEntity,
    is DetailsOrigin.TrackingItem,
    -> DetailsNavKey()
}
