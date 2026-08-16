package com.mangaverse.app.main.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import com.mangaverse.app.core.model.isLocal
import com.mangaverse.app.core.os.NetworkState
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.observeAsFlow
import com.mangaverse.app.history.data.HistoryRepository
import javax.inject.Inject

class ReadingResumeEnabledUseCase @Inject constructor(
	private val networkState: NetworkState,
	private val historyRepository: HistoryRepository,
	private val settings: AppSettings,
) {

	operator fun invoke(): Flow<Boolean> = combine(
		networkState,
		settings.observeAsFlow(AppSettings.KEY_HISTORY_EXCLUDE_NSFW) { isHistoryExcludeNsfw }
			.flatMapLatest { excludeNsfw ->
				historyRepository.observeLast(excludeNsfw = excludeNsfw)
			},
	) { isOnline, last ->
		last != null && (isOnline || last.isLocal)
	}.distinctUntilChanged()
}
