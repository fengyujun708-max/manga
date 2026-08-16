package com.mangaverse.app.core.ui.dialog

import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import com.mangaverse.app.R
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.nav.router
import com.mangaverse.app.core.ui.BaseComposeActivity
import com.mangaverse.app.core.util.ext.copyToClipboard
import com.mangaverse.app.core.util.ext.getCauseUrl
import com.mangaverse.app.core.util.ext.isHttpUrl
import com.mangaverse.app.core.util.ext.isReportable
import com.mangaverse.app.core.util.ext.report
import java.io.Serializable

@AndroidEntryPoint
class ErrorDetailsActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val exception = intent.getSerializableExtra(AppRouter.KEY_ERROR) as? Throwable ?: return finish()
        val causeUrl = exception.getCauseUrl()?.takeIf(String::isHttpUrl)
        setComposeContent {
            AlertDialog(
                onDismissRequest = ::finishAfterTransition,
                title = { Text(getString(R.string.error_details)) },
                text = {
                    Column {
                        exception.message?.let { Text(it) }
                        if (causeUrl != null) {
                            TextButton(onClick = { router.openBrowser(causeUrl, null, null) }) {
                                Text(getString(R.string.open_in_browser))
                            }
                        }
                        if (exception.isReportable()) Text(getString(R.string.error_disclaimer_report), Modifier.padding(top = 8.dp))
                    }
                },
                confirmButton = {
                    if (exception.isReportable()) {
                        TextButton(onClick = {
                            exception.report(silent = true)
                            finishAfterTransition()
                        }) { Text(getString(R.string.report)) }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        copyToClipboard(getString(R.string.error), exception.stackTraceToString())
                    }) { Text(getString(androidx.preference.R.string.copy)) }
                },
            )
        }
    }
}
