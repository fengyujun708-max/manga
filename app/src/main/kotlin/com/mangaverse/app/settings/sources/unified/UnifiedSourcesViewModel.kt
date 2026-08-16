package com.mangaverse.app.settings.sources.unified

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import com.mangaverse.app.R
import com.mangaverse.app.core.util.ext.getDisplayMessage
import com.mangaverse.app.core.db.entity.JsonSourceEntity
import com.mangaverse.app.core.db.entity.JsonSourceType
import com.mangaverse.app.core.extensions.GlobalExtensionManager
import com.mangaverse.app.core.jsonsource.JsonSourceImportMetadata
import com.mangaverse.app.core.jsonsource.JsonSourceManager
import com.mangaverse.app.core.model.ContentSourceAvailability
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.network.jsonsource.JsonSourceHttpClient
import com.mangaverse.app.core.network.jsonsource.LegadoHttpClient
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.ui.BaseViewModel
import com.mangaverse.app.core.util.ext.call
import com.mangaverse.app.core.util.ext.getDisplayMessage
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.explore.data.SourceAvailabilityRepository
import com.mangaverse.app.extensions.runtime.LocalApkExtensionSupport
import com.mangaverse.app.extensions.install.ExtensionInstallDownloadState
import com.mangaverse.app.extensions.install.ExtensionInstallMode
import com.mangaverse.app.extensions.install.ExtensionInstallResult
import com.mangaverse.app.extensions.install.ExtensionInstallService
import com.mangaverse.app.extensions.repo.ExternalExtensionRepo
import com.mangaverse.app.extensions.repo.ExternalExtensionRepoRepository
import com.mangaverse.app.extensions.repo.ExternalExtensionType
import com.mangaverse.app.extensions.repo.InstalledExtensionSignatureValidator
import com.mangaverse.app.extensions.repo.RepoAvailableExtension
import com.mangaverse.app.mihon.MihonExtensionManager
import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.parsers.model.ContentListFilter
import com.mangaverse.app.parsers.util.runCatchingCancellable
import com.mangaverse.app.settings.sources.extensions.ExtensionBatchUpdateStateMachine
import com.mangaverse.app.settings.sources.extensions.isNewerThanInstalled
import com.mangaverse.app.settings.sources.extensions.normalizeExtensionLanguageCode
import com.mangaverse.app.settings.sources.extensions.normalizePackageNameForMatching
import javax.inject.Inject

private const val TAG = "UnifiedSourcesVM"
private const val REFRESH_PACKAGES_TIMEOUT_MS = 30_000L
private const val SOURCE_TEST_TIMEOUT_MS = 45_000L
private const val SOURCE_TEST_MAX_PARALLELISM = 3

