package com.mangaverse.app.settings.sources.extensions

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.mangaverse.app.R
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.util.ext.MutableEventFlow
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.core.util.ext.require
import com.mangaverse.app.extensions.install.ExtensionInstallResult
import com.mangaverse.app.extensions.install.ExtensionInstallService
import com.mangaverse.app.extensions.repo.ExternalExtensionRepoRepository
import com.mangaverse.app.extensions.repo.ExternalExtensionType
import com.mangaverse.app.extensions.repo.RepoAvailableExtension
import com.mangaverse.app.mihon.MihonExtensionManager
import com.mangaverse.app.extensions.repo.InstalledExtensionSignatureValidator
import javax.inject.Inject

@HiltViewModel
class AvailableExtensionsViewModel @Inject constructor(
	savedStateHandle: SavedStateHandle,
	@ApplicationContext private val appContext: Context,
	private val repoRepository: ExternalExtensionRepoRepository,
	private val installService: ExtensionInstallService,
	private val mihonExtensionManager: MihonExtensionManager,
	private val signatureValidator: InstalledExtensionSignatureValidator,
) : BaseViewModel() {

	val type: ExternalExtensionType = enumValueOf(savedStateHandle.require<String>(ARG_EXTENSION_TYPE))

	private val rawExtensions = MutableStateFlow<List<RepoAvailableExtension>>(emptyList())

	val repoCount: StateFlow<Int> = repoRepository.observeByType(type)
		.map { it.size }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

	private val installedExtensions: StateFlow<Map<String, InstalledExtensionVersionInfo>> = observeInstalledExtensionInfoMap(
		type = type,
		mihonExtensionManager = mihonExtensionManager,
	).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

	val items: StateFlow<List<AvailableExtensionListItem>> = combine(
		rawExtensions,
		installedExtensions,
		installService.downloadStates,
	) { available, installed, downloads ->
		available.map { extension ->
			val installedSearchKey = type.normalizePackageNameForMatching(extension.pkgName)
			val installedInfo = installed[installedSearchKey]
			val state = extension.resolveAvailableState(
				installedInfo = installedInfo,
				isInstalling = extension.pkgName in downloads,
			)
			AvailableExtensionListItem(
				extension = extension,
				installedVersionName = installedInfo?.versionName,
				state = state,
			)
		}
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

	val availableCount: StateFlow<Int> = items.map { it.size }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

	val updateCount: StateFlow<Int> = items.map { list -> list.count { it.state == AvailableExtensionState.UPDATE_AVAILABLE } }
		.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

	val onInstallIntent = MutableEventFlow<Intent>()
	val onMessage = MutableEventFlow<String>()

	init {
		refresh()
	}

	fun refresh() {
		launchLoadingJob(Dispatchers.IO) {
			repoRepository.refresh(type)
			rawExtensions.value = repoRepository.getAvailableExtensions(type)
		}
	}

	fun install(item: AvailableExtensionListItem) {
		val extension = item.extension
		if (item.state == AvailableExtensionState.INSTALLED || extension.pkgName in installService.downloadStates.value) {
			return
		}
		launchLoadingJob(Dispatchers.IO) {
			try {
				when (val result = installService.install(extension)) {
					is ExtensionInstallResult.RequiresInstaller -> onInstallIntent.call(result.intent)
					ExtensionInstallResult.Completed -> {
						refresh()
						onMessage.call(appContext.getString(R.string.unified_sources_package_installed))
					}
				}
			} catch (e: Throwable) {
				errorEvent.call(e)
			}
		}
	}
}

data class AvailableExtensionListItem(
	val extension: RepoAvailableExtension,
	val installedVersionName: String?,
	val state: AvailableExtensionState,
)

enum class AvailableExtensionState {
	AVAILABLE,
	UPDATE_AVAILABLE,
	INSTALLED,
	INSTALLING,
}
