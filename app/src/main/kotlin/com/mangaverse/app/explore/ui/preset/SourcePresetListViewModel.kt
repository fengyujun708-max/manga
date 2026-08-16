package com.mangaverse.app.explore.ui.preset

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.observeAsStateFlow
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.util.ext.MutableEventFlow
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.explore.data.SourcePreset
import com.mangaverse.app.explore.data.SourcePresetsRepository
import javax.inject.Inject

@HiltViewModel
class SourcePresetListViewModel @Inject constructor(
	private val presetsRepository: SourcePresetsRepository,
	private val settings: AppSettings,
	private val sourcesRepository: ContentSourcesRepository,
) : BaseViewModel() {

	val presets: StateFlow<List<SourcePreset>> = presetsRepository.observeAll()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val onPresetDeleted = MutableEventFlow<Unit>()

	val activePresetId: StateFlow<Long> = settings.observeAsStateFlow(
		scope = viewModelScope + Dispatchers.Default,
		key = AppSettings.KEY_ACTIVE_SOURCE_PRESET_ID,
		valueProducer = { activeSourcePresetId },
	)

	fun setActivePreset(presetId: Long) {
		settings.activeSourcePresetId = presetId
	}

	fun countSourcesForPreset(preset: SourcePreset): Int {
		return preset.sources.size
	}

	fun deletePreset(presetId: Long) {
		launchJob(Dispatchers.Default) {
			if (settings.activeSourcePresetId == presetId) {
				settings.activeSourcePresetId = -1L
			}
			presetsRepository.deletePreset(presetId)
			onPresetDeleted.call(Unit)
		}
	}
}