@HiltViewModel
class UnifiedSourcesViewModel @Inject constructor(
	@ApplicationContext private val appContext: Context,
	private val catalogRepository: UnifiedSourceCatalogRepository,
	private val contentSourcesRepository: ContentSourcesRepository,
	private val sourceAvailabilityRepository: SourceAvailabilityRepository,
	private val contentRepositoryFactory: ContentRepository.Factory,
	private val jsonSourceManager: JsonSourceManager,
	private val legadoHttpClient: LegadoHttpClient,
	@JsonSourceHttpClient private val okHttpClient: OkHttpClient,
	private val extensionRepoRepository: ExternalExtensionRepoRepository,
	private val installService: ExtensionInstallService,
	private val signatureValidator: InstalledExtensionSignatureValidator,
	private val settings: AppSettings,
	private val mihonExtensionManager: MihonExtensionManager,
) : BaseViewModel() {

	private val availableExternalExtensions = MutableStateFlow<List<RepoAvailableExtension>>(emptyList())
	private val pendingUninstallIntents = ArrayDeque<Intent>()
	private val batchUpdateState = ExtensionBatchUpdateStateMachine()
	private val filterState = MutableStateFlow(
		UnifiedSourcesFilterState(
			languages = settings.extensionLanguages.normalizeLanguageCodes(),
		),
	)
	private val _events = MutableSharedFlow<UnifiedSourcesEvent>(extraBufferCapacity = 1)
	val events: SharedFlow<UnifiedSourcesEvent> = _events.asSharedFlow()
	val updateAllInProgress: StateFlow<Boolean> = batchUpdateState.inProgress

	val uiState: StateFlow<UnifiedSourcesUiState> = combine(
		catalogRepository.observeState(),
		availableExternalExtensions,
		installService.downloadStates,
		filterState,
	) { catalog, availableExtensions, downloadStates, filters ->
		catalog
			.withAvailableExternalPackages(availableExtensions, downloadStates)
			.toUiState(filters)
	}.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5000),
		initialValue = UnifiedSourcesUiState.Loading,
	)

	fun setSearchQuery(query: String) {
		filterState.update { it.copy(query = query) }
	}

	fun toggleKind(kind: UnifiedSourceKind) {
		filterState.update { state ->
			state.copy(kinds = state.kinds.toggle(kind))
		}
	}

	fun setKindFilter(kind: UnifiedSourceKind?) {
		filterState.update { state ->
			state.copy(kinds = kind?.let(::setOf) ?: emptySet())
		}
	}

	fun toggleContentType(contentType: ContentType) {
		filterState.update { state ->
			state.copy(contentTypes = state.contentTypes.toggle(contentType))
		}
	}

	fun setPrimaryContentTypeFilter(contentType: ContentType?) {
		setContentTypeFilter(contentType)
	}

	fun setContentTypeFilter(contentType: ContentType?) {
		filterState.update { state ->
			state.copy(contentTypes = contentType?.let(::setOf) ?: emptySet())
		}
	}

	fun toggleLocationType(locationType: UnifiedRepositoryLocationType) {
		filterState.update { state ->
			state.copy(locationTypes = state.locationTypes.toggle(locationType))
		}
	}

	fun toggleLanguage(language: String) {
		val normalized = language.normalizeLanguageCode()
		if (normalized.isBlank()) {
			return
		}
		filterState.update { state ->
			state.copy(languages = state.languages.toggle(normalized))
		}
	}

	fun setEnabledFilter(filter: UnifiedEnabledFilter) {
		filterState.update { it.copy(enabledFilter = filter) }
	}

	fun setAvailabilityFilter(filter: UnifiedAvailabilityFilter) {
		filterState.update { it.copy(availabilityFilter = filter) }
	}

	fun setTestAvailabilityFilter(filter: UnifiedTestAvailabilityFilter) {
		filterState.update { it.copy(testAvailabilityFilter = filter) }
	}

	fun setNsfwFilter(filter: UnifiedNsfwFilter) {
		filterState.update { it.copy(nsfwFilter = filter) }
	}

	fun clearLanguages() {
		filterState.update { it.copy(languages = emptySet()) }
	}

	fun applyPreferredLanguages() {
		filterState.update {
			val availableLanguages = (uiState.value as? UnifiedSourcesUiState.Ready)
				?.availableLanguages
				.orEmpty()
				.toSet()
			it.copy(
				languages = settings.contentLanguages.normalizeLanguageCodes()
					.filterTo(LinkedHashSet()) { language -> language in availableLanguages },
			)
		}
	}

	fun clearFilters() {
		filterState.value = UnifiedSourcesFilterState(
			availabilityFilter = UnifiedAvailabilityFilter.AVAILABLE,
		)
	}

	fun refreshPackages(
		refreshRepositories: Boolean = true,
		showLoading: Boolean = true,
	) {
		val refreshBlock: suspend kotlinx.coroutines.CoroutineScope.() -> Unit = {
			try {
				withTimeout(REFRESH_PACKAGES_TIMEOUT_MS) {
					val types = externalExtensionTypes()
					if (refreshRepositories) {
						types.forEach { type -> extensionRepoRepository.refresh(type) }
					}
					refreshAvailableExternalPackages(types)
					if (showLoading) {
						emitRefreshFailures(types)
					}
				}
			} catch (e: TimeoutCancellationException) {
				if (showLoading) {
					emitMessage(appContext.getString(R.string.unified_sources_refresh_timeout))
				}
			}
		}
		if (showLoading) {
			launchLoadingJob(Dispatchers.IO, block = refreshBlock)
		} else {
			launchJob(Dispatchers.IO, block = refreshBlock)
		}
	}

	private suspend fun emitRefreshFailures(types: List<ExternalExtensionType>) {
		val failedRepositories = types
			.flatMap { type -> extensionRepoRepository.getByType(type) }
			.filter { !it.lastError.isNullOrBlank() }
			.map { it.displayName }
			.distinct()
		if (failedRepositories.isNotEmpty()) {
			emitMessage(
				appContext.getString(
					R.string.unified_sources_refresh_partial_failed,
					failedRepositories.take(3).joinToString(", "),
				),
			)
		}
	}

	fun installPackage(packageId: String) {
		installPackage(packageId, ExtensionInstallMode.LOCAL_APK)
	}

	fun installPackage(packageId: String, mode: ExtensionInstallMode) {
		val item = currentPackage(packageId) ?: return
		if (item.state == UnifiedSourcePackageState.INSTALLED || item.packageName in installService.downloadStates.value) {
			return
		}
		requestInstall(item, fromBatch = false, mode = mode)
	}

	fun installPackageWithSystemInstaller(packageId: String) {
		installPackage(packageId, ExtensionInstallMode.SYSTEM)
	}

	fun cancelPackageInstall(packageId: String) {
		val item = currentPackage(packageId) ?: return
		val packageName = item.packageName ?: return
		if (batchUpdateState.shouldCancelCurrent(packageName)) {
			cancelUpdateAll()
			return
		}
		installService.cancelDownload(packageName)
	}

	fun uninstallPackage(packageId: String) {
		val item = currentPackage(packageId) ?: return
		val ready = uiState.value as? UnifiedSourcesUiState.Ready ?: return
		launchLoadingJob(Dispatchers.IO) {
			val result = removePackage(item, ready)
			if (result.reloadExternalExtensionManagers) {
				reloadExternalExtensionManagers()
			}
			if (result.removedDirectly) {
				emitMessage(appContext.getString(R.string.removal_completed))
			}
			result.uninstallIntent?.let { dispatchUninstallIntents(listOf(it)) }
		}
	}

	fun deletePackages(packageIds: Set<String>) {
		if (packageIds.isEmpty()) {
			return
		}
		val ready = uiState.value as? UnifiedSourcesUiState.Ready ?: return
		val packageItems = ready.allPackages
			.filter { it.id in packageIds }
			.distinctBy { it.id }
		if (packageItems.isEmpty()) {
			return
		}
		launchLoadingJob(Dispatchers.IO) {
			val uninstallIntents = ArrayList<Intent>(packageItems.size)
			var removedDirectly = false
			var reloadExternalManagers = false
			packageItems.forEach { item ->
				val result = removePackage(item, ready)
				removedDirectly = removedDirectly || result.removedDirectly
				reloadExternalManagers = reloadExternalManagers || result.reloadExternalExtensionManagers
				result.uninstallIntent?.let(uninstallIntents::add)
			}
			if (reloadExternalManagers) {
				reloadExternalExtensionManagers()
			}
			if (removedDirectly) {
				emitMessage(appContext.getString(R.string.removal_completed))
			}
			dispatchUninstallIntents(uninstallIntents)
		}
	}

	fun onPackagePrimaryAction(packageId: String) {
		when (val item = currentPackage(packageId)?.state) {
			UnifiedSourcePackageState.AVAILABLE,
			UnifiedSourcePackageState.UPDATE_AVAILABLE -> installPackage(packageId)

			UnifiedSourcePackageState.UNTRUSTED,
			UnifiedSourcePackageState.INCOMPATIBLE -> currentPackage(packageId)?.let {
				_events.tryEmit(UnifiedSourcesEvent.PackageStateDetails(it))
			}

			UnifiedSourcePackageState.INSTALLING,
			UnifiedSourcePackageState.INSTALLED,
			null -> Unit
		}
	}

	fun onUpdateAllPackagesAction() {
		if (updateAllInProgress.value) {
			cancelUpdateAll()
		} else {
			startUpdateAll()
		}
	}

	fun onInstallActivityResult() {
		handleBatchNextAction(batchUpdateState.onInstallActivityResult())
	}

	fun onUninstallActivityResult() {
		viewModelScope.launch {
			dispatchNextPendingUninstall()
		}
	}

	fun importLocalJar(uri: Uri) {
		launchLoadingJob(Dispatchers.IO) {
			val fileName = resolveDisplayName(uri)
				?.takeIf { it.isNotBlank() }
				?: "plugin_${System.currentTimeMillis()}.jar"
			val pluginsDir = File(appContext.filesDir, "plugins").apply { mkdirs() }
			val destinationFile = File(pluginsDir, fileName)
			appContext.contentResolver.openInputStream(uri)?.use { input ->
				destinationFile.outputStream().use { output ->
					input.copyTo(output)
				}
			} ?: throw IllegalArgumentException(appContext.getString(R.string.unified_sources_cannot_open_selected_jar))
			GlobalExtensionManager.initialize(appContext)
			emitMessage(appContext.getString(R.string.unified_sources_imported_plugin, fileName))
		}
	}

	fun addRepositoryFromUrl(kind: UnifiedSourceKind, url: String, title: String? = null) {
		val cleanUrl = url.trim()
		if (cleanUrl.isBlank()) return
		launchLoadingJob(Dispatchers.IO) {
			when (kind) {
				UnifiedSourceKind.LEGADO,
				UnifiedSourceKind.JS -> {
					importJsonRepository(
						kind = kind,
						content = fetchRemoteText(cleanUrl),
						sourceLocator = cleanUrl,
						sourceTitle = title,
					)
				}
				UnifiedSourceKind.MIHON,
				UnifiedSourceKind.JAR -> prepareExternalRepository(kind, cleanUrl)
				UnifiedSourceKind.NATIVE -> emitMessage(appContext.getString(R.string.unified_sources_native_no_repository))
			}
		}
	}

	fun addRepositoryFromFile(kind: UnifiedSourceKind, uri: Uri) {
		launchLoadingJob(Dispatchers.IO) {
			val title = resolveDisplayName(uri)
			val content = appContext.contentResolver.openInputStream(uri)
				?.bufferedReader()
				?.use { it.readText() }
				?: throw IllegalArgumentException(appContext.getString(R.string.unified_sources_cannot_open_selected_file))
			importJsonRepository(
				kind = kind,
				content = content,
				sourceLocator = uri.toString(),
				sourceTitle = title,
			)
		}
	}

	fun addRepositoryFromInline(kind: UnifiedSourceKind, content: String, title: String? = null) {
		val cleanContent = content.trim()
		if (cleanContent.isBlank()) return
		launchLoadingJob(Dispatchers.Default) {
			val inlineLocator = title
				?.trim()
				?.takeIf { it.isNotBlank() }
				?: "inline:${kind.name.lowercase()}:${System.currentTimeMillis()}"
			importJsonRepository(
				kind = kind,
				content = cleanContent,
				sourceLocator = inlineLocator,
				sourceTitle = title,
			)
		}
	}

	fun refreshRepository(repositoryId: String) {
		val repository = (uiState.value as? UnifiedSourcesUiState.Ready)
			?.allRepositories
			?.firstOrNull { it.id == repositoryId }
			?: return
		launchLoadingJob(Dispatchers.IO) {
			when (repository.kind) {
				UnifiedSourceKind.LEGADO,
				UnifiedSourceKind.JS -> {
					if (repository.locationType == UnifiedRepositoryLocationType.INLINE_IMPORT ||
						repository.locationType == UnifiedRepositoryLocationType.PRESET_ONLY
					) {
						emitMessage(appContext.getString(R.string.unified_sources_repository_manual_refresh_only))
						return@launchLoadingJob
					}
					importJsonRepository(
						kind = repository.kind,
						content = loadRepositoryText(repository.url),
						sourceLocator = repository.url,
						sourceTitle = repository.name,
					)
				}
				UnifiedSourceKind.MIHON,
				UnifiedSourceKind.JAR -> refreshExternalRepository(repository)
				UnifiedSourceKind.NATIVE -> emitMessage(appContext.getString(R.string.unified_sources_native_no_repository))
			}
		}
	}

	fun deleteRepository(repositoryId: String) {
		val ready = uiState.value as? UnifiedSourcesUiState.Ready ?: return
		val repository = ready.allRepositories.firstOrNull { it.id == repositoryId } ?: return
		launchLoadingJob(Dispatchers.IO) {
			when (repository.kind) {
				UnifiedSourceKind.LEGADO,
				UnifiedSourceKind.JS -> {
					val sourceIds = ready.allSources
						.filter { it.repositoryId == repository.id }
						.map { it.id }
					val ids = if (sourceIds.isNotEmpty()) {
						sourceIds
					} else {
						jsonSourceManager.observeAllJsonSources()
							.first()
							.filter { it.jsonRepositoryIdForAction() == repository.id }
							.map { it.id }
					}
					if (ids.isNotEmpty()) {
						jsonSourceManager.deleteSourcesBatch(ids)
					}
					emitMessage(appContext.getString(R.string.unified_sources_repository_sources_deleted))
				}
				UnifiedSourceKind.MIHON,
				UnifiedSourceKind.JAR -> deleteExternalRepository(repository)
				UnifiedSourceKind.NATIVE -> emitMessage(appContext.getString(R.string.unified_sources_native_no_repository))
			}
		}
	}

	fun confirmExternalRepository(repo: ExternalExtensionRepo) {
		launchLoadingJob(Dispatchers.IO) {
			when (val result = extensionRepoRepository.confirmAddRepo(repo)) {
				is ExternalExtensionRepoRepository.AddRepoResult.Success -> {
					emitMessage(appContext.getString(R.string.extension_repo_added_message, result.repo.displayName))
					extensionRepoRepository.refresh(repo.type)
					refreshAvailableExternalPackages(listOf(repo.type))
				}
				is ExternalExtensionRepoRepository.AddRepoResult.DuplicateFingerprint -> emitMessage(
					appContext.getString(
						R.string.extension_repo_duplicate_fingerprint_message,
						result.existingRepo.displayName,
					),
				)
				is ExternalExtensionRepoRepository.AddRepoResult.FetchFailed -> emitMessage(
					result.error.getDisplayMessage(appContext.resources),
				)
				ExternalExtensionRepoRepository.AddRepoResult.InvalidUrl -> emitMessage(
					appContext.getString(R.string.extension_repo_invalid_url_message),
				)
				ExternalExtensionRepoRepository.AddRepoResult.RepoAlreadyExists -> emitMessage(
					appContext.getString(R.string.extension_repo_already_exists_message),
				)
			}
		}
	}

	fun setSourceEnabled(sourceId: String, enabled: Boolean) {
		setSourcesEnabled(setOf(sourceId), enabled)
	}

	fun setSourcesEnabled(sourceIds: Set<String>, enabled: Boolean) {
		if (sourceIds.isEmpty()) {
			return
		}
		val ready = uiState.value as? UnifiedSourcesUiState.Ready ?: return
		val sourceItems = ready.allSources.filter { it.id in sourceIds }
		if (sourceItems.isEmpty()) {
			return
		}
		viewModelScope.launch(Dispatchers.Default) {
			if (!enabled && settings.isAllSourcesEnabled) {
				contentSourcesRepository.setSourcesEnabled(ready.allSources.map { it.source }, true)
				settings.isAllSourcesEnabled = false
			}
			contentSourcesRepository.setSourcesEnabled(sourceItems.map { it.source }, enabled)
		}
	}

	fun testSources(sourceIds: Set<String>) {
		if (sourceIds.isEmpty()) {
			return
		}
		val sourceItems = (uiState.value as? UnifiedSourcesUiState.Ready)
			?.allSources
			.orEmpty()
			.filter { it.id in sourceIds }
		if (sourceItems.isEmpty()) {
			return
		}
		launchLoadingJob(Dispatchers.IO) {
			val semaphore = Semaphore(SOURCE_TEST_MAX_PARALLELISM)
			val results = sourceItems.map { item ->
				async {
					val isAvailable = runCatchingCancellable {
						withTimeoutOrNull(SOURCE_TEST_TIMEOUT_MS) {
							semaphore.withPermit {
								val repository = contentRepositoryFactory.create(item.source)
								repository.getList(
									offset = 0,
									order = repository.defaultSortOrder,
									filter = ContentListFilter.EMPTY,
								).isNotEmpty()
							}
						} ?: false
					}.getOrDefault(false)
					item to isAvailable
				}
			}.awaitAll()
			results.forEach { (item, isAvailable) ->
				sourceAvailabilityRepository.setAvailability(
					item.source,
					if (isAvailable) ContentSourceAvailability.AVAILABLE else ContentSourceAvailability.EMPTY,
				)
			}
			emitMessage(
				appContext.getString(
					R.string.source_test_completed,
					results.count { it.second },
					results.count { !it.second },
				),
			)
		}
	}

	fun setSourcePinned(sourceId: String, pinned: Boolean) {
		val source = (uiState.value as? UnifiedSourcesUiState.Ready)
			?.allSources
			?.firstOrNull { it.id == sourceId }
			?.source
			?: return
		viewModelScope.launch(Dispatchers.Default) {
			contentSourcesRepository.setIsPinned(setOf(source), pinned)
		}
	}

	private fun currentPackage(packageId: String): UnifiedSourcePackageItem? {
		return (uiState.value as? UnifiedSourcesUiState.Ready)
			?.allPackages
			?.firstOrNull { it.id == packageId }
	}

	private suspend fun removePackage(
		item: UnifiedSourcePackageItem,
		ready: UnifiedSourcesUiState.Ready,
	): PackageRemovalResult {
		if (item.state == UnifiedSourcePackageState.INSTALLING) {
			return PackageRemovalResult()
		}

		if (item.kind.isJsonBackedKind()) {
			val uiSourceIds = ready.allSources
				.filter { it.packageId == item.id }
				.map { it.id }
			val sourceIds = if (uiSourceIds.isNotEmpty()) {
				uiSourceIds
			} else {
				jsonSourceManager.observeAllJsonSources()
					.first()
					.filter { it.jsonPackageIdForAction() == item.id }
					.map { it.id }
			}
			if (sourceIds.isEmpty()) {
				return PackageRemovalResult()
			}
			jsonSourceManager.deleteSourcesBatch(sourceIds)
			return PackageRemovalResult(removedDirectly = true)
		}

		val packageName = item.packageName ?: return PackageRemovalResult()
		if (item.kind == UnifiedSourceKind.JAR) {
			val pluginDir = File(appContext.filesDir, "plugins")
			val jarFile = File(pluginDir, "$packageName.jar")
			if (jarFile.exists()) {
				jarFile.delete()
			}
			appContext.getSharedPreferences("jar_plugin_versions", Context.MODE_PRIVATE)
				.edit()
				.remove(packageName)
				.apply()
			GlobalExtensionManager.initialize(appContext)
			return PackageRemovalResult(removedDirectly = true)
		}

		val ecosystem = item.kind.toLocalApkEcosystem()
		if (ecosystem != null && item.installLocation == UnifiedSourcePackageInstallLocation.LOCAL_APK) {
			val deleted = LocalApkExtensionSupport.deleteManagedLocalPackage(
				context = appContext,
				ecosystem = ecosystem,
				packageName = packageName,
			)
			if (deleted) {
				return PackageRemovalResult(
					removedDirectly = true,
					reloadExternalExtensionManagers = true,
				)
			}
		}

		return PackageRemovalResult(uninstallIntent = buildUninstallIntent(item.kind, packageName))
	}

	private fun buildUninstallIntent(kind: UnifiedSourceKind, packageName: String): Intent {
		val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			Intent.ACTION_DELETE
		} else {
			@Suppress("DEPRECATION")
			Intent.ACTION_UNINSTALL_PACKAGE
		}
		return Intent(action, Uri.fromParts("package", packageName, null))
	}

	private suspend fun dispatchUninstallIntents(intents: List<Intent>) {
		if (intents.isEmpty()) {
			return
		}
		val iterator = intents.iterator()
		if (pendingUninstallIntents.isEmpty()) {
			val first = iterator.next()
			while (iterator.hasNext()) {
				pendingUninstallIntents.addLast(iterator.next())
			}
			_events.emit(UnifiedSourcesEvent.StartUninstall(first))
			return
		}
		while (iterator.hasNext()) {
			pendingUninstallIntents.addLast(iterator.next())
		}
	}

	private suspend fun dispatchNextPendingUninstall() {
		val next = pendingUninstallIntents.removeFirstOrNull() ?: return
		_events.emit(UnifiedSourcesEvent.StartUninstall(next))
	}

	private fun requestInstall(
		item: UnifiedSourcePackageItem,
		fromBatch: Boolean,
		mode: ExtensionInstallMode = ExtensionInstallMode.LOCAL_APK,
	) {
		val extension = item.installPayload ?: return
		if (extension.pkgName in installService.downloadStates.value) {
			return
		}
		if (fromBatch) {
			batchUpdateState.beginInstall(extension.pkgName)
		}
		launchLoadingJob(Dispatchers.IO) {
			try {
				when (val result = installService.install(extension, mode)) {
					is ExtensionInstallResult.RequiresInstaller -> {
						if (fromBatch) {
							batchUpdateState.markInstallerIntentDispatched()
						}
						_events.emit(UnifiedSourcesEvent.StartInstall(result.intent))
					}
					ExtensionInstallResult.Completed -> {
						onPackageInstallCompleted(item, fromBatch)
					}
				}
			} catch (e: CancellationException) {
				if (!fromBatch) {
					emitMessage(appContext.getString(R.string.canceled))
				}
				if (fromBatch) {
					handleBatchNextAction(batchUpdateState.onInstallInterrupted())
				}
			} catch (e: Throwable) {
				_events.emit(UnifiedSourcesEvent.InstallFailed(e.getDisplayMessage(appContext.resources)))
				if (fromBatch) {
					emitMessage(appContext.getString(R.string.extension_update_failed, item.name))
					handleBatchNextAction(batchUpdateState.onInstallInterrupted())
				}
			}
		}
	}

	private suspend fun onPackageInstallCompleted(
		item: UnifiedSourcePackageItem,
		fromBatch: Boolean,
	) {
		if (item.kind.isHotReloadableExternalKind()) {
			reloadExternalExtensionManagers()
			refreshPackages(refreshRepositories = false, showLoading = false)
		}
		emitMessage(appContext.getString(R.string.unified_sources_package_installed))
		if (fromBatch) {
			handleBatchNextAction(batchUpdateState.onInstallInterrupted())
		}
	}

	private fun startUpdateAll() {
		val updatePackages = currentUpdatePackages()
		if (!batchUpdateState.start(updatePackages.mapNotNull { it.packageName })) {
			viewModelScope.launch { emitMessage(appContext.getString(R.string.no_extension_updates_available)) }
			return
		}
		handleBatchNextAction(batchUpdateState.nextAction())
	}

	private fun cancelUpdateAll() {
		if (!updateAllInProgress.value) {
			return
		}
		batchUpdateState.cancel(installService::cancelDownload)
		viewModelScope.launch { emitMessage(appContext.getString(R.string.extension_update_all_cancelled)) }
	}

	private fun handleBatchNextAction(action: ExtensionBatchUpdateStateMachine.NextAction) {
		when (action) {
			ExtensionBatchUpdateStateMachine.NextAction.None -> Unit
			ExtensionBatchUpdateStateMachine.NextAction.Completed -> {
				viewModelScope.launch { emitMessage(appContext.getString(R.string.extension_update_all_complete)) }
			}
			is ExtensionBatchUpdateStateMachine.NextAction.InstallNext -> {
				val item = currentUpdatePackages().firstOrNull { it.packageName == action.packageName } ?: run {
					handleBatchNextAction(batchUpdateState.nextAction())
					return
				}
				requestInstall(item, fromBatch = true)
			}
		}
	}

	private fun currentUpdatePackages(): List<UnifiedSourcePackageItem> {
		return (uiState.value as? UnifiedSourcesUiState.Ready)
			?.allPackages
			.orEmpty()
			.filter { it.state == UnifiedSourcePackageState.UPDATE_AVAILABLE }
	}

	private data class PackageRemovalResult(
		val removedDirectly: Boolean = false,
		val uninstallIntent: Intent? = null,
		val reloadExternalExtensionManagers: Boolean = false,
	)

	private suspend fun prepareExternalRepository(kind: UnifiedSourceKind, url: String) {
		val type = kind.toExternalExtensionType()
			?: throw IllegalArgumentException(
				appContext.getString(R.string.unified_sources_unsupported_repository_kind, kind.name),
			)
		when (val result = extensionRepoRepository.prepareAddRepo(type, url)) {
			is ExternalExtensionRepoRepository.PrepareAddRepoResult.Ready -> {
				_events.emit(UnifiedSourcesEvent.TrustExternalRepository(result.repo))
			}
			is ExternalExtensionRepoRepository.PrepareAddRepoResult.DuplicateFingerprint -> emitMessage(
				appContext.getString(
					R.string.extension_repo_duplicate_fingerprint_message,
					result.existingRepo.displayName,
				),
			)
			is ExternalExtensionRepoRepository.PrepareAddRepoResult.FetchFailed -> emitMessage(
				result.error.getDisplayMessage(appContext.resources),
			)
			ExternalExtensionRepoRepository.PrepareAddRepoResult.InvalidUrl -> emitMessage(
				appContext.getString(R.string.extension_repo_invalid_url_message),
			)
			ExternalExtensionRepoRepository.PrepareAddRepoResult.RepoAlreadyExists -> emitMessage(
				appContext.getString(R.string.extension_repo_already_exists_message),
			)
		}
	}

	private suspend fun refreshExternalRepository(repository: UnifiedSourceRepositoryItem) {
		val type = repository.kind.toExternalExtensionType()
			?: throw IllegalArgumentException(
				appContext.getString(R.string.unified_sources_unsupported_repository_kind, repository.kind.name),
			)
		val baseUrl = normalizeRepositoryUrlForAction(repository.url)
		val repo = extensionRepoRepository.getByType(type)
			.firstOrNull { normalizeRepositoryUrlForAction(it.baseUrl) == baseUrl }
		if (repo == null) {
			emitMessage(appContext.getString(R.string.unified_sources_repository_not_configured))
			return
		}
		extensionRepoRepository.refresh(repo)
		refreshAvailableExternalPackages(listOf(type))
		emitMessage(appContext.getString(R.string.unified_sources_repository_refreshed))
	}

	private suspend fun deleteExternalRepository(repository: UnifiedSourceRepositoryItem) {
		val type = repository.kind.toExternalExtensionType()
			?: throw IllegalArgumentException(
				appContext.getString(R.string.unified_sources_unsupported_repository_kind, repository.kind.name),
			)
		val baseUrl = normalizeRepositoryUrlForAction(repository.url)
		val repo = extensionRepoRepository.getByType(type)
			.firstOrNull { normalizeRepositoryUrlForAction(it.baseUrl) == baseUrl }
		if (repo == null) {
			emitMessage(appContext.getString(R.string.unified_sources_repository_not_configured))
			return
		}
		extensionRepoRepository.delete(repo)
		refreshAvailableExternalPackages()
		emitMessage(appContext.getString(R.string.unified_sources_repository_deleted))
	}

	private suspend fun importJsonRepository(
		kind: UnifiedSourceKind,
		content: String,
		sourceLocator: String?,
		sourceTitle: String?,
	) {
		val result = when (kind) {
			UnifiedSourceKind.LEGADO -> jsonSourceManager.importLegadoJson(
				jsonContent = content,
				sourceLocator = sourceLocator,
				sourceTitle = sourceTitle,
			)
			UnifiedSourceKind.JS -> jsonSourceManager.importJsSource(content)
			else -> Result.failure(
				IllegalArgumentException(
					appContext.getString(
						R.string.unified_sources_cannot_import_json,
						kind.displayNameForMessage(appContext),
					),
				),
			)
		}
		result
			.onSuccess { count ->
				emitMessage(appContext.getString(R.string.unified_sources_imported_sources, count))
			}
			.onFailure { error -> emitMessage(error.getDisplayMessage(appContext.resources)) }
	}

	private suspend fun loadRepositoryText(locator: String): String {
		return when (resolveRepositoryLocationTypeForAction(locator)) {
			UnifiedRepositoryLocationType.REMOTE_URL -> fetchRemoteText(locator)
			UnifiedRepositoryLocationType.LOCAL_FILE -> {
				appContext.contentResolver.openInputStream(Uri.parse(locator))
					?.bufferedReader()
					?.use { it.readText() }
					?: throw IllegalArgumentException(appContext.getString(R.string.unified_sources_cannot_open_repository_file))
			}
			UnifiedRepositoryLocationType.INLINE_IMPORT,
			UnifiedRepositoryLocationType.PRESET_ONLY -> throw IllegalArgumentException(
				appContext.getString(R.string.unified_sources_repository_cannot_refresh),
			)
		}
	}

	private suspend fun fetchRemoteText(url: String): String {
		val response = legadoHttpClient.get(url)
		return try {
			if (!response.isSuccessful) {
				throw IllegalArgumentException(appContext.getString(R.string.unified_sources_http_error, response.code))
			}
			response.body?.string()?.takeIf { it.isNotBlank() }
				?: throw IllegalArgumentException(appContext.getString(R.string.unified_sources_empty_response_body))
		} finally {
			response.close()
		}
	}

	private fun resolveDisplayName(uri: Uri): String? {
		return runCatching {
			appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
				?.use { cursor ->
					if (!cursor.moveToFirst()) return@use null
					val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
					cursor.getString(index.takeIf { it >= 0 } ?: return@use null)
				}
		}.getOrNull() ?: uri.lastPathSegment
	}

	private suspend fun emitMessage(message: String) {
		_events.emit(UnifiedSourcesEvent.Message(message))
	}

	private suspend fun refreshAvailableExternalPackages(types: List<ExternalExtensionType> = externalExtensionTypes()) {
		availableExternalExtensions.value = types
			.flatMap { type -> extensionRepoRepository.getCatalogExtensions(type) }
			.let { refreshed ->
				val refreshedTypes = types.toSet()
				availableExternalExtensions.value.filterNot { it.type in refreshedTypes } + refreshed
			}
	}

	private suspend fun reloadExternalExtensionManagers() {
		mihonExtensionManager.loadExtensions()
	}

	private fun UnifiedSourceCatalogState.withAvailableExternalPackages(
		availableExtensions: List<RepoAvailableExtension>,
		downloadStates: Map<String, ExtensionInstallDownloadState>,
	): UnifiedSourceCatalogState {
		val externalInstalledPackages = packages
			.filter { it.kind.isExternalExtensionKind() && !it.packageName.isNullOrBlank() }
		val installedByKey = externalInstalledPackages.associateBy { item ->
			item.kind.toExternalExtensionType()?.normalizePackageNameForMatching(item.packageName.orEmpty())
		}
		val handledInstalledKeys = LinkedHashSet<String>()
		val availablePackages = availableExtensions.map { extension ->
			val installedKey = extension.type.normalizePackageNameForMatching(extension.pkgName)
			val installedPackage = installedByKey[installedKey]
			if (installedPackage != null) {
				handledInstalledKeys += installedKey
			}
			extension.toUnifiedPackageItem(
				installedPackage = installedPackage,
				downloadState = downloadStates[extension.pkgName],
			)
		}
		val installedWithoutCatalogMatch = packages.filterNot { item ->
			item.kind.isExternalExtensionKind() &&
				item.kind.toExternalExtensionType()?.normalizePackageNameForMatching(item.packageName.orEmpty()) in handledInstalledKeys
		}
		return copy(
			packages = (installedWithoutCatalogMatch + availablePackages)
				.sortedWith(packageItemComparator)
				.withUniquePackageIds(),
		)
	}

	private fun RepoAvailableExtension.toUnifiedPackageItem(
		installedPackage: UnifiedSourcePackageItem?,
		downloadState: ExtensionInstallDownloadState?,
	): UnifiedSourcePackageItem {
		val isInstalled = installedPackage != null
		val isTrusted = installedPackage == null ||
			signatureValidator.isTrusted(installedPackage.packageName.orEmpty(), signatureHash)
		val state = when {
			downloadState != null -> UnifiedSourcePackageState.INSTALLING
			isInstalled && !isTrusted -> UnifiedSourcePackageState.UNTRUSTED
			!isCompatible -> UnifiedSourcePackageState.INCOMPATIBLE
			!isInstalled -> UnifiedSourcePackageState.AVAILABLE
			isNewerThanInstalled(installedPackage.versionCode) -> UnifiedSourcePackageState.UPDATE_AVAILABLE
			else -> UnifiedSourcePackageState.INSTALLED
		}
		val kind = type.toUnifiedKindForPackage()
		return UnifiedSourcePackageItem(
			id = installedPackage?.id ?: packageIdForAction(kind, pkgName),
			kind = kind,
			name = name,
			packageName = pkgName,
			repositoryId = repositoryIdForAction(kind, repoUrl),
			repositoryName = installedPackage?.repositoryName ?: repoName,
			versionName = versionName,
			versionCode = versionCode,
			libVersion = libVersion,
			language = lang.normalizeLanguageCode(),
			isInstalled = isInstalled,
			isNsfw = isNsfw,
			sourceCount = installedPackage?.sourceCount?.takeIf { it > 0 } ?: sourceNames.size,
			sourceNames = installedPackage?.sourceNames?.takeIf { it.isNotEmpty() } ?: sourceNames,
			iconUrl = iconUrl.takeIf { it.isNotBlank() } ?: installedPackage?.iconUrl,
			state = state,
			installedVersionName = installedPackage?.versionName,
			installProgressPercent = downloadState?.progressPercent,
			installLocation = installedPackage?.installLocation,
			installPayload = this,
		)
	}

	private fun UnifiedSourceCatalogState.toUiState(filters: UnifiedSourcesFilterState): UnifiedSourcesUiState.Ready {
		val repositoriesById = repositories.associateBy { it.id }
		val enrichedPackages = packages.withUniquePackageIds().enrichWithSourceCoverage(sources)
		val packagesById = enrichedPackages.associateBy { it.id }
		val visibleRepositories = repositories.filterBy(filters)
		val visiblePackages = enrichedPackages.filterBy(filters, repositoriesById)
		val visibleSources = sources.filterBy(filters, repositoriesById, packagesById)
		val availableLanguages = (enrichedPackages.mapNotNull { it.language } + sources.mapNotNull { it.language })
			.map { it.normalizeLanguageCode() }
			.filter { it.isNotBlank() }
			.distinct()
			.sorted()
		Log.d(
			TAG,
			"language filter availableLanguages=$availableLanguages selectedLanguages=${filters.languages}",
		)

		return UnifiedSourcesUiState.Ready(
			filters = filters,
			repositories = visibleRepositories,
			packages = visiblePackages,
			sources = visibleSources,
			allRepositories = repositories,
			allPackages = enrichedPackages,
			allSources = sources,
			availableKinds = (repositories.map { it.kind } + enrichedPackages.map { it.kind } + sources.map { it.kind })
				.distinct()
				.sortedBy { it.ordinal },
			availableContentTypes = sources.map { it.contentType }
				.distinct()
				.sortedBy { it.ordinal },
			availableLocationTypes = repositories.map { it.locationType }
				.distinct()
				.sortedBy { it.ordinal },
			availableLanguages = availableLanguages,
		)
	}

	private fun List<UnifiedSourcePackageItem>.enrichWithSourceCoverage(
		sources: List<UnifiedSourceItem>,
	): List<UnifiedSourcePackageItem> {
		if (isEmpty()) {
			return this
		}
		val activeSourceCountByPackageId = sources
			.asSequence()
			.mapNotNull { source -> source.packageId }
			.groupBy { it }
			.mapValues { (_, packageIds) -> packageIds.size }
		return map { item ->
			val declaredCount = item.sourceCount.coerceAtLeast(item.sourceNames.size)
			val supportsShadowedSources = item.kind == UnifiedSourceKind.JAR
			val activeCount = if (supportsShadowedSources) {
				(activeSourceCountByPackageId[item.id] ?: 0).coerceIn(0, declaredCount)
			} else {
				declaredCount
			}
			val shadowedCount = if (supportsShadowedSources) {
				(declaredCount - activeCount).coerceAtLeast(0)
			} else {
				0
			}
			item.copy(
				activeSourceCount = activeCount,
				shadowedSourceCount = shadowedCount,
			)
		}
	}

	private fun List<UnifiedSourceRepositoryItem>.filterBy(
		filters: UnifiedSourcesFilterState,
	): List<UnifiedSourceRepositoryItem> {
		val query = filters.query.trim()
		return asSequence()
			.filter { filters.kinds.isEmpty() || it.kind in filters.kinds }
			.filter { filters.locationTypes.isEmpty() || it.locationType in filters.locationTypes }
			.filter { query.isBlank() || it.matchesQuery(query) }
			.sortedWith(compareBy({ it.kind.ordinal }, { !it.isConfigured }, { it.name.lowercase() }))
			.toList()
	}

	private fun List<UnifiedSourcePackageItem>.filterBy(
		filters: UnifiedSourcesFilterState,
		repositoriesById: Map<String, UnifiedSourceRepositoryItem>,
	): List<UnifiedSourcePackageItem> {
		val query = filters.query.trim()
		return asSequence()
			.filter { filters.kinds.isEmpty() || it.kind in filters.kinds }
			.filter { filters.locationTypes.isEmpty() || it.repositoryLocationType(repositoriesById) in filters.locationTypes }
			.filter { filters.languages.isEmpty() || it.language.matchesLanguageFilter(filters.languages) }
			.filter {
				when (filters.nsfwFilter) {
					UnifiedNsfwFilter.ALL -> true
					UnifiedNsfwFilter.SFW -> !it.isNsfw
					UnifiedNsfwFilter.NSFW -> it.isNsfw
				}
			}
			.filter { query.isBlank() || it.matchesQuery(query) }
			.sortedWith(packageItemComparator)
			.toList()
	}

	private fun List<UnifiedSourceItem>.filterBy(
		filters: UnifiedSourcesFilterState,
		repositoriesById: Map<String, UnifiedSourceRepositoryItem>,
		packagesById: Map<String, UnifiedSourcePackageItem>,
	): List<UnifiedSourceItem> {
		val query = filters.query.trim()
		return asSequence()
			.filter { filters.kinds.isEmpty() || it.kind in filters.kinds }
			.filter { filters.contentTypes.isEmpty() || it.contentType in filters.contentTypes }
			.filter { filters.languages.isEmpty() || it.language.matchesLanguageFilter(filters.languages) }
			.filter {
				when (filters.enabledFilter) {
					UnifiedEnabledFilter.ALL -> true
					UnifiedEnabledFilter.ENABLED -> it.isEnabled
					UnifiedEnabledFilter.DISABLED -> !it.isEnabled
				}
			}
			.filter {
				when (filters.availabilityFilter) {
					UnifiedAvailabilityFilter.ALL -> true
					UnifiedAvailabilityFilter.AVAILABLE -> it.isAvailable && !it.isBroken
					UnifiedAvailabilityFilter.UNAVAILABLE -> !it.isAvailable || it.isBroken
				}
			}
			.filter {
				when (filters.testAvailabilityFilter) {
					UnifiedTestAvailabilityFilter.ALL -> true
					UnifiedTestAvailabilityFilter.UNTESTED -> it.testAvailability == ContentSourceAvailability.UNKNOWN
					UnifiedTestAvailabilityFilter.AVAILABLE -> it.testAvailability == ContentSourceAvailability.AVAILABLE
					UnifiedTestAvailabilityFilter.UNAVAILABLE -> it.testAvailability == ContentSourceAvailability.EMPTY
				}
			}
			.filter {
				when (filters.nsfwFilter) {
					UnifiedNsfwFilter.ALL -> true
					UnifiedNsfwFilter.SFW -> !it.isNsfw
					UnifiedNsfwFilter.NSFW -> it.isNsfw
				}
			}
			.filter { filters.locationTypes.isEmpty() || it.repositoryLocationType(repositoriesById, packagesById) in filters.locationTypes }
			.filter { query.isBlank() || it.matchesQuery(query) }
			.sortedWith(compareByDescending<UnifiedSourceItem> { it.isPinned }.thenBy { it.title.lowercase() })
			.toList()
	}
}

