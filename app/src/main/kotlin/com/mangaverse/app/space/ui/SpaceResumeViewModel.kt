package com.mangaverse.app.space.ui

import androidx.lifecycle.viewModelScope
import dagger.Reusable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.withTimeoutOrNull
import com.mangaverse.app.core.model.getContentType
import com.mangaverse.app.core.model.isLocal
import com.mangaverse.app.core.os.NetworkState
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.observeAsFlow
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.util.ext.MutableEventFlow
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.history.data.HistoryRepository
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.reader.ui.ReaderState
import com.mangaverse.app.space.domain.SpaceId
import com.mangaverse.app.space.domain.SpaceCatalogRepository
import com.mangaverse.app.space.domain.SpaceFeatureFlagsRepository
import com.mangaverse.app.space.domain.SpaceKind
import com.mangaverse.app.space.domain.SpaceRepository
import javax.inject.Inject

data class SpaceResumeItem(
	val spaceId: SpaceId,
	val title: String,
	val content: Content,
	val canResume: Boolean,
)

data class SpaceResumeUiState(
	val items: Map<SpaceId, SpaceResumeItem> = emptyMap(),
)

data class SpaceResumeRequest(
	val content: Content,
	val contentType: ContentType?,
	val state: ReaderState?,
)

@Reusable
class SpaceResumeStateSource @Inject constructor(
	historyRepository: HistoryRepository,
	catalogRepository: SpaceCatalogRepository,
	featureFlagsRepository: SpaceFeatureFlagsRepository,
	private val networkState: NetworkState,
	private val settings: AppSettings,
) {
	private val activeContexts = combine(
		catalogRepository.spaces,
		featureFlagsRepository.flags,
	) { contexts, flags ->
		contexts.takeIf { flags.effectiveSwitcherEnabled }.orEmpty()
	}

	private val recentBySpace = combine(
		activeContexts,
		settings.observeAsFlow(AppSettings.KEY_HISTORY_EXCLUDE_NSFW) { isHistoryExcludeNsfw },
	) { contexts, excludeNsfw ->
		contexts to excludeNsfw
	}.flatMapLatest { (contexts, excludeNsfw) ->
		if (contexts.isEmpty()) {
			flowOf(emptyMap())
		} else {
			combine(contexts.map { context ->
				historyRepository.observeLast(
					spaceId = context.id,
					excludeNsfw = excludeNsfw,
				).map { context.id to it }
			}) { entries -> entries.toMap() }
		}
	}

	fun observe() = combine(
		recentBySpace,
		networkState,
	) { recent, isOnline ->
		buildSpaceResumeUiState(recent, isOnline, true)
	}.distinctUntilChanged()
}

@HiltViewModel
class SpaceResumeViewModel @Inject constructor(
	stateSource: SpaceResumeStateSource,
	private val spaceRepository: SpaceRepository,
	private val catalogRepository: SpaceCatalogRepository,
	private val historyRepository: HistoryRepository,
) : BaseViewModel() {

	val onOpenReader = MutableEventFlow<SpaceResumeRequest>()

	val uiState = stateSource.observe().stateIn(
		scope = viewModelScope + Dispatchers.Default,
		started = SharingStarted.WhileSubscribed(5_000),
		initialValue = SpaceResumeUiState(),
	)

	fun resume(spaceId: SpaceId) {
		launchLoadingJob(Dispatchers.Default) {
			val item = uiState.value.items[spaceId]?.takeIf(SpaceResumeItem::canResume)
				?: withTimeoutOrNull(2_000L) {
					uiState.map { state ->
						state.items[spaceId]?.takeIf(SpaceResumeItem::canResume)
					}.first { it != null }
				}
				?: return@launchLoadingJob
			spaceRepository.activate(spaceId)
			val history = historyRepository.getOne(item.content)
			onOpenReader.call(
				SpaceResumeRequest(
					content = item.content,
					contentType = catalogRepository.find(spaceId)?.kind?.toContentType(),
					state = history?.let(::ReaderState),
				),
			)
		}
	}
}

private fun SpaceKind.toContentType(): ContentType = when (this) {
	SpaceKind.MANGA -> ContentType.MANGA
	SpaceKind.NOVEL -> ContentType.NOVEL
	SpaceKind.ANIME -> ContentType.VIDEO
}

internal fun buildSpaceResumeUiState(
	recent: Map<SpaceId, Content?>,
	isOnline: Boolean,
	resumeEnabled: Boolean,
): SpaceResumeUiState {
	return SpaceResumeUiState(
		items = recent.mapNotNull { (spaceId, content) ->
			content?.let {
				spaceId to SpaceResumeItem(
					spaceId = spaceId,
					title = it.title,
					content = it,
					canResume = resumeEnabled && (isOnline || it.isLocal),
				)
			}
		}.toMap(),
	)
}
