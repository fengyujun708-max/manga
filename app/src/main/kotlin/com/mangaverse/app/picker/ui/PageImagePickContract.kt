package com.mangaverse.app.picker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.mangaverse.app.core.model.parcelable.ParcelableContent
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.parsers.model.Content

class PageImagePickContract : ActivityResultContract<Content?, Uri?>() {

	override fun createIntent(context: Context, input: Content?): Intent =
		Intent(context, PageImagePickActivity::class.java)
			.putExtra(AppRouter.KEY_MANGA, input?.let { ParcelableContent(it) })

	override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data
}