sealed interface UnifiedSourcesEvent {
	data class Message(val message: String) : UnifiedSourcesEvent
	data class InstallFailed(val message: String) : UnifiedSourcesEvent
	data class TrustExternalRepository(val repo: ExternalExtensionRepo) : UnifiedSourcesEvent
	data class StartInstall(val intent: Intent) : UnifiedSourcesEvent
	data class StartUninstall(val intent: Intent) : UnifiedSourcesEvent
	data class PackageStateDetails(val item: UnifiedSourcePackageItem) : UnifiedSourcesEvent
}

data class UnifiedSourcesFilterState(
	val query: String = "",
	val kinds: Set<UnifiedSourceKind> = emptySet(),
	val contentTypes: Set<ContentType> = emptySet(),
	val languages: Set<String> = emptySet(),
	val locationTypes: Set<UnifiedRepositoryLocationType> = emptySet(),
	val enabledFilter: UnifiedEnabledFilter = UnifiedEnabledFilter.ALL,
	val availabilityFilter: UnifiedAvailabilityFilter = UnifiedAvailabilityFilter.AVAILABLE,
	val testAvailabilityFilter: UnifiedTestAvailabilityFilter = UnifiedTestAvailabilityFilter.ALL,
	val nsfwFilter: UnifiedNsfwFilter = UnifiedNsfwFilter.ALL,
)

