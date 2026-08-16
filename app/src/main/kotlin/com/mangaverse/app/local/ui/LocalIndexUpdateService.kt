package com.mangaverse.app.local.ui

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import com.mangaverse.app.core.ui.CoroutineIntentService
import com.mangaverse.app.local.data.index.LocalContentIndex
import javax.inject.Inject

@AndroidEntryPoint
class LocalIndexUpdateService : CoroutineIntentService() {

	@Inject
	lateinit var localContentIndex: LocalContentIndex

	override suspend fun IntentJobContext.processIntent(intent: Intent) {
		localContentIndex.update()
	}

	override fun IntentJobContext.onError(error: Throwable) = Unit
}
