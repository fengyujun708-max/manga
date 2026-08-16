package com.mangaverse.app.settings.sources

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import kotlinx.coroutines.flow.StateFlow
import com.mangaverse.app.core.extensions.GlobalExtensionManager
import com.mangaverse.app.core.extensions.jarBaseName
import com.mangaverse.app.core.extensions.resolveJarPriorityOrder
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.R
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.explore.data.ContentSourcesRepository
import javax.inject.Inject

@HiltViewModel
class SourcesSettingsViewModel @Inject constructor(
	sourcesRepository: ContentSourcesRepository,
	@ApplicationContext private val context: Context,
	private val settings: AppSettings,
) : BaseViewModel() {

	val enabledSourcesCount = sourcesRepository.observeEnabledSourcesCount()
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, -1)

	val availableSourcesCount = sourcesRepository.observeAvailableSourcesCount()
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, -1)

	val builtInSourcesCount = sourcesRepository.observeBuiltInSourcesCount()
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, 0)

	val jsonSourcesCount = sourcesRepository.observeJsonSourcesCount()
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, 0)

	val mihonSourcesCount = sourcesRepository.observeMihonSourcesCount()
		.withErrorHandling()
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, 0)

	val installedJarNames: StateFlow<List<String>> = GlobalExtensionManager.installedJarNames

	init {
		loadPlugins()
	}

	val installedPlugins = MutableLiveData<List<File>>()

	fun loadPlugins() {
		viewModelScope.launch(Dispatchers.IO) {
			val pluginsDir = File(context.filesDir, "plugins")
			val jarFiles = pluginsDir
				.listFiles { file -> file.extension.equals("jar", ignoreCase = true) }
				?.sortedBy { it.name.lowercase() }
				.orEmpty()
			installedPlugins.postValue(jarFiles)
		}
	}

	fun resolveJarPriorityOrder(currentValue: String): List<String> {
		return resolveJarPriorityOrder(installedJarNames.value, currentValue)
	}

	fun persistJarPriorityOrder(jarNames: List<String>) {
		val normalized = jarNames
			.map { it.jarBaseName() }
			.distinct()
			.joinToString(",")
		settings.jarPriorityOrder = normalized
	}

	fun deletePlugin(file: File) {
		viewModelScope.launch(Dispatchers.IO) {
			if (file.delete()) {
				GlobalExtensionManager.initialize(context)
				loadPlugins()
			}
		}
	}

	fun importPlugin(uri: Uri, onResult: (Result<String>) -> Unit) {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val documentFile = DocumentFile.fromSingleUri(context, uri) ?: throw Exception("Invalid file URI")
				val fileName = documentFile.name ?: "plugin_${System.currentTimeMillis()}.jar"
				val pluginsDir = File(context.filesDir, "plugins")
				if (!pluginsDir.exists()) pluginsDir.mkdirs()
				
				val destinationFile = File(pluginsDir, fileName)
				context.contentResolver.openInputStream(uri)?.use { input ->
					destinationFile.outputStream().use { output ->
						input.copyTo(output)
					}
				} ?: throw Exception("Cannot open input stream")
				
				// Re-initialize manager
				GlobalExtensionManager.initialize(context)
				launch(Dispatchers.Main) {
					onResult(Result.success(fileName))
				}
			} catch (e: Exception) {
				launch(Dispatchers.Main) {
					onResult(Result.failure(e))
				}
			}
		}
	}
}