enum class UnifiedEnabledFilter {
	ALL,
	ENABLED,
	DISABLED,
}

enum class UnifiedAvailabilityFilter {
	ALL,
	AVAILABLE,
	UNAVAILABLE,
}

enum class UnifiedTestAvailabilityFilter {
	ALL,
	UNTESTED,
	AVAILABLE,
	UNAVAILABLE,
}

enum class UnifiedNsfwFilter {
	ALL,
	SFW,
	NSFW,
}

sealed interface UnifiedSourcesUiState {
	data object Loading : UnifiedSourcesUiState

	data class Ready(
		val filters: UnifiedSourcesFilterState,
		val repositories: List<UnifiedSourceRepositoryItem>,
		val packages: List<UnifiedSourcePackageItem>,
		val sources: List<UnifiedSourceItem>,
		val allRepositories: List<UnifiedSourceRepositoryItem>,
		val allPackages: List<UnifiedSourcePackageItem>,
		val allSources: List<UnifiedSourceItem>,
		val availableKinds: List<UnifiedSourceKind>,
		val availableContentTypes: List<ContentType>,
		val availableLocationTypes: List<UnifiedRepositoryLocationType>,
		val availableLanguages: List<String>,
	) : UnifiedSourcesUiState
}

internal fun <T> Set<T>.toggle(value: T): Set<T> {
	return if (value in this) this - value else this + value
}

