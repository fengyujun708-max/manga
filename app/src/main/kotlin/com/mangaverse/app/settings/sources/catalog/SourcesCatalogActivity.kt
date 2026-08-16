package com.mangaverse.app.settings.sources.catalog

import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.mangaverse.app.core.nav.router
import com.mangaverse.app.core.ui.BaseComposeActivity

@AndroidEntryPoint
class SourcesCatalogActivity : BaseComposeActivity() {

    private val viewModel by viewModels<SourcesCatalogViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent {
            SourcesCatalogRoute(
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onBack = ::finish,
                onOpenSource = { source -> router.openList(source, null, null) },
            )
        }
    }
}
