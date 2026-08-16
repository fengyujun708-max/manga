package com.mangaverse.app.download.ui.list

import android.os.Bundle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import com.mangaverse.app.core.nav.router
import com.mangaverse.app.core.ui.BaseComposeActivity
import com.mangaverse.app.download.ui.compose.AppDownloadsRoute

@AndroidEntryPoint
class DownloadsActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setComposeContent {
            AppDownloadsRoute(
                appRouter = router,
                contentPadding = PaddingValues(0.dp),
            )
        }
    }
}
