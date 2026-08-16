package com.mangaverse.app.local.ui.info

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import com.mangaverse.app.core.model.parcelable.ParcelableContent
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.util.ext.MutableEventFlow
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.core.util.ext.computeSize
import com.mangaverse.app.core.util.ext.toFileOrNull
import com.mangaverse.app.local.data.LocalMangaRepository
import com.mangaverse.app.local.data.LocalStorageManager
import com.mangaverse.app.local.domain.DeleteReadChaptersUseCase
import com.mangaverse.app.parsers.model.Content
import javax.inject.Inject

@HiltViewModel
class LocalInfoViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val contentDataRepository: ContentDataRepository,
	private val localContentRepository: LocalMangaRepository,
	private val storageManager: LocalStorageManager,
	private val deleteReadChaptersUseCase: DeleteReadChaptersUseCase,
) : BaseViewModel() {

	private var mangaState: Content? = savedStateHandle.get<ParcelableContent>(AppRouter.KEY_MANGA)?.manga
	private val manga: Content
		get() = checkNotNull(mangaState) {
			"LocalInfoViewModel is not initialized with content"
		}

	val isCleaningUp = MutableStateFlow(false)
	val onCleanedUp = MutableEventFlow<Pair<Int, Long>>()

	val path = MutableStateFlow<String?>(null)
	val size = MutableStateFlow(-1L)
	val availableSize = MutableStateFlow(-1L)

	init {
		launchJob(Dispatchers.Default) {
			val resolved = mangaState?.id
				?.takeIf { it != 0L }
				?.let {
					contentDataRepository.findPreferredLocalContentById(it, withChapters = false)
						?: contentDataRepository.findContentById(it, withChapters = false)
				}
				?: mangaState
			if (resolved != null) {
				mangaState = resolved
				computeSize().join()
			}
		}
	}

	fun initialize(manga: Content) {
		if (mangaState?.id == manga.id && path.value != null) {
			return
		}
		mangaState = manga
		path.value = null
		size.value = -1L
		availableSize.value = -1L
		computeSize()
	}

	fun cleanup() {
		launchJob(Dispatchers.Default) {
			try {
				isCleaningUp.value = true
				val oldSize = size.value
				val chaptersCount = deleteReadChaptersUseCase.invoke(manga)
				computeSize().join()
				val newSize = size.value
				onCleanedUp.call(chaptersCount to oldSize - newSize)
			} finally {
				isCleaningUp.value = false
			}
		}
	}

	private fun computeSize() = launchLoadingJob(Dispatchers.Default) {
		val file = manga.url.toUri().toFileOrNull() ?: localContentRepository.findSavedContent(manga)?.file
		requireNotNull(file)
		path.value = file.path
		size.value = file.computeSize()
		availableSize.value = storageManager.computeAvailableSize()
	}
}
