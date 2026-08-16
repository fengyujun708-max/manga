package com.mangaverse.app.download.ui.worker

import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.FlowCollector
import com.mangaverse.app.R
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.util.ext.findActivity
import com.mangaverse.app.core.util.ext.getThemeColor

class DownloadStartedObserver(
	private val snackbarHost: View,
) : FlowCollector<Unit> {

	override suspend fun emit(value: Unit) {
		val snackbar = Snackbar.make(snackbarHost, R.string.download_started, Snackbar.LENGTH_LONG)
		val router = AppRouter.from(snackbarHost)
		if (router != null) {
			snackbar.setAction(R.string.details) { router.openDownloads() }
			snackbar.setActionTextColor(
				snackbarHost.context.getThemeColor(androidx.appcompat.R.attr.colorPrimary),
			)
		}
		snackbar.show()
	}
}
