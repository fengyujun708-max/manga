package com.mangaverse.app.details.ui.related

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.mangaverse.app.R
import com.mangaverse.app.core.model.parcelable.ParcelableContent
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.nav.ContentIntent
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.core.util.ext.require
import com.mangaverse.app.list.domain.ContentListMapper
import com.mangaverse.app.list.ui.ContentListViewModel
import com.mangaverse.app.list.ui.model.EmptyState
import com.mangaverse.app.list.ui.model.LoadingState
import com.mangaverse.app.list.ui.model.toErrorState
import com.mangaverse.app.local.data.LocalStorageChanges
import com.mangaverse.app.local.domain.model.LocalContent
import com.mangaverse.app.parsers.model.Content
import javax.inject.Inject

@HiltViewModel
class RelatedListViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val mangaRepositoryFactory: ContentRepository.Factory,
	settings: AppSettings,
	private val mangaListMapper: ContentListMapper,
	private val mangaDataRepository: ContentDataRepository,
	@LocalStorageChanges localStorageChanges: SharedFlow<LocalContent?>,
) : ContentListViewModel(settings, mangaDataRepository, localStorageChanges) {

	private val seed = savedStateHandle.require<ParcelableContent>(AppRouter.KEY_MANGA).manga
	private val intent = ContentIntent(savedStateHandle)
	private val currentContent = MutableStateFlow<Content?>(null)
	private val mangaList = MutableStateFlow<List<Content>?>(null)
	private val listError = MutableStateFlow<Throwable?>(null)
	private var loadingJob: Job? = null

	override val content = combine(
		mangaList,
		observeListModeWithTriggers(),
		listError,
	) { list, mode, error ->
		when {
			list.isNullOrEmpty() && error != null -> listOf(error.toErrorState(canRetry = true))
			list == null -> listOf(LoadingState)
			list.isEmpty() -> listOf(createEmptyState())
			else -> mangaListMapper.toListModelList(list, mode)
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	init {
		launchJob(Dispatchers.Default) {
			currentContent.value = resolveCurrentContent()
			loadList()
		}
	}

	override fun onRefresh() {
		loadList()
	}

	override fun onRetry() {
		loadList()
	}

	private fun loadList(): Job {
		loadingJob?.let {
			if (it.isActive) return it
		}
		return launchLoadingJob(Dispatchers.Default) {
			try {
				listError.value = null
				val content = currentContent.value ?: resolveCurrentContent()
								?: throw IllegalStateException("Unable to resolve related content context")
				currentContent.value = content
				val repository = mangaRepositoryFactory.create(content.source)
				mangaList.value = repository.getRelated(content)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				e.printStackTraceDebug()
				listError.value = e
				if (!mangaList.value.isNullOrEmpty()) {
					errorEvent.call(e)
				}
			}
		}.also { loadingJob = it }
	}

	private suspend fun resolveCurrentContent(): Content? {
		val resolved = mangaDataRepository.resolveIntent(intent, withChapters = false)
		if (resolved != null) {
			return resolved
		}
		return seed.takeIf { intent.mangaId == 0L }
	}

	private fun createEmptyState() = EmptyState(
		icon = R.drawable.ic_empty_common,
		textPrimary = R.string.nothing_found,
		textSecondary = 0,
		actionStringRes = 0,
	)
}
