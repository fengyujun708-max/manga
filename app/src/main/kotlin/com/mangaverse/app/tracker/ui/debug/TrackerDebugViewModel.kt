package com.mangaverse.app.tracker.ui.debug

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.tracker.domain.TrackingRepository
import javax.inject.Inject

@HiltViewModel
class TrackerDebugViewModel @Inject constructor(
	repository: TrackingRepository,
) : BaseViewModel() {

	val content = repository.observeTrackDebugItems()
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())
}
