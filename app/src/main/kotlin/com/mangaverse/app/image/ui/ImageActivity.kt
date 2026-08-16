package com.mangaverse.app.image.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.lifecycle
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import com.mangaverse.app.R
import com.mangaverse.app.core.exceptions.resolve.SnackbarErrorObserver
import com.mangaverse.app.core.image.CoilMemoryCacheKey
import com.mangaverse.app.core.model.ContentSource
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.ui.BaseComposeActivity
import com.mangaverse.app.core.ui.util.PopupMenuMediator
import com.mangaverse.app.core.util.ShareHelper
import com.mangaverse.app.core.util.ext.getDisplayIcon
import com.mangaverse.app.core.util.ext.getDisplayMessage
import com.mangaverse.app.core.util.ext.getParcelableExtraCompat
import com.mangaverse.app.core.util.ext.mangaSourceExtra
import com.mangaverse.app.core.util.ext.observe
import com.mangaverse.app.core.util.ext.observeEvent
import javax.inject.Inject

@AndroidEntryPoint
class ImageActivity : BaseComposeActivity(), ImageRequest.Listener {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel: ImageViewModel by viewModels()
	private lateinit var menuMediator: PopupMenuMediator
	private var menuAnchor: View? = null
	private var imageModel by androidx.compose.runtime.mutableStateOf<Any?>(null)
	private var isImageLoading by androidx.compose.runtime.mutableStateOf(false)
	private var imageError by androidx.compose.runtime.mutableStateOf<ImageErrorState?>(null)
	private var isSaving by androidx.compose.runtime.mutableStateOf(false)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		menuMediator = PopupMenuMediator(
			ImageMenuProvider(
				activity = this,
				snackbarHost = window.decorView,
				viewModel = viewModel,
			),
		)
		viewModel.isLoading.observe(this) { isSaving = it }
		viewModel.onError.observeEvent(this, SnackbarErrorObserver(window.decorView, null))
		viewModel.onImageSaved.observeEvent(this, ::onImageSaved)

		setComposeContent {
			ImageViewerScreen(
				imageModel = imageModel,
				imageLoader = coil,
				showMenu = true,
				isSaving = isSaving,
				isLoading = isImageLoading,
				error = imageError,
				onBack = ::navigateUp,
				onMenu = { menuAnchor?.let(menuMediator::onLongClick) },
				onRetry = ::loadImage,
				onMenuAnchorCreated = { menuAnchor = it },
			)
		}
		loadImage()
	}

	override fun onError(request: ImageRequest, result: ErrorResult) {
		isImageLoading = false
		imageError = ImageErrorState(
			message = result.throwable.getDisplayMessage(resources),
			iconRes = result.throwable.getDisplayIcon(),
		)
	}

	override fun onStart(request: ImageRequest) {
		isImageLoading = true
		imageError = null
	}

	override fun onSuccess(request: ImageRequest, result: SuccessResult) {
		isImageLoading = false
		imageError = null
	}

	private fun loadImage() {
		isImageLoading = true
		imageError = null
		imageModel = ImageRequest.Builder(this)
			.data(intent.data)
			.memoryCacheKey(intent.getParcelableExtraCompat<CoilMemoryCacheKey>(AppRouter.KEY_PREVIEW)?.data)
			.memoryCachePolicy(CachePolicy.READ_ONLY)
			.lifecycle(this)
			.listener(this)
			.mangaSourceExtra(ContentSource(intent.getStringExtra(AppRouter.KEY_SOURCE)))
			.build()
	}

	private fun onImageSaved(uri: Uri) {
		Snackbar.make(window.decorView, R.string.page_saved, Snackbar.LENGTH_LONG)
			.setAction(R.string.share) {
				ShareHelper(this).shareImage(uri)
			}.show()
	}

	private fun navigateUp() {
		val upIntent = parentActivityIntent
		if (upIntent != null) {
			if (!navigateUpTo(upIntent)) {
				startActivity(upIntent)
			}
		} else {
			finishAfterTransition()
		}
	}
}