private fun UnifiedSourceRepositoryItem.matchesQuery(query: String): Boolean {
	return name.contains(query, ignoreCase = true) ||
		url.contains(query, ignoreCase = true) ||
		website.contains(query, ignoreCase = true)
}

private fun UnifiedSourcePackageItem.matchesQuery(query: String): Boolean {
	return name.contains(query, ignoreCase = true) ||
		packageName.orEmpty().contains(query, ignoreCase = true) ||
		repositoryName.orEmpty().contains(query, ignoreCase = true) ||
		sourceNames.any { it.contains(query, ignoreCase = true) }
}

private fun UnifiedSourceItem.matchesQuery(query: String): Boolean {
	return title.contains(query, ignoreCase = true) ||
		id.contains(query, ignoreCase = true) ||
		packageName.orEmpty().contains(query, ignoreCase = true) ||
		repositoryName.orEmpty().contains(query, ignoreCase = true)
}

private fun UnifiedSourcePackageItem.repositoryLocationType(
	repositoriesById: Map<String, UnifiedSourceRepositoryItem>,
): UnifiedRepositoryLocationType? {
	return repositoryId?.let(repositoriesById::get)?.locationType
}

private fun UnifiedSourceItem.repositoryLocationType(
	repositoriesById: Map<String, UnifiedSourceRepositoryItem>,
	packagesById: Map<String, UnifiedSourcePackageItem>,
): UnifiedRepositoryLocationType? {
	repositoryId?.let(repositoriesById::get)?.locationType?.let { return it }
	val packageRepositoryId = packageId?.let(packagesById::get)?.repositoryId
	return packageRepositoryId?.let(repositoriesById::get)?.locationType
}

