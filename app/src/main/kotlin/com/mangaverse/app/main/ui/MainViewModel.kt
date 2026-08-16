package com.mangaverse.app.main.ui

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.mangaverse.app.core.exceptions.EmptyHistoryException
import com.mangaverse.app.core.parser.ContentDataRepository
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.observeAsFlow
import com.mangaverse.app.core.prefs.observeAsStateFlow
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.util.ext.MutableEventFlow
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.history.data.HistoryRepository
import com.mangaverse.app.main.domain.ReadingResumeEnabledUseCase
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.reader.ui.ReaderState
import com.mangaverse.app.tracker.domain.TrackingRepository
import com.mangaverse.app.work.domain.WorkResolver
import javax.inject.Inject

data class MainReaderRequest(
	val content: Content,
	val state: ReaderState?,
)

@HiltViewModel
class MainViewModel @Inject constructor(
	private val historyRepository: HistoryRepository,
	trackingRepository: TrackingRepository,
	private val settings: AppSettings,
	readingResumeEnabledUseCase: ReadingResumeEnabledUseCase,
	private val sourcesRepository: ContentSourcesRepository,
	private val contentDataRepository: ContentDataRepository,
	private val workResolver: WorkResolver,
) : BaseViewModel() {

	val onOpenReader = MutableEventFlow<MainReaderRequest>()
	val onFirstStart = MutableEventFlow<Unit>()

	private val _topBarHeightPx = MutableStateFlow(0)
	val topBarHeightPx = _topBarHeightPx.asStateFlow()

	private val _bottomNavHeightPx = MutableStateFlow(0)
	val bottomNavHeightPx = _bottomNavHeightPx.asStateFlow()

	private val _topContentInsetPx = MutableStateFlow(0)
	val topContentInsetPx = _topContentInsetPx.asStateFlow()

	private val _bottomContentInsetPx = MutableStateFlow(0)
	val bottomContentInsetPx = _bottomContentInsetPx.asStateFlow()

	fun setTopBarHeightPx(height: Int) {
		_topBarHeightPx.value = height
	}

	fun setBottomNavHeightPx(height: Int) {
		_bottomNavHeightPx.value = height
	}

	fun setContentInsetsPx(top: Int, bottom: Int) {
		_topContentInsetPx.value = top
		_bottomContentInsetPx.value = bottom
	}

	val isResumeEnabled = readingResumeEnabledUseCase()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.WhileSubscribed(5000), false)

	val lastReadContent = settings.observeAsFlow(
		AppSettings.KEY_HISTORY_EXCLUDE_NSFW,
	) { isHistoryExcludeNsfw }
		.flatMapLatest { excludeNsfw ->
			historyRepository.observeLast(excludeNsfw = excludeNsfw)
		}
		.withErrorHandling()
		.stateIn(
			scope = viewModelScope + Dispatchers.Default,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = null,
		)

	val feedCounter = trackingRepository.observeUnreadUpdatesCount()
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Lazily, 0)

	val isBottomNavPinned = flowOf(true).flowOn(Dispatchers.Default)

	val isNavFloating = settings.observeAsFlow(
		AppSettings.KEY_NAV_FLOATING,
	) {
		isNavFloating
	}.flowOn(Dispatchers.Default)

	val navHeight = settings.observe(
		AppSettings.KEY_NAV_HEIGHT,
		AppSettings.KEY_NAV_FLOATING_HEIGHT,
		AppSettings.KEY_NAV_FLOATING,
	).map {
		val floating = settings.isNavFloating
		if (floating) {
			settings.navFloatingHeight
		} else {
			settings.navHeight
		}
	}.flowOn(Dispatchers.Default)

	val isIncognitoModeEnabled = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_INCOGNITO_MODE,
		valueProducer = { isIncognitoModeEnabled },
	)

	init {
		launchJob(Dispatchers.Default) {
			if (sourcesRepository.isSetupRequired()) {
				onFirstStart.call(Unit)
			}
		}
	}

	fun openLastReader() {
		launchLoadingJob(Dispatchers.Default) {
			val rawContent = historyRepository.getLastOrNull(
				excludeNsfw = settings.isHistoryExcludeNsfw,
			) ?: throw EmptyHistoryException()
			val history = historyRepository.getOne(rawContent)
			val entityId = workResolver.resolveByMangaId(rawContent.id).entityId
			val preferredLocalMangaId = entityId?.let { workResolver.selectPreferredProjection(it) }
			val resolvedBase = preferredLocalMangaId
				?.takeIf { it != rawContent.id }
				?.let { contentDataRepository.findDisplayContentById(it, withChapters = false) }
				?: rawContent
			onOpenReader.call(
				MainReaderRequest(
					content = resolvedBase,
					state = history?.let(::ReaderState),
				),
			)
		}
	}

	fun setIncognitoMode(isEnabled: Boolean) {
		settings.isIncognitoModeEnabled = isEnabled
	}
}
