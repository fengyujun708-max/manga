package com.mangaverse.app.favourites.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import com.mangaverse.app.core.model.FavouriteCategory.Companion.NO_ID
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.nav.router
import com.mangaverse.app.core.ui.theme.KototoroTheme
import com.mangaverse.app.favourites.ui.compose.KototoroFavoritesHostRoute

@AndroidEntryPoint
class FavouritesActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialCategoryId = intent?.getLongExtra(AppRouter.KEY_ID, NO_ID) ?: NO_ID
        val initialCategoryTitle = intent?.getStringExtra(AppRouter.KEY_TITLE)

        setContent {
            KototoroTheme {
                KototoroFavoritesHostRoute(
                    appRouter = router,
                    contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
                    initialCategoryId = initialCategoryId,
                    initialCategoryTitle = initialCategoryTitle,
                    onOpenEntityOrganize = { selectedIds ->
                        router.openEntityOrganizeSettings(selectedIds)
                    },
                )
            }
        }
    }
}