private fun String?.matchesLanguageFilter(languages: Set<String>): Boolean {
	val normalized = this?.normalizeLanguageCode().orEmpty()
	return normalized.isBlank() || normalized in languages
}

private fun Iterable<String>.normalizeLanguageCodes(): LinkedHashSet<String> {
	return mapTo(LinkedHashSet()) { it.normalizeLanguageCode() }
		.filterTo(LinkedHashSet()) { it.isNotBlank() }
}

private fun String.normalizeLanguageCode(): String {
	return normalizeExtensionLanguageCode()
}

private fun UnifiedSourceKind.toExternalExtensionType(): ExternalExtensionType? {
	return when (this) {
		UnifiedSourceKind.MIHON -> ExternalExtensionType.MIHON
		UnifiedSourceKind.JAR -> ExternalExtensionType.JAR
		else -> null
	}
}

private fun ExternalExtensionType.toUnifiedKindForPackage(): UnifiedSourceKind {
	return when (this) {
		ExternalExtensionType.MIHON -> UnifiedSourceKind.MIHON
		ExternalExtensionType.JAR -> UnifiedSourceKind.JAR
	}
}

private fun UnifiedSourceKind.isExternalExtensionKind(): Boolean {
	return when (this) {
		UnifiedSourceKind.JAR,
		UnifiedSourceKind.MIHON -> true
		else -> false
	}
}

