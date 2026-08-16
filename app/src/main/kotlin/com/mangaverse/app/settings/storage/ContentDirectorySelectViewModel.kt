package com.mangaverse.app.settings.storage

import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import com.mangaverse.app.R
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.util.ext.MutableEventFlow
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.core.util.ext.isWriteable
import com.mangaverse.app.local.data.LocalStorageManager
import javax.inject.Inject

@HiltViewModel
class ContentDirectorySelectViewModel @Inject constructor(
	private val storageManager: LocalStorageManager,
	private val settings: AppSettings,
) : BaseViewModel() {

	private var contentType: String? = null

	val items = MutableStateFlow(emptyList<DirectoryModel>())
	val onDismissDialog = MutableEventFlow<Unit>()
	val onPickDirectory = MutableEventFlow<Unit>()

	fun initialize(contentType: String) {
		this.contentType = contentType
		refresh()
	}

	fun onItemClick(item: DirectoryModel) {
		if (item.file != null) {
			when (contentType) {
				CONTENT_TYPE_NOVEL -> settings.novelStorageDir = item.file
				CONTENT_TYPE_VIDEO -> settings.videoStorageDir = item.file
				else -> settings.mangaStorageDir = item.file
			}
			onDismissDialog.call(Unit)
		} else {
			onPickDirectory.call(Unit)
		}
	}

	fun onCustomDirectoryPicked(uri: Uri) {
		launchJob(Dispatchers.Default) {
			storageManager.takePermissions(uri)
			val dir = storageManager.resolveUri(uri)
			if (!dir.isWriteable()) {
				throw AccessDeniedException(dir)
			}
			when (contentType) {
				CONTENT_TYPE_NOVEL -> settings.novelStorageDir = dir
				CONTENT_TYPE_VIDEO -> settings.videoStorageDir = dir
				else -> {
					if (dir !in storageManager.getApplicationStorageDirs()) {
						settings.mangaStorageDir = dir
					}
				}
			}
			storageManager.setDirIsNoMedia(dir)
			onDismissDialog.call(Unit)
		}
	}

	fun refresh() {
		launchJob(Dispatchers.Default) {
			val defaultValue = when (contentType) {
				CONTENT_TYPE_NOVEL -> storageManager.getDefaultNovelWriteableDir()
				CONTENT_TYPE_VIDEO -> storageManager.getDefaultVideoWriteableDir()
				else -> storageManager.getDefaultWriteableDir()
			}
			val available = when (contentType) {
				CONTENT_TYPE_NOVEL -> storageManager.getNovelWriteableDirs()
				CONTENT_TYPE_VIDEO -> storageManager.getVideoWriteableDirs()
				else -> storageManager.getWriteableDirs()
			}
			items.value = buildList(available.size + 1) {
				available.mapTo(this) { dir ->
					DirectoryModel(
						title = storageManager.getDirectoryDisplayName(dir, isFullPath = false),
						titleRes = 0,
						file = dir,
						isChecked = dir == defaultValue,
						isAvailable = true,
						isRemovable = false,
					)
				}
				this += DirectoryModel(
					title = null,
					titleRes = R.string.pick_custom_directory,
					file = null,
					isChecked = false,
					isAvailable = true,
					isRemovable = false,
				)
			}
		}
	}

	companion object {
		const val CONTENT_TYPE_MANGA = "manga"
		const val CONTENT_TYPE_NOVEL = "novel"
		const val CONTENT_TYPE_VIDEO = "video"
	}
}
