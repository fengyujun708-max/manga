package com.mangaverse.app.settings.storage.directories

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.mangaverse.app.R
import com.mangaverse.app.core.exceptions.resolve.SnackbarErrorObserver
import com.mangaverse.app.core.os.OpenDocumentTreeHelper
import com.mangaverse.app.core.ui.BaseComposeActivity
import com.mangaverse.app.core.util.ext.observeEvent
import com.mangaverse.app.core.util.ext.tryLaunch
import com.mangaverse.app.settings.storage.RequestStorageManagerPermissionContract

@AndroidEntryPoint
class ContentDirectoriesActivity : BaseComposeActivity() {

	private val viewModel: ContentDirectoriesViewModel by viewModels()
	private val pickFileTreeLauncher = OpenDocumentTreeHelper(
		activityResultCaller = this,
		flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
			or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
	) {
		if (it != null) viewModel.onCustomDirectoryPicked(it)
	}
	private val permissionRequestLauncher = registerForActivityResult(
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			RequestStorageManagerPermissionContract()
		} else {
			ActivityResultContracts.RequestPermission()
		},
	) {
		if (it) {
			viewModel.updateList()
			if (!pickFileTreeLauncher.tryLaunch(null)) {
				lifecycleScope.launch {
					snackbarHostState.showSnackbar(getString(R.string.operation_not_supported))
				}
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		viewModel.onError.observeEvent(
			this,
			SnackbarErrorObserver(window.decorView, null, exceptionResolver) {
				if (it) viewModel.updateList()
			},
		)
		setComposeContent {
			ContentDirectoriesScreen(
				items = viewModel.items.collectAsStateWithLifecycle().value,
				isLoading = viewModel.isLoading.collectAsStateWithLifecycle().value,
				onBack = ::finish,
				onAddDirectory = {
					if (!permissionRequestLauncher.tryLaunch(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
						lifecycleScope.launch {
							snackbarHostState.showSnackbar(getString(R.string.operation_not_supported))
						}
					}
				},
				onRemoveDirectory = viewModel::onRemoveClick,
			)
		}
	}
}