private fun UnifiedSourceKind.toLocalApkEcosystem(): String? {
	return when (this) {
		UnifiedSourceKind.MIHON -> "mihon"
		else -> null
	}
}

private fun UnifiedSourceKind.isJsonBackedKind(): Boolean {
	return when (this) {
		UnifiedSourceKind.LEGADO,
		UnifiedSourceKind.JS -> true
		else -> false
	}
}

private fun JsonSourceEntity.jsonPackageIdForAction(): String? {
	return when (type) {
		JsonSourceType.LEGADO -> {
			val repositoryId = jsonRepositoryIdForAction()
			packageIdForAction(UnifiedSourceKind.LEGADO, repositoryId ?: "imported")
		}
		JsonSourceType.JS -> packageIdForAction(UnifiedSourceKind.JS, id)
	}
}

private fun JsonSourceEntity.jsonRepositoryIdForAction(): String? {
	return when (type) {
		JsonSourceType.LEGADO -> {
			val locator = JsonSourceImportMetadata.parse(config)
				?.sourceLocator
				?.trim()
				?.takeIf { it.isNotBlank() }
				?: return null
			repositoryIdForAction(UnifiedSourceKind.LEGADO, locator)
		}
		JsonSourceType.JS -> null
	}
}

private fun UnifiedSourceKind.isHotReloadableExternalKind(): Boolean {
	return toLocalApkEcosystem() != null
}

private val UnifiedSourcePackageState.sortOrder: Int
	get() = when (this) {
		UnifiedSourcePackageState.UPDATE_AVAILABLE -> 0
		UnifiedSourcePackageState.UNTRUSTED -> 1
		UnifiedSourcePackageState.INCOMPATIBLE -> 2
		UnifiedSourcePackageState.INSTALLING -> 3
		UnifiedSourcePackageState.INSTALLED -> 4
		UnifiedSourcePackageState.AVAILABLE -> 5
	}

private val packageItemComparator = compareByDescending<UnifiedSourcePackageItem> { it.isInstalled }
	.thenBy { it.state.sortOrder }
	.thenBy { it.kind.ordinal }
	.thenBy { it.name.lowercase() }

private fun externalExtensionTypes(): List<ExternalExtensionType> {
	return listOf(
		ExternalExtensionType.JAR,
		ExternalExtensionType.MIHON,
	)
}

private fun repositoryIdForAction(kind: UnifiedSourceKind, url: String): String {
	return "repo:${kind.name}:${normalizeRepositoryUrlForAction(url)}"
}

private fun packageIdForAction(kind: UnifiedSourceKind, value: String): String {
	return "package:${kind.name}:${value.trim()}"
}

private fun UnifiedSourceKind.displayNameForMessage(context: Context): String {
	return when (this) {
		UnifiedSourceKind.NATIVE -> context.getString(R.string.source_type_native)
		UnifiedSourceKind.JAR -> context.getString(R.string.source_type_jar)
		UnifiedSourceKind.MIHON -> context.getString(R.string.source_type_mihon)
		UnifiedSourceKind.LEGADO -> context.getString(R.string.source_type_legado)
		UnifiedSourceKind.JS -> context.getString(R.string.source_type_js)
	}
}

private fun normalizeRepositoryUrlForAction(url: String): String {
	val trimmed = url.trim()
	val lower = trimmed.lowercase()
	if (
		lower.endsWith(".json") &&
		!lower.endsWith("/index.min.json") &&
		!lower.endsWith("/plugins.json") &&
		!lower.endsWith("/repo.json")
	) {
		return trimmed.trimEnd('/')
	}
	return trimmed
		.trimEnd('/')
		.removeSuffix("/index.pb")
		.removeSuffix("/index.min.json")
		.removeSuffix("/plugins.json")
		.removeSuffix("/repo.json")
		.removeSuffix("/repo")
		.trimEnd('/')
}

private fun resolveRepositoryLocationTypeForAction(locator: String): UnifiedRepositoryLocationType {
	return when {
		locator.startsWith("content://", ignoreCase = true) -> UnifiedRepositoryLocationType.LOCAL_FILE
		locator.startsWith("file://", ignoreCase = true) -> UnifiedRepositoryLocationType.LOCAL_FILE
		locator.startsWith("http://", ignoreCase = true) -> UnifiedRepositoryLocationType.REMOTE_URL
		locator.startsWith("https://", ignoreCase = true) -> UnifiedRepositoryLocationType.REMOTE_URL
		else -> UnifiedRepositoryLocationType.INLINE_IMPORT
	}
}
