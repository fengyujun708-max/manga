package com.mangaverse.app.settings

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.mangaverse.app.R
import com.mangaverse.app.core.model.ContentSource
import com.mangaverse.app.core.model.getTitle
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.nav.applyHorizontalRouteCloseTransition
import com.mangaverse.app.core.nav.router
import com.mangaverse.app.core.network.BaseHttpClient
import com.mangaverse.app.core.os.AppShortcutManager
import com.mangaverse.app.core.os.OpenDocumentTreeHelper
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.ui.BaseComposeActivity
import com.mangaverse.app.core.ui.compose.DynamicArtworkBackdrop
import com.mangaverse.app.core.ui.theme.KototoroTheme
import com.mangaverse.app.history.data.HistoryRepository
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.core.ui.util.ActivityRecreationHandle
import com.mangaverse.app.core.ui.util.ReversibleActionObserver
import com.mangaverse.app.core.util.FileSize
import com.mangaverse.app.core.util.FoldableUtils
import com.mangaverse.app.core.util.ext.buildBundle
import com.mangaverse.app.core.util.ext.end
import com.mangaverse.app.core.util.ext.getDisplayMessage
import com.mangaverse.app.core.util.ext.getQuantityStringSafe
import com.mangaverse.app.core.util.ext.observeEvent
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.core.util.ext.start
import com.mangaverse.app.core.util.ext.textAndVisible
import com.mangaverse.app.core.util.ext.tryLaunch
import com.mangaverse.app.download.ui.worker.DownloadWorker
import com.mangaverse.app.explore.data.SourcePresetsRepository
import com.mangaverse.app.favourites.ui.migration.compose.EntityOrganizePageIntroCard
import com.mangaverse.app.favourites.ui.migration.compose.SourceMigrationPanel
import com.mangaverse.app.local.data.LocalStorageManager
import com.mangaverse.app.main.ui.compose.encodeEntityOrganizeSelection
import com.mangaverse.app.main.ui.compose.parseEntityOrganizeSelection
import com.mangaverse.app.parsers.util.await
import com.mangaverse.app.reader.translate.data.OnnxModelManager
import com.mangaverse.app.settings.about.AboutSettingsRoute
import com.mangaverse.app.settings.about.AboutSettingsViewModel
import com.mangaverse.app.settings.compose.SettingsAdaptiveShell
import com.mangaverse.app.settings.compose.AppearanceSettingsPage
import com.mangaverse.app.settings.compose.SettingsAlertDialog
import com.mangaverse.app.settings.compose.SettingsChoiceOption
import com.mangaverse.app.settings.compose.SettingsDialogActionButton
import com.mangaverse.app.settings.compose.SettingsRootScreen
import com.mangaverse.app.settings.compose.SettingsSectionScaffold
import com.mangaverse.app.settings.compose.SettingsSearchTopBarAction
import com.mangaverse.app.settings.compose.SettingsSearchTopAppBar
import com.mangaverse.app.settings.compose.SettingsTopBarScaffold
import com.mangaverse.app.settings.compose.SpacesSettingsRoute
import com.mangaverse.app.settings.compose.buildSettingsRootSections
import com.mangaverse.app.settings.nav.NavConfigRoute
import com.mangaverse.app.settings.nav.NavConfigViewModel
import com.mangaverse.app.settings.userdata.storage.DataCleanupSettingsRoute
import com.mangaverse.app.settings.userdata.storage.DataCleanupSettingsViewModel
import com.mangaverse.app.settings.protect.ProtectSetupActivity
import com.mangaverse.app.settings.search.SettingsItem
import com.mangaverse.app.settings.search.SettingsSearchMenuProvider
import com.mangaverse.app.settings.search.SettingsSearchViewModel
import com.mangaverse.app.settings.support.TranslationApiSettingsSupport
import com.mangaverse.app.reader.translate.domain.TranslationApiProviderCatalog
import com.mangaverse.app.core.exceptions.resolve.SnackbarErrorObserver
import com.mangaverse.app.settings.sources.SourceSettingsRoute
import com.mangaverse.app.settings.sources.SourcesSettingsRoute
import com.mangaverse.app.settings.sources.SourcesSettingsViewModel
import com.mangaverse.app.settings.sources.unified.UnifiedSourceKind
import com.mangaverse.app.settings.sources.unified.UnifiedSourcesToolbarActions
import com.mangaverse.app.settings.sources.unified.UnifiedToolbarFilterPanel
import com.mangaverse.app.settings.sources.unified.UnifiedSourcesRoute
import com.mangaverse.app.settings.sources.unified.UnifiedSourcesViewModel
import com.mangaverse.app.settings.tracker.TrackerSettingsRoute
import com.mangaverse.app.settings.tracker.TrackerSettingsViewModel
import com.mangaverse.app.settings.userdata.BackupsSettingsRoute
import com.mangaverse.app.settings.utils.RingtonePickContract
import com.mangaverse.app.tracker.ui.debug.TrackerDebugActivity
import com.mangaverse.app.tracker.work.TrackerNotificationHelper
import com.mangaverse.app.core.parser.EmptyContentRepository
import com.mangaverse.app.core.parser.ContentRepository
import com.mangaverse.app.core.parser.JsContentRepository
import com.mangaverse.app.core.parser.ParserContentRepository
import com.mangaverse.app.core.parser.kotatsu.KotatsuParserRepository
import com.mangaverse.app.core.parser.legado.LegadoRepository
import com.mangaverse.app.parsers.model.ContentSource
import kotlin.coroutines.cancellation.CancellationException

@AndroidEntryPoint
class SettingsActivity :
	BaseComposeActivity() {

	private val initialEntityOrganizeSelection: Set<Long> by lazy(LazyThreadSafetyMode.NONE) {
		parseEntityOrganizeSelection(intent?.getStringExtra(EXTRA_ENTITY_ORGANIZE_SELECTION).orEmpty())
	}

	@Inject
	lateinit var activityRecreationHandle: ActivityRecreationHandle

	@Inject
	lateinit var appShortcutManager: AppShortcutManager

	@Inject
	lateinit var sourcePresetsRepository: SourcePresetsRepository

	@Inject
	lateinit var storageManager: LocalStorageManager

	@Inject
	lateinit var historyRepository: HistoryRepository

	@Inject
	lateinit var downloadsScheduler: DownloadWorker.Scheduler

	@Inject
	lateinit var mangaRepositoryFactory: ContentRepository.Factory

	@Inject
	lateinit var trackerNotificationHelper: TrackerNotificationHelper

	@Inject
	@BaseHttpClient
	lateinit var okHttpClient: OkHttpClient

	@Inject
	lateinit var suggestionsScheduler: SuggestionsWorker.Scheduler

	@Inject
	lateinit var contentSourcesRepository: ContentSourcesRepository

	@Inject
	lateinit var onnxModelManager: OnnxModelManager

	private val isMasterDetails
		get() = FoldableUtils.shouldUseTabletLayout(this, kototoroAppSettings) && if (kototoroAppSettings.tabletUiMode == com.mangaverse.app.core.prefs.TabletUiMode.STRICT) {
			resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
		} else {
			true
		}

	private val viewModel: SettingsSearchViewModel by viewModels()
	private val rootSettingsViewModel: RootSettingsViewModel by viewModels()
	private val aboutSettingsViewModel: AboutSettingsViewModel by viewModels()
	private val periodicalBackupSettingsViewModel: PeriodicalBackupSettingsViewModel by viewModels()
	private val sourcesSettingsViewModel: SourcesSettingsViewModel by viewModels()
	private val unifiedSourcesViewModel: UnifiedSourcesViewModel by viewModels()
	private val storageAndNetworkSettingsViewModel: StorageAndNetworkSettingsViewModel by viewModels()
	private val dataCleanupSettingsViewModel: DataCleanupSettingsViewModel by viewModels()
	private val navConfigViewModel: NavConfigViewModel by viewModels()
	private val trackerSettingsViewModel: TrackerSettingsViewModel by viewModels()

	private var isFoldUnfolded = false
	private var composeDestination: SettingsDestination? by mutableStateOf(null)
	private var pendingSettingsDialog by mutableStateOf<SettingsDialogState?>(null)
	private sealed interface SettingsDialogState {
		data class Confirmation(
			val titleRes: Int,
			val messageRes: Int,
			val confirmRes: Int,
			val onConfirm: () -> Unit,
		) : SettingsDialogState

		data class ModelPicker(
			val titleRes: Int,
			val models: List<String>,
			val selectedIndex: Int? = null,
			val onSelected: (String) -> Unit,
		) : SettingsDialogState

		data class ProxyTestResult(val message: String) : SettingsDialogState
	}
	private val composeNavigationStack = ArrayDeque<SettingsDestination>()
	private var isDataCleanupObserversBound = false
	private var translationApiFetchModelsJob: Job? = null
	private var translationE2EApiFetchModelsJob: Job? = null
	private var proxyTestJob: Job? = null
	private val downloadsStorageTick = MutableStateFlow(0)
	private val downloadsDozeTick = MutableStateFlow(0)
	private val trackerDozeTick = MutableStateFlow(0)
	private val trackerNotificationTick = MutableStateFlow(0)
	private val proxyTestSummaryFlow = MutableStateFlow<String?>(null)
	private val proxyIsTestRunningFlow = MutableStateFlow(false)
	private val suggestionsExcludeTagsFlow = MutableStateFlow("")
	private val suggestionsPreferredTagsFlow = MutableStateFlow("")
	private var hasAppliedCloseRouteTransition = false
	private var pendingExternalBackupApp: ExternalBackupApp? = null
	private var pendingUnifiedSourcesFileImportKind: UnifiedSourceKind? = null
	private var unifiedSourcesSearchActive by mutableStateOf(false)
	private var unifiedSourcesActivePanel by mutableStateOf<UnifiedToolbarFilterPanel?>(null)
	private var isLegacyTopBarVisible = false
	private val contentRoot: View
		get() = window.decorView

	private val composeBackCallback = object : OnBackPressedCallback(false) {
		override fun handleOnBackPressed() {
			handleComposeNavigateUp()
		}
	}

	private val pickDownloadsPagesDirectory = OpenDocumentTreeHelper(this) { uri ->
		if (uri == null) return@OpenDocumentTreeHelper
		onDownloadsPagesDirectoryPicked(uri)
	}

	private val ignoreDownloadsDozeLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult(),
	) {
		downloadsDozeTick.update { it + 1 }
	}

	private val ignoreTrackerDozeLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult(),
	) {
		trackerDozeTick.update { it + 1 }
	}

	private val backupSelectCall = registerForActivityResult(
		ActivityResultContracts.OpenDocument(),
	) { uri ->
		if (uri != null) {
			router.showBackupRestoreDialog(uri)
		}
	}

	private val legacyBackupSelectCall = registerForActivityResult(
		ActivityResultContracts.OpenDocument(),
	) { uri ->
		if (uri != null) {
			router.showBackupRestoreDialog(uri, BackupRestoreFormat.KOTATSU_OR_LEGACY_KOTOTORO)
		}
	}

	private val externalBackupSelectCall = registerForActivityResult(
		ActivityResultContracts.OpenDocument(),
	) { uri ->
		if (uri != null) {
			val app = pendingExternalBackupApp ?: return@registerForActivityResult
			if (ExternalBackupImportService.start(this, uri, app)) {
				Toast.makeText(this, R.string.import_backup_started_background, Toast.LENGTH_SHORT).show()
			} else {
				Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
			}
		}
		pendingExternalBackupApp = null
	}

	private val backupCreateCall = registerForActivityResult(
		ActivityResultContracts.CreateDocument("application/zip"),
	) { uri ->
		if (uri != null && !BackupService.start(this, uri)) {
			Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
		}
	}

	private val mihonBackupExportCall = registerForActivityResult(
		ActivityResultContracts.CreateDocument("application/octet-stream"),
	) { uri ->
		if (uri != null && !MihonBackupExportService.start(this, uri)) {
			Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
		}
	}

	private val kotatsuBackupExportCall = registerForActivityResult(
		ActivityResultContracts.CreateDocument("application/zip"),
	) { uri ->
		if (uri != null && !BackupService.startKotatsuExport(this, uri)) {
			Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
		}
	}

	private val usagiBackupExportCall = registerForActivityResult(
		ActivityResultContracts.CreateDocument("application/zip"),
	) { uri ->
		if (uri != null && !UsagiBackupExportService.start(this, uri)) {
			Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
		}
	}

	private val backupOutputSelectCall = OpenDocumentTreeHelper(this) { uri ->
		if (uri != null) {
			val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			contentResolver.takePersistableUriPermission(uri, takeFlags)
			kototoroAppSettings.periodicalBackupDirectory = uri
			periodicalBackupSettingsViewModel.updateSummaryData()
		}
	}

	private val ringtonePickContract = registerForActivityResult(
		RingtonePickContract(R.string.notification_sound),
	) { uri ->
		kototoroAppSettings.notificationSound = uri ?: return@registerForActivityResult
	}

	private val openUnifiedSourcesRepositoryFile = registerForActivityResult(
		ActivityResultContracts.OpenDocument(),
	) { uri ->
		if (uri == null) return@registerForActivityResult
		val kind = pendingUnifiedSourcesFileImportKind ?: return@registerForActivityResult
		pendingUnifiedSourcesFileImportKind = null
		persistReadPermission(uri)
		unifiedSourcesViewModel.addRepositoryFromFile(kind, uri)
	}

	private val openUnifiedSourcesLocalJar = registerForActivityResult(
		ActivityResultContracts.OpenDocument(),
	) { uri ->
		if (uri == null) return@registerForActivityResult
		persistReadPermission(uri)
		unifiedSourcesViewModel.importLocalJar(uri)
	}

	private val unifiedSourcesInstallLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult(),
	) {
		unifiedSourcesViewModel.onInstallActivityResult()
	}

	private val unifiedSourcesUninstallLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult(),
	) {
		unifiedSourcesViewModel.onUninstallActivityResult()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setLegacyTopBarVisible(false)
		addMenuProvider(
			SettingsSearchMenuProvider(viewModel) {
				isLegacyTopBarVisible
			},
		)
		onBackPressedDispatcher.addCallback(this, composeBackCallback)
		val restoredDestination = savedInstanceState?.toComposeDestination()
		val initialComposeDestination = restoredDestination ?: resolveDefaultComposeDestination(intent)
		composeDestination = initialComposeDestination
		setComposeContent {
			val lastReadContent by historyRepository.observeLast().collectAsStateWithLifecycle(initialValue = null)
			DynamicArtworkBackdrop(content = lastReadContent) {
				SettingsAdaptiveShell(
					isTwoPane = isMasterDetails,
					destination = composeDestination,
					destinationKey = ::composeDestinationStateKey,
					modifier = Modifier.fillMaxSize(),
					rootContent = { modifier -> RenderSettingsRootContent(modifier) },
					destinationContent = { destination -> RenderComposeDestination(destination) },
				)
				RenderSettingsDialog()
			}
		}
		if (initialComposeDestination != null) {
			openComposeDestination(initialComposeDestination, shouldRestoreFragment = false)
		} else {
			openDefaultDestination()
		}
		viewModel.onNavigateToPreference.observeEvent(this, ::navigateToPreference)

		observeFoldableState()
	}

	override fun onResume() {
		super.onResume()
		when (composeDestination) {
			SettingsDestination.DownloadsSettings -> {
				downloadsStorageTick.update { it + 1 }
				downloadsDozeTick.update { it + 1 }
			}
			SettingsDestination.SuggestionsSettings -> {
				refreshSuggestionsTags()
			}
			SettingsDestination.TrackerSettings -> {
				trackerDozeTick.update { it + 1 }
				trackerNotificationTick.update { it + 1 }
			}
			else -> Unit
		}
		// 从后台恢复或状态变化后，立即按当前折叠状态调整布局
		adjustLayoutForFoldableState()
	}

	override fun onDestroy() {
		translationApiFetchModelsJob?.cancel()
		translationE2EApiFetchModelsJob?.cancel()
		super.onDestroy()
	}

	override fun finish() {
		super.finish()
		applyCloseRouteTransitionIfNeeded()
	}

	override fun finishAfterTransition() {
		super.finishAfterTransition()
		applyCloseRouteTransitionIfNeeded()
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
			when (val destination = composeDestination) {
				SettingsDestination.Root -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_ROOT)
				}
				SettingsDestination.AppearanceSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_APPEARANCE_SETTINGS)
				}
				SettingsDestination.AppearanceBadgesSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_APPEARANCE_BADGES_SETTINGS)
				}
				SettingsDestination.AppearanceSearchFiltersSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_APPEARANCE_SEARCH_FILTERS_SETTINGS)
				}
				SettingsDestination.AppearanceNavigationSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_APPEARANCE_NAVIGATION_SETTINGS)
				}
				SettingsDestination.PanoramaSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_PANORAMA_SETTINGS)
				}
				SettingsDestination.SpacesSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_SPACES_SETTINGS)
				}
				SettingsDestination.AISettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_AI_SETTINGS)
				}
				SettingsDestination.OcrModelsSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_OCR_MODELS_SETTINGS)
				}
				SettingsDestination.AiImageEnhancementSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_AI_IMAGE_ENHANCEMENT_SETTINGS)
				}
				SettingsDestination.AiVideoEnhancementSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_AI_VIDEO_ENHANCEMENT_SETTINGS)
				}
				SettingsDestination.PlaybackSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_PLAYBACK_SETTINGS)
				}
				SettingsDestination.ReaderSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_READER_SETTINGS)
				}
				SettingsDestination.SourcesSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_SOURCES_SETTINGS)
				}
			SettingsDestination.SuggestionsSettings -> {
				outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_SUGGESTIONS_SETTINGS)
			}
			SettingsDestination.BackupsSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_BACKUPS_SETTINGS)
				}
				SettingsDestination.EntityOrganizeSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_ENTITY_ORGANIZE_SETTINGS)
				}
				SettingsDestination.TranslationSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_TRANSLATION_SETTINGS)
				}
				SettingsDestination.TranslationApiSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_TRANSLATION_API_SETTINGS)
				}
				SettingsDestination.TranslationE2EApiSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_TRANSLATION_E2E_API_SETTINGS)
				}
				SettingsDestination.StorageAndNetworkSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_STORAGE_AND_NETWORK_SETTINGS)
				}
				SettingsDestination.CacheLimitsSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_CACHE_LIMITS_SETTINGS)
				}
				SettingsDestination.DataCleanupSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_DATA_CLEANUP_SETTINGS)
				}
				SettingsDestination.DownloadsSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_DOWNLOADS_SETTINGS)
				}
				SettingsDestination.TrackerSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_TRACKER_SETTINGS)
				}
				SettingsDestination.NotificationSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_NOTIFICATION_SETTINGS)
				}
				SettingsDestination.ServicesSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_SERVICES_SETTINGS)
				}
				SettingsDestination.ProxySettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_PROXY_SETTINGS)
				}
				SettingsDestination.NavConfigSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_NAV_CONFIG_SETTINGS)
				}
				SettingsDestination.AboutSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_ABOUT_SETTINGS)
				}
				is SettingsDestination.SourceSettings -> {
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_SOURCE_SETTINGS)
					outState.putString(STATE_SOURCE_SETTINGS_SOURCE, destination.sourceName)
				}
				is SettingsDestination.UnifiedSources -> {
					val unifiedDestination = composeDestination as? SettingsDestination.UnifiedSources ?: return
					outState.putString(STATE_COMPOSE_DESTINATION, COMPOSE_DESTINATION_UNIFIED_SOURCES)
					outState.putString(
						STATE_UNIFIED_SOURCES_KIND,
						unifiedDestination.initialRepositoryKind?.name,
					)
					outState.putString(
						STATE_UNIFIED_SOURCES_URL,
						unifiedDestination.initialRepositoryUrl,
					)
				}
				null -> Unit
			}
	}

	fun setSectionTitle(title: CharSequence?) {
		setTitle(title ?: getString(R.string.settings))
	}

	private fun updateUnifiedSourcesSearchActive(active: Boolean) {
		unifiedSourcesSearchActive = active
	}

	private fun setLegacyTopBarVisible(isVisible: Boolean) {
		isLegacyTopBarVisible = false
		invalidateOptionsMenu()
	}

	private fun renderComposeContent(
		showLegacyTopBar: Boolean,
		destination: SettingsDestination,
	) {
		setLegacyTopBarVisible(showLegacyTopBar)
	}

	@Composable
	private fun RenderComposeSection(
		title: String,
		actions: (@Composable BoxScope.() -> Unit)? = null,
		content: @Composable () -> Unit,
	) {
		SettingsSectionScaffold(
			title = title,
			onNavigateUp = if (isMasterDetails) null else ::handleComposeNavigateUp,
			showTopBar = true,
			actions = actions,
			content = content,
		)
	}

	fun openDestination(destination: SettingsDestination, args: Bundle?, isFromRoot: Boolean) {
		if (isFromRoot) {
			composeNavigationStack.clear()
			if (destination != SettingsDestination.Root) {
				composeNavigationStack.addLast(SettingsDestination.Root)
			}
		}
		when (destination) {
			SettingsDestination.Root -> openComposeDestination(
				destination,
				shouldRestoreFragment = false,
				pushCurrentToStack = false,
			)
			SettingsDestination.AppearanceSettings,
			SettingsDestination.AppearanceBadgesSettings,
			SettingsDestination.AppearanceSearchFiltersSettings,
			SettingsDestination.AppearanceNavigationSettings,
			SettingsDestination.PanoramaSettings,
			SettingsDestination.SpacesSettings,
			SettingsDestination.AISettings,
			SettingsDestination.OcrModelsSettings,
			SettingsDestination.AiImageEnhancementSettings,
			SettingsDestination.AiVideoEnhancementSettings,
			SettingsDestination.PlaybackSettings,
			SettingsDestination.ReaderSettings,
			SettingsDestination.SourcesSettings,
			SettingsDestination.SuggestionsSettings,
			SettingsDestination.BackupsSettings,
			SettingsDestination.EntityOrganizeSettings,
			SettingsDestination.TranslationSettings,
			SettingsDestination.TranslationApiSettings,
			SettingsDestination.TranslationE2EApiSettings,
			SettingsDestination.StorageAndNetworkSettings,
			SettingsDestination.CacheLimitsSettings,
			SettingsDestination.DataCleanupSettings,
			SettingsDestination.DownloadsSettings,
			SettingsDestination.TrackerSettings,
			SettingsDestination.NotificationSettings,
			SettingsDestination.ServicesSettings,
			SettingsDestination.ProxySettings,
			SettingsDestination.NavConfigSettings,
			SettingsDestination.AboutSettings,
			is SettingsDestination.SourceSettings,
			is SettingsDestination.UnifiedSources -> openComposeDestination(
				destination,
				shouldRestoreFragment = false,
				pushCurrentToStack = !isFromRoot,
			)
		}
	}

	private fun openDefaultDestination() {
		resolveDefaultComposeDestination(intent)?.let { destination ->
			openComposeDestination(destination, shouldRestoreFragment = false)
			return
		}
		openComposeDestination(SettingsDestination.Root, shouldRestoreFragment = false)
	}

	private fun resolveDefaultComposeDestination(intent: Intent?): SettingsDestination? {
		return resolveInitialUnifiedSourcesDestination(intent) ?: when (intent?.action) {
			// 小说/视频/追踪/建议等已移除功能的深链统一回根设置
			AppRouter.ACTION_SUGGESTIONS -> SettingsDestination.Root
			AppRouter.ACTION_PERIODIC_BACKUP,
			AppRouter.ACTION_HISTORY -> SettingsDestination.BackupsSettings
			AppRouter.ACTION_TRANSLATION -> SettingsDestination.Root
			AppRouter.ACTION_TRACKER -> SettingsDestination.Root
			AppRouter.ACTION_PROXY -> SettingsDestination.ProxySettings
			AppRouter.ACTION_READER -> SettingsDestination.ReaderSettings
			AppRouter.ACTION_SOURCES -> SettingsDestination.SourcesSettings
			AppRouter.ACTION_ENTITY_ORGANIZE -> SettingsDestination.Root
			AppRouter.ACTION_MANAGE_DOWNLOADS -> SettingsDestination.DownloadsSettings
			AppRouter.ACTION_MANAGE_SOURCES -> null
			AppRouter.ACTION_SOURCE -> intent.getStringExtra(AppRouter.KEY_SOURCE)
				?.takeIf { it.isNotBlank() }
				?.let(SettingsDestination::SourceSettings)
			Intent.ACTION_VIEW -> when (intent.data?.host) {
				HOST_ADD_REPO -> null
				HOST_ABOUT -> SettingsDestination.AboutSettings
				else -> SettingsDestination.Root
			}
			else -> SettingsDestination.Root
		}
	}

	private fun navigateToPreference(item: SettingsItem) {
		val args = buildBundle(1) {
			putString(ARG_PREF_KEY, item.key)
		}
		openDestination(item.destination, args, true)
	}

	private fun shouldKeepComposeHistory(): Boolean = true

	private fun openComposeDestination(
		destination: SettingsDestination,
		shouldRestoreFragment: Boolean,
		pushCurrentToStack: Boolean = true,
	) {
		viewModel.discardSearch()
		if (destination !is SettingsDestination.UnifiedSources) {
			updateUnifiedSourcesSearchActive(false)
			unifiedSourcesActivePanel = null
		}
		val currentComposeDestination = composeDestination
		if (destination == SettingsDestination.Root) {
			composeNavigationStack.clear()
		} else if (
			pushCurrentToStack &&
			shouldKeepComposeHistory() &&
			currentComposeDestination != null &&
			currentComposeDestination != destination
		) {
			composeNavigationStack.addLast(currentComposeDestination)
		} else if (!shouldKeepComposeHistory()) {
			composeNavigationStack.clear()
		}
		composeDestination = destination
		prepareComposeDestination(destination)
		renderComposeContent(showLegacyTopBar = false, destination = destination)
		composeBackCallback.isEnabled = true
	}

	private fun prepareComposeDestination(destination: SettingsDestination) {
		when (destination) {
			is SettingsDestination.UnifiedSources -> {
				updateUnifiedSourcesSearchActive(false)
				unifiedSourcesActivePanel = null
			}
			SettingsDestination.SuggestionsSettings -> {
				refreshSuggestionsTags()
			}
			SettingsDestination.BackupsSettings -> {
				periodicalBackupSettingsViewModel.updateSummaryData()
			}
			SettingsDestination.DataCleanupSettings -> {
				bindDataCleanupObservers()
			}
			SettingsDestination.DownloadsSettings -> {
				downloadsStorageTick.update { it + 1 }
				downloadsDozeTick.update { it + 1 }
			}
			is SettingsDestination.SourceSettings -> {
				intent.putExtra(AppRouter.KEY_SOURCE, destination.sourceName)
			}
			SettingsDestination.TrackerSettings -> {
				trackerDozeTick.update { it + 1 }
				trackerNotificationTick.update { it + 1 }
			}
			else -> Unit
		}
	}

	private fun composeDestinationStateKey(destination: SettingsDestination): String {
		return when (destination) {
			SettingsDestination.Root -> COMPOSE_DESTINATION_ROOT
			SettingsDestination.AppearanceSettings -> COMPOSE_DESTINATION_APPEARANCE_SETTINGS
			SettingsDestination.AppearanceBadgesSettings -> COMPOSE_DESTINATION_APPEARANCE_BADGES_SETTINGS
			SettingsDestination.AppearanceSearchFiltersSettings -> COMPOSE_DESTINATION_APPEARANCE_SEARCH_FILTERS_SETTINGS
			SettingsDestination.AppearanceNavigationSettings -> COMPOSE_DESTINATION_APPEARANCE_NAVIGATION_SETTINGS
			SettingsDestination.PanoramaSettings -> COMPOSE_DESTINATION_PANORAMA_SETTINGS
			SettingsDestination.SpacesSettings -> COMPOSE_DESTINATION_SPACES_SETTINGS
			SettingsDestination.AISettings -> COMPOSE_DESTINATION_AI_SETTINGS
			SettingsDestination.OcrModelsSettings -> COMPOSE_DESTINATION_OCR_MODELS_SETTINGS
			SettingsDestination.AiImageEnhancementSettings -> COMPOSE_DESTINATION_AI_IMAGE_ENHANCEMENT_SETTINGS
			SettingsDestination.AiVideoEnhancementSettings -> COMPOSE_DESTINATION_AI_VIDEO_ENHANCEMENT_SETTINGS
			SettingsDestination.PlaybackSettings -> COMPOSE_DESTINATION_PLAYBACK_SETTINGS
			SettingsDestination.ReaderSettings -> COMPOSE_DESTINATION_READER_SETTINGS
			SettingsDestination.SourcesSettings -> COMPOSE_DESTINATION_SOURCES_SETTINGS
			SettingsDestination.SuggestionsSettings -> COMPOSE_DESTINATION_SUGGESTIONS_SETTINGS
			SettingsDestination.BackupsSettings -> COMPOSE_DESTINATION_BACKUPS_SETTINGS
			SettingsDestination.EntityOrganizeSettings -> COMPOSE_DESTINATION_ENTITY_ORGANIZE_SETTINGS
			SettingsDestination.TranslationSettings -> COMPOSE_DESTINATION_TRANSLATION_SETTINGS
			SettingsDestination.TranslationApiSettings -> COMPOSE_DESTINATION_TRANSLATION_API_SETTINGS
			SettingsDestination.TranslationE2EApiSettings -> COMPOSE_DESTINATION_TRANSLATION_E2E_API_SETTINGS
			SettingsDestination.StorageAndNetworkSettings -> COMPOSE_DESTINATION_STORAGE_AND_NETWORK_SETTINGS
			SettingsDestination.CacheLimitsSettings -> COMPOSE_DESTINATION_CACHE_LIMITS_SETTINGS
			SettingsDestination.DataCleanupSettings -> COMPOSE_DESTINATION_DATA_CLEANUP_SETTINGS
			SettingsDestination.DownloadsSettings -> COMPOSE_DESTINATION_DOWNLOADS_SETTINGS
			SettingsDestination.TrackerSettings -> COMPOSE_DESTINATION_TRACKER_SETTINGS
			SettingsDestination.NotificationSettings -> COMPOSE_DESTINATION_NOTIFICATION_SETTINGS
			SettingsDestination.ServicesSettings -> COMPOSE_DESTINATION_SERVICES_SETTINGS
			SettingsDestination.ProxySettings -> COMPOSE_DESTINATION_PROXY_SETTINGS
			SettingsDestination.NavConfigSettings -> COMPOSE_DESTINATION_NAV_CONFIG_SETTINGS
			SettingsDestination.AboutSettings -> COMPOSE_DESTINATION_ABOUT_SETTINGS
			is SettingsDestination.SourceSettings -> "source:${destination.sourceName}"
			is SettingsDestination.UnifiedSources -> "unified:${destination.initialRepositoryKind}:${destination.initialRepositoryUrl}"
		}
	}

	private fun composeDestinationTitle(destination: SettingsDestination): String {
		return when (destination) {
			SettingsDestination.Root -> getString(R.string.settings)
			SettingsDestination.AppearanceSettings -> getString(R.string.appearance)
			SettingsDestination.AppearanceBadgesSettings -> getString(R.string.badges_in_lists)
			SettingsDestination.AppearanceSearchFiltersSettings -> getString(R.string.search_bar_filters)
			SettingsDestination.AppearanceNavigationSettings -> getString(R.string.appearance_navigation_group)
			SettingsDestination.PanoramaSettings -> getString(R.string.panorama_settings_title)
			SettingsDestination.SpacesSettings -> getString(R.string.spaces)
			SettingsDestination.AISettings -> getString(R.string.ai_settings)
			SettingsDestination.OcrModelsSettings -> getString(R.string.reader_translation_ocr_models_title)
			SettingsDestination.AiImageEnhancementSettings -> getString(R.string.ai_image_enhancement_settings)
			SettingsDestination.AiVideoEnhancementSettings -> getString(R.string.ai_video_enhancement_settings)
			SettingsDestination.PlaybackSettings -> getString(R.string.playback_settings)
			SettingsDestination.ReaderSettings -> getString(R.string.reader_settings)
			SettingsDestination.SourcesSettings -> getString(R.string.remote_sources)
			SettingsDestination.SuggestionsSettings -> getString(R.string.suggestions)
			SettingsDestination.BackupsSettings -> getString(R.string.backup_restore)
			SettingsDestination.EntityOrganizeSettings -> getString(R.string.entity_organize_title)
			SettingsDestination.TranslationSettings -> getString(R.string.translation_settings)
			SettingsDestination.TranslationApiSettings -> getString(R.string.ai_api_settings)
			SettingsDestination.TranslationE2EApiSettings -> getString(R.string.reader_translation_e2e_api_settings_title)
			SettingsDestination.StorageAndNetworkSettings -> getString(R.string.storage_and_network)
			SettingsDestination.CacheLimitsSettings -> getString(R.string.cache_limits)
			SettingsDestination.DataCleanupSettings -> getString(R.string.data_removal)
			SettingsDestination.DownloadsSettings -> getString(R.string.downloads)
			SettingsDestination.TrackerSettings -> getString(R.string.check_for_new_chapters)
			SettingsDestination.NotificationSettings -> getString(R.string.notifications)
			SettingsDestination.ServicesSettings -> getString(R.string.services)
			SettingsDestination.ProxySettings -> getString(R.string.proxy)
			SettingsDestination.NavConfigSettings -> getString(R.string.main_screen_sections)
			SettingsDestination.AboutSettings -> getString(R.string.about)
			is SettingsDestination.SourceSettings -> com.mangaverse.app.core.model.ContentSource(
				destination.sourceName,
			).getTitle(this)
			is SettingsDestination.UnifiedSources -> getString(R.string.extension_management)
		}
	}

	@Composable
	private fun RenderAppearanceSettings(page: AppearanceSettingsPage) {
		AppearanceSettingsRoute(
			page = page,
			settings = kototoroAppSettings,
			activityRecreationHandle = activityRecreationHandle,
			appShortcutManager = appShortcutManager,
			sourcePresetsRepository = sourcePresetsRepository,
			onOpenNavConfig = { openDestination(SettingsDestination.NavConfigSettings, null, false) },
			onOpenPanoramaSettings = { openDestination(SettingsDestination.PanoramaSettings, null, false) },
			onOpenProtectSetup = { startActivity(Intent(this, ProtectSetupActivity::class.java)) },
			onOpenBadgesSettings = {
				openDestination(SettingsDestination.AppearanceBadgesSettings, null, false)
			},
			onOpenSearchFiltersSettings = {
				openDestination(SettingsDestination.AppearanceSearchFiltersSettings, null, false)
			},
			onOpenNavigationSettings = {
				openDestination(SettingsDestination.AppearanceNavigationSettings, null, false)
			},
		)
	}

	@Composable
	private fun RenderComposeDestination(destination: SettingsDestination) {
		when (destination) {
			SettingsDestination.Root -> {
				RenderSettingsRootContent(modifier = Modifier.fillMaxSize())
			}
			SettingsDestination.AppearanceSettings -> RenderComposeSection(title = getString(R.string.appearance)) {
				RenderAppearanceSettings(AppearanceSettingsPage.OVERVIEW)
			}
			SettingsDestination.AppearanceBadgesSettings -> RenderComposeSection(
				title = getString(R.string.badges_in_lists),
			) {
				RenderAppearanceSettings(AppearanceSettingsPage.BADGES)
			}
			SettingsDestination.AppearanceSearchFiltersSettings -> RenderComposeSection(
				title = getString(R.string.search_bar_filters),
			) {
				RenderAppearanceSettings(AppearanceSettingsPage.SEARCH_FILTERS)
			}
			SettingsDestination.AppearanceNavigationSettings -> RenderComposeSection(
				title = getString(R.string.appearance_navigation_group),
			) {
				RenderAppearanceSettings(AppearanceSettingsPage.NAVIGATION)
			}
			SettingsDestination.PanoramaSettings -> RenderComposeSection(
				title = getString(R.string.panorama_settings_title),
			) {
				PanoramaSettingsRoute(settings = kototoroAppSettings)
			}
			SettingsDestination.SpacesSettings -> RenderComposeSection(title = getString(R.string.spaces)) {
				SpacesSettingsRoute(settings = kototoroAppSettings)
			}
			SettingsDestination.AISettings -> RenderComposeSection(title = getString(R.string.ai_settings)) {
				AISettingsRoute(
					onOpenOcrModels = { openDestination(SettingsDestination.OcrModelsSettings, null, false) },
					onOpenApiSettings = { openDestination(SettingsDestination.TranslationApiSettings, null, false) },
					onOpenTranslationSettings = { openDestination(SettingsDestination.TranslationSettings, null, false) },
					onOpenImageEnhancementSettings = {
						openDestination(SettingsDestination.AiImageEnhancementSettings, null, false)
					},
				)
			}
			SettingsDestination.OcrModelsSettings -> RenderComposeSection(
				title = getString(R.string.reader_translation_ocr_models_title),
			) {
				OcrModelsRoute(
					onnxModelManager = onnxModelManager,
					settings = kototoroAppSettings,
					modifier = Modifier.fillMaxSize(),
				)
			}
			SettingsDestination.AiImageEnhancementSettings -> RenderComposeSection(
				title = getString(R.string.ai_image_enhancement_settings),
			) {
				AIImageEnhancementSettingsRoute(
					settings = kototoroAppSettings,
					onnxModelManager = onnxModelManager,
					modifier = Modifier.fillMaxSize(),
				)
			}
			SettingsDestination.AiVideoEnhancementSettings -> RenderComposeSection(
				title = getString(R.string.ai_video_enhancement_settings),
			) {
				AIVideoEnhancementSettingsRoute(
					settings = kototoroAppSettings,
				)
			}
			SettingsDestination.PlaybackSettings -> RenderComposeSection(title = getString(R.string.playback_settings)) {
				PlaybackSettingsRoute(
					settings = kototoroAppSettings,
					onAiSettingsClick = {
						openDestination(SettingsDestination.AISettings, null, false)
					},
				)
			}
			SettingsDestination.ReaderSettings -> RenderComposeSection(title = getString(R.string.reader_settings)) {
				ReaderSettingsRoute(
					settings = kototoroAppSettings,
					onReaderTapActionsClick = {
						startActivity(Intent(this, com.mangaverse.app.settings.reader.ReaderTapGridConfigActivity::class.java))
					},
					onReaderAiSettingsEntryClick = {
						openDestination(SettingsDestination.AISettings, null, false)
					},
				)
			}
			SettingsDestination.StorageAndNetworkSettings -> RenderComposeSection(
				title = getString(R.string.storage_and_network),
			) {
				StorageAndNetworkSettingsRoute(
					settings = kototoroAppSettings,
					viewModel = storageAndNetworkSettingsViewModel,
					dataCleanupViewModel = dataCleanupSettingsViewModel,
					onOpenCacheLimits = {
						openDestination(SettingsDestination.CacheLimitsSettings, null, false)
					},
					onOpenDataRemoval = {
						openDestination(SettingsDestination.DataCleanupSettings, null, false)
					},
					onOpenProxySettings = {
						openDestination(SettingsDestination.ProxySettings, null, false)
					},
					onConfirmClearSearchHistory = ::confirmClearSearchHistory,
					onConfirmClearCookies = ::confirmClearCookies,
					onConfirmCleanupChapters = ::confirmCleanupChapters,
					onConfirmClearLocalManga = ::confirmClearLocalManga,
					onConfirmClearLocalNovels = ::confirmClearLocalNovels,
					onConfirmClearLocalVideos = ::confirmClearLocalVideos,
				)
			}
			SettingsDestination.CacheLimitsSettings -> RenderComposeSection(title = getString(R.string.cache_limits)) {
				CacheLimitsSettingsRoute(
					settings = kototoroAppSettings,
					modifier = Modifier.fillMaxSize(),
				)
			}
			SettingsDestination.DataCleanupSettings -> RenderComposeSection(title = getString(R.string.data_removal)) {
				DataCleanupSettingsRoute(
					settings = kototoroAppSettings,
					viewModel = dataCleanupSettingsViewModel,
					onClearLocalManga = ::confirmClearLocalManga,
					onClearLocalNovels = ::confirmClearLocalNovels,
					onClearLocalVideos = ::confirmClearLocalVideos,
					onClearSearchHistory = ::confirmClearSearchHistory,
					onClearCookies = ::confirmClearCookies,
					onDeleteReadChapters = ::confirmCleanupChapters,
					onOpenEntityOrganize = {
						openDestination(SettingsDestination.EntityOrganizeSettings, null, false)
					},
					modifier = Modifier.fillMaxSize(),
				)
			}
			SettingsDestination.SuggestionsSettings -> RenderComposeSection(title = getString(R.string.suggestions)) {
				SuggestionsSettingsRoute(
					settings = kototoroAppSettings,
					suggestionsScheduler = suggestionsScheduler,
					contentSourcesRepository = contentSourcesRepository,
					excludeTagsFlow = suggestionsExcludeTagsFlow,
					preferredTagsFlow = suggestionsPreferredTagsFlow,
				)
			}
			SettingsDestination.BackupsSettings -> RenderComposeSection(title = getString(R.string.backup_restore)) {
				BackupsSettingsRoute(
					settings = kototoroAppSettings,
					viewModel = periodicalBackupSettingsViewModel,
					onBackupOutputClick = {
						if (!backupOutputSelectCall.tryLaunch(null)) {
							Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
						}
					},
					onCreateBackupClick = {
						if (!backupCreateCall.tryLaunch(BackupUtils.generateFileName(this))) {
							Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
						}
					},
					onExportMihonBackupClick = {
						if (!mihonBackupExportCall.tryLaunch(BackupUtils.generateMihonBackupFileName(this))) {
							Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
						}
					},
					onExportKotatsuBackupClick = {
						if (!kotatsuBackupExportCall.tryLaunch(BackupUtils.generateKotatsuBackupFileName(this))) {
							Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
						}
					},
					onExportUsagiBackupClick = {
						if (!usagiBackupExportCall.tryLaunch(BackupUtils.generateUsagiBackupFileName(this))) {
							Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
						}
					},
					onRestoreBackupClick = {
						if (!backupSelectCall.tryLaunch(arrayOf("*/*"))) {
							Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
						}
					},
					onImportKotatsuOrLegacyBackupClick = {
						if (!legacyBackupSelectCall.tryLaunch(arrayOf("*/*"))) {
							Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
						}
					},
					onImportExternalBackupFilePick = { app ->
						pendingExternalBackupApp = app
						if (!externalBackupSelectCall.tryLaunch(arrayOf("*/*"))) {
							pendingExternalBackupApp = null
							Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
						}
					},
				)
			}
			SettingsDestination.EntityOrganizeSettings -> RenderComposeSection(
				title = getString(R.string.entity_organize_title),
			) {
				SourceMigrationPanel(
					initialSelectedContentIds = initialEntityOrganizeSelection,
					onDismiss = ::handleComposeNavigateUp,
					showHeader = false,
				)
			}
			SettingsDestination.TranslationSettings -> RenderComposeSection(
				title = getString(R.string.translation_settings),
			) {
				TranslationSettingsRoute(
					settings = kototoroAppSettings,
					onnxModelManager = onnxModelManager,
					onOpenOcrModels = { openDestination(SettingsDestination.OcrModelsSettings, null, false) },
					onOpenApiSettings = { openDestination(SettingsDestination.TranslationApiSettings, null, false) },
				)
			}
			SettingsDestination.TranslationApiSettings -> RenderComposeSection(
				title = getString(R.string.ai_api_settings),
			) {
				TranslationApiSettingsRoute(
					settings = kototoroAppSettings,
					onFetchModelsClick = ::fetchAndPickTranslationApiModel,
					modifier = Modifier.fillMaxSize(),
				)
			}
			SettingsDestination.TranslationE2EApiSettings -> RenderComposeSection(
				title = getString(R.string.reader_translation_e2e_api_settings_title),
			) {
				TranslationE2EApiSettingsRoute(
					settings = kototoroAppSettings,
					onFetchModelsClick = ::fetchAndPickTranslationE2EApiModel,
				)
			}
			SettingsDestination.DownloadsSettings -> RenderComposeSection(title = getString(R.string.downloads)) {
				val storageRefreshKey by downloadsStorageTick.collectAsStateWithLifecycle()
				val dozeRefreshKey by downloadsDozeTick.collectAsStateWithLifecycle()
				DownloadsSettingsRoute(
					settings = kototoroAppSettings,
					storageManager = storageManager,
					storageRefreshKey = storageRefreshKey,
					dozeRefreshKey = dozeRefreshKey,
					onOpenMangaDirectories = { router.openDirectoriesSettings() },
					onOpenMangaStorage = { router.showDirectorySelectDialog() },
					onOpenNovelStorage = {
						router.showDirectorySelectDialog(
							com.mangaverse.app.settings.storage.ContentDirectorySelectViewModel.CONTENT_TYPE_NOVEL,
						)
					},
					onOpenVideoStorage = {
						router.showDirectorySelectDialog(
							com.mangaverse.app.settings.storage.ContentDirectorySelectViewModel.CONTENT_TYPE_VIDEO,
						)
					},
					onAllowMeteredNetworkChange = { option ->
						kototoroAppSettings.allowDownloadOnMeteredNetwork = option
						updateDownloadsConstraints()
					},
					onRequestIgnoreDoze = ::startDownloadsIgnoreDozeActivity,
					onPickPagesDirectory = { initialUri ->
						pickDownloadsPagesDirectory.tryLaunch(initialUri)
					},
				)
			}
			SettingsDestination.TrackerSettings -> RenderComposeSection(
				title = getString(R.string.check_for_new_chapters),
			) {
				val dozeRefreshKey by trackerDozeTick.collectAsStateWithLifecycle()
				val notificationRefreshKey by trackerNotificationTick.collectAsStateWithLifecycle()
				TrackerSettingsRoute(
					settings = kototoroAppSettings,
					notificationHelper = trackerNotificationHelper,
					viewModel = trackerSettingsViewModel,
					dozeRefreshKey = dozeRefreshKey,
					notificationRefreshKey = notificationRefreshKey,
					onTrackCategoriesClick = { router.showTrackerCategoriesConfigSheet() },
					onOpenNotificationsSettings = ::openTrackerNotificationsSettings,
					onOpenTrackerDebug = {
						startActivity(Intent(this, TrackerDebugActivity::class.java))
					},
					onRequestIgnoreDoze = ::startTrackerIgnoreDozeActivity,
					onOpenTrackerWarning = ::openTrackerWarning,
				)
			}
			SettingsDestination.NotificationSettings -> RenderComposeSection(title = getString(R.string.notifications)) {
				NotificationSettingsRoute(
					settings = kototoroAppSettings,
					onNotificationSoundClick = {
						ringtonePickContract.launch(kototoroAppSettings.notificationSound)
					},
					onNotificationVibrateClick = {
						trackerNotificationHelper.updateChannels()
						startSettingsActivitySafe(
							Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
								.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
								.putExtra(
									android.provider.Settings.EXTRA_CHANNEL_ID,
									TrackerNotificationHelper.CHANNEL_ID,
								),
						)
					},
				)
			}
			SettingsDestination.ServicesSettings -> RenderComposeSection(title = getString(R.string.services)) {
				ServicesSettingsRoute(
					settings = kototoroAppSettings,
					onSuggestionsClick = {
						openDestination(SettingsDestination.SuggestionsSettings, null, false)
					},
				)
			}
			SettingsDestination.ProxySettings -> RenderComposeSection(title = getString(R.string.proxy)) {
				ProxySettingsRoute(
					settings = kototoroAppSettings,
					testSummaryFlow = proxyTestSummaryFlow,
					isTestRunningFlow = proxyIsTestRunningFlow,
					onTestConnection = ::testProxyConnection,
				)
			}
			SettingsDestination.NavConfigSettings -> RenderComposeSection(
				title = getString(R.string.main_screen_sections),
			) {
				NavConfigRoute(
					viewModel = navConfigViewModel,
					modifier = Modifier.fillMaxSize(),
				)
			}
			SettingsDestination.AboutSettings -> RenderComposeSection(title = getString(R.string.about)) {
				AboutSettingsRoute(
					settings = kototoroAppSettings,
					viewModel = aboutSettingsViewModel,
					onLinkClick = { key -> openAboutLink(key) },
					onCrashLogsClick = {
						startActivity(com.mangaverse.app.settings.about.crashlog.CrashLogActivity.newIntent(this))
					},
				)
			}
			is SettingsDestination.SourceSettings -> RenderComposeSection(
				title = composeDestinationTitle(destination),
			) {
				SourceSettingsRoute(appRouter = router)
			}
			SettingsDestination.SourcesSettings -> RenderComposeSection(title = getString(R.string.remote_sources)) {
				SourcesSettingsRoute(
					settings = kototoroAppSettings,
					viewModel = sourcesSettingsViewModel,
					onSetupWizardClick = { router.showWelcomeSheet() },
					onGlobalTagBlacklistClick = {
						startActivity(
							Intent(
								this,
								com.mangaverse.app.settings.sources.blacklist.GlobalTagBlacklistActivity::class.java,
							),
						)
					},
				)
			}
			is SettingsDestination.UnifiedSources -> {
				val readyState by unifiedSourcesViewModel.uiState.collectAsStateWithLifecycle()
				RenderComposeSection(
					title = getString(R.string.extension_management),
					actions = {
						UnifiedSourcesToolbarActions(
							readyState = readyState as? com.mangaverse.app.settings.sources.unified.UnifiedSourcesUiState.Ready,
							searchActive = unifiedSourcesSearchActive,
							onSearchClick = { updateUnifiedSourcesSearchActive(true) },
							onSearchClose = {
								updateUnifiedSourcesSearchActive(false)
								unifiedSourcesViewModel.setSearchQuery("")
							},
							onSearchQueryChange = unifiedSourcesViewModel::setSearchQuery,
							onLanguageFilterClick = {
								unifiedSourcesActivePanel = UnifiedToolbarFilterPanel.LANGUAGE
							},
							onMoreFiltersClick = {
								unifiedSourcesActivePanel = UnifiedToolbarFilterPanel.MORE
							},
							modifier = Modifier.fillMaxSize(),
						)
					},
				) {
					UnifiedSourcesRoute(
						searchActive = unifiedSourcesSearchActive,
						onSearchActiveChange = ::updateUnifiedSourcesSearchActive,
						activePanel = unifiedSourcesActivePanel,
						onActivePanelChange = { unifiedSourcesActivePanel = it },
						initialAddRepositoryKind = destination.initialRepositoryKind,
						initialAddRepositoryUrl = destination.initialRepositoryUrl,
						viewModel = unifiedSourcesViewModel,
						onBrowseSource = { item -> router.openList(item.source, null, null) },
						onOpenSourceSettings = { item -> router.openSourceSettings(item.source) },
						onOpenRepositoryFile = ::openUnifiedSourcesRepositoryFilePicker,
						onOpenLocalJarPicker = ::openUnifiedSourcesLocalJarPicker,
						onStartInstall = { intent ->
							runCatching { unifiedSourcesInstallLauncher.launch(intent) }
								.onFailure { Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
						},
						onStartUninstall = { intent ->
							runCatching { unifiedSourcesUninstallLauncher.launch(intent) }
								.onFailure { Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show() }
						},
						modifier = Modifier.fillMaxSize(),
					)
				}
			}
		}
	}

	@Composable
	private fun RenderSettingsRootContent(modifier: Modifier = Modifier) {
		val enabledSourcesCount by rootSettingsViewModel.enabledSourcesCount.collectAsStateWithLifecycle()
		val searchResults by viewModel.content.collectAsStateWithLifecycle()
		val searchQuery by viewModel.queryText.collectAsStateWithLifecycle()
		val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
		val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0, 0) }
		SettingsTopBarScaffold(
			title = getString(R.string.settings),
			onNavigateUp = ::handleComposeNavigateUp,
			modifier = modifier,
			searchContent = if (isSearchActive) {
				{
					SettingsSearchTopAppBar(
						query = searchQuery,
						onNavigateUp = viewModel::discardSearch,
						onQueryChange = viewModel::onQueryChanged,
					)
				}
			} else {
				null
			},
			actions = {
				SettingsSearchTopBarAction(
					onStartSearch = viewModel::startSearch,
				)
			},
		) { innerPadding ->
			SettingsRootScreen(
				sections = buildSettingsRootSections(
					context = this,
					enabledSourcesCount = enabledSourcesCount,
					totalSourcesCount = rootSettingsViewModel.totalSourcesCount,
					onOpenDestination = { composeDestination ->
						openDestination(composeDestination, null, true)
					},
				),
				searchQuery = searchQuery,
				searchResults = searchResults,
				onSearchResultClick = { item -> navigateToPreference(item) },
				listState = listState,
				topInset = innerPadding.calculateTopPadding(),
				applyHorizontalDisplayCutoutPadding = false,
				modifier = Modifier.fillMaxSize(),
			)
		}
	}

	@Composable
	private fun RenderSettingsDialog() {
		val dialog = pendingSettingsDialog ?: return
		when (dialog) {
			is SettingsDialogState.Confirmation -> {
				SettingsAlertDialog(
					title = stringResource(dialog.titleRes),
					onDismissRequest = { dismissSettingsDialog(dialog) },
					text = { Text(stringResource(dialog.messageRes)) },
					confirmButton = {
						SettingsDialogActionButton(
							text = stringResource(dialog.confirmRes),
							onClick = { confirmSettingsDialog(dialog) },
						)
					},
					dismissButton = {
						SettingsDialogActionButton(
							text = stringResource(android.R.string.cancel),
							onClick = { dismissSettingsDialog(dialog) },
						)
					},
				)
			}
			is SettingsDialogState.ModelPicker -> {
				SettingsAlertDialog(
					title = stringResource(dialog.titleRes),
					onDismissRequest = { dismissSettingsDialog(dialog) },
					text = {
						LazyColumn(
							modifier = Modifier
								.fillMaxWidth()
								.heightIn(max = 360.dp),
						) {
							itemsIndexed(dialog.models) { index, model ->
								Row(
									modifier = Modifier
										.fillMaxWidth()
										.selectable(
											selected = dialog.selectedIndex == index,
											onClick = { selectModel(dialog, index) },
										)
										.padding(vertical = 4.dp),
									verticalAlignment = Alignment.CenterVertically,
								) {
									if (dialog.selectedIndex != null) {
										RadioButton(
											selected = dialog.selectedIndex == index,
											onClick = null,
										)
									}
									Text(
										text = model,
										modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
									)
								}
							}
							}
					},
					confirmButton = {},
					dismissButton = {
						SettingsDialogActionButton(
							text = stringResource(android.R.string.cancel),
							onClick = { dismissSettingsDialog(dialog) },
						)
					},
				)
			}
			is SettingsDialogState.ProxyTestResult -> {
				SettingsAlertDialog(
					title = stringResource(R.string.proxy),
					onDismissRequest = { dismissSettingsDialog(dialog) },
					text = { Text(dialog.message) },
					confirmButton = {
						SettingsDialogActionButton(
							text = stringResource(android.R.string.ok),
							onClick = { dismissSettingsDialog(dialog) },
						)
					},
				)
			}
		}
	}

	private fun showConfirmationDialog(
		titleRes: Int,
		messageRes: Int,
		confirmRes: Int,
		onConfirm: () -> Unit,
	) {
		pendingSettingsDialog = SettingsDialogState.Confirmation(
			titleRes = titleRes,
			messageRes = messageRes,
			confirmRes = confirmRes,
			onConfirm = onConfirm,
		)
	}

	private fun dismissSettingsDialog(dialog: SettingsDialogState) {
		if (pendingSettingsDialog === dialog) {
			pendingSettingsDialog = null
		}
	}

	private fun confirmSettingsDialog(dialog: SettingsDialogState.Confirmation) {
		if (pendingSettingsDialog !== dialog) return
		pendingSettingsDialog = null
		dialog.onConfirm()
	}

	private fun selectModel(dialog: SettingsDialogState.ModelPicker, index: Int) {
		if (pendingSettingsDialog !== dialog) return
		val model = dialog.models.getOrNull(index).orEmpty()
		pendingSettingsDialog = null
		if (model.isNotBlank()) {
			dialog.onSelected(model)
		}
	}

	private fun bindDataCleanupObservers() {
		if (isDataCleanupObserversBound) return
		isDataCleanupObserversBound = true
		dataCleanupSettingsViewModel.onError.observeEvent(this, SnackbarErrorObserver(contentRoot, null))
		dataCleanupSettingsViewModel.onActionDone.observeEvent(this, ReversibleActionObserver(contentRoot))
		dataCleanupSettingsViewModel.onChaptersCleanedUp.observeEvent(this, ::onDataCleanupChaptersCleanedUp)
		dataCleanupSettingsViewModel.onStorageChanged.observeEvent(this) {
			storageAndNetworkSettingsViewModel.refreshStorageUsage()
		}
		dataCleanupSettingsViewModel.onLocalContentCleanedUp.observeEvent(this, ::onLocalContentCleanedUp)
	}

	private fun onDataCleanupChaptersCleanedUp(result: Pair<Int, Long>) {
		val text = if (result.first == 0 && result.second == 0L) {
			getString(R.string.no_chapters_deleted)
		} else {
			getString(
				R.string.chapters_deleted_pattern,
				resources.getQuantityStringSafe(R.plurals.chapters, result.first, result.first),
				FileSize.BYTES.format(this, result.second),
			)
		}
		Snackbar.make(contentRoot, text, Snackbar.LENGTH_SHORT).show()
	}

	private fun confirmClearSearchHistory() {
		showConfirmationDialog(
			titleRes = R.string.clear_search_history,
			messageRes = R.string.text_clear_search_history_prompt,
			confirmRes = R.string.clear,
			onConfirm = dataCleanupSettingsViewModel::clearSearchHistory,
		)
	}

	private fun confirmClearCookies() {
		showConfirmationDialog(
			titleRes = R.string.clear_cookies,
			messageRes = R.string.text_clear_cookies_prompt,
			confirmRes = R.string.clear,
			onConfirm = dataCleanupSettingsViewModel::clearCookies,
		)
	}

	private fun confirmCleanupChapters() {
		showConfirmationDialog(
			titleRes = R.string.delete_read_chapters,
			messageRes = R.string.delete_read_chapters_prompt,
			confirmRes = R.string.delete,
			onConfirm = dataCleanupSettingsViewModel::cleanupChapters,
		)
	}

	private fun onLocalContentCleanedUp(result: DataCleanupSettingsViewModel.LocalContentCleanupResult) {
		val labelRes = when (result.kind) {
			com.mangaverse.app.local.data.StorageContentKind.MANGA -> R.string.local_manga_storage
			com.mangaverse.app.local.data.StorageContentKind.NOVEL -> R.string.local_novel_storage
			com.mangaverse.app.local.data.StorageContentKind.VIDEO -> R.string.local_video_storage
		}
		val text = if (result.removedCount == 0 && result.bytesFreed == 0L) {
			getString(R.string.no_local_content_deleted)
		} else {
			getString(
				R.string.local_content_deleted_pattern,
				getString(labelRes),
				resources.getQuantityStringSafe(R.plurals.items, result.removedCount, result.removedCount),
				FileSize.BYTES.format(this, result.bytesFreed),
			)
		}
		Snackbar.make(contentRoot, text, Snackbar.LENGTH_SHORT).show()
	}

	private fun confirmClearLocalManga() {
		showConfirmationDialog(
			titleRes = R.string.clear_local_manga_storage,
			messageRes = R.string.clear_local_manga_storage_prompt,
			confirmRes = R.string.clear,
			onConfirm = dataCleanupSettingsViewModel::clearLocalMangaContent,
		)
	}

	private fun confirmClearLocalNovels() {
		showConfirmationDialog(
			titleRes = R.string.clear_local_novel_storage,
			messageRes = R.string.clear_local_novel_storage_prompt,
			confirmRes = R.string.clear,
			onConfirm = dataCleanupSettingsViewModel::clearLocalNovelContent,
		)
	}

	private fun confirmClearLocalVideos() {
		showConfirmationDialog(
			titleRes = R.string.clear_local_video_storage,
			messageRes = R.string.clear_local_video_storage_prompt,
			confirmRes = R.string.clear,
			onConfirm = dataCleanupSettingsViewModel::clearLocalVideoContent,
		)
	}

	private fun fetchAndPickTranslationApiModel() {
		translationApiFetchModelsJob?.cancel()
		translationApiFetchModelsJob = lifecycleScope.launch {
			try {
				val providerId = kototoroAppSettings.readerTranslationApiProviderPreset
				val endpoint = TranslationApiProviderCatalog.resolveChatEndpoint(
					providerId,
					kototoroAppSettings.readerTranslationApiEndpoint,
				)
				if (endpoint.isBlank()) {
					Toast.makeText(this@SettingsActivity, R.string.reader_translation_api_endpoint_missing, Toast.LENGTH_SHORT).show()
					return@launch
				}
				val modelsUrl = TranslationApiSettingsSupport.buildModelsUrl(endpoint, providerId)
				val key = kototoroAppSettings.readerTranslationApiKey.trim()
				val models = withContext(Dispatchers.IO) {
					val requestBuilder = Request.Builder().get().url(modelsUrl)
					TranslationApiProviderCatalog.applyAuthentication(requestBuilder, providerId, key)
					okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
						if (!response.isSuccessful) return@withContext emptyList<String>()
						TranslationApiSettingsSupport.parseModelIds(response.body?.string().orEmpty())
					}
				}
				if (models.isEmpty()) {
					Toast.makeText(this@SettingsActivity, R.string.reader_translation_api_models_fetch_failed, Toast.LENGTH_SHORT).show()
					return@launch
				}
				showTranslationApiModelPicker(models)
			} catch (_: Throwable) {
				Toast.makeText(this@SettingsActivity, R.string.reader_translation_api_models_fetch_failed, Toast.LENGTH_SHORT).show()
			}
		}
	}

	private fun showTranslationApiModelPicker(models: List<String>) {
		val current = kototoroAppSettings.readerTranslationApiModel.trim()
		val selected = models.indexOf(current).coerceAtLeast(0)
		pendingSettingsDialog = SettingsDialogState.ModelPicker(
			titleRes = R.string.reader_translation_api_models_pick_title,
			models = models,
			selectedIndex = selected,
			onSelected = { chosen ->
				PreferenceManager.getDefaultSharedPreferences(this).edit {
					putString(AppSettings.KEY_READER_TRANSLATION_API_MODEL, chosen)
				}
			},
		)
	}

	private fun fetchAndPickTranslationE2EApiModel() {
		if (translationE2EApiFetchModelsJob?.isActive == true) return

		val endpoint = kototoroAppSettings.readerE2eApiEndpoint
		val apiKey = kototoroAppSettings.readerE2eApiKey
		if (endpoint.isEmpty() || apiKey.isEmpty()) {
			Toast.makeText(this, R.string.reader_translation_api_endpoint_missing, Toast.LENGTH_SHORT).show()
			return
		}

		val request = Request.Builder()
			.url(endpoint.removeSuffix("/chat/completions").removeSuffix("/") + "/models")
			.get()
			.header("Authorization", "Bearer $apiKey")
			.build()

		translationE2EApiFetchModelsJob = lifecycleScope.launch(Dispatchers.IO) {
			try {
				val response = okHttpClient.newCall(request).execute()
				val bodyStr = response.body?.string()
				if (!response.isSuccessful || bodyStr == null) {
					withContext(Dispatchers.Main) {
						Toast.makeText(this@SettingsActivity, R.string.reader_translation_api_models_fetch_failed, Toast.LENGTH_SHORT).show()
					}
					return@launch
				}
				val models = TranslationApiSettingsSupport.parseModelIds(bodyStr)
				withContext(Dispatchers.Main) {
					if (models.isEmpty()) {
						Toast.makeText(this@SettingsActivity, R.string.reader_translation_api_models_fetch_failed, Toast.LENGTH_SHORT).show()
						return@withContext
					}
					pendingSettingsDialog = SettingsDialogState.ModelPicker(
						titleRes = R.string.reader_translation_api_models_fetch,
						models = models,
						onSelected = { chosen ->
							kototoroAppSettings.prefs.edit()
								.putString(AppSettings.KEY_READER_E2E_API_MODEL, chosen)
								.apply()
						},
					)
				}
			} catch (_: Exception) {
				withContext(Dispatchers.Main) {
					Toast.makeText(this@SettingsActivity, R.string.reader_translation_api_models_fetch_failed, Toast.LENGTH_SHORT).show()
				}
			}
		}
	}

	private fun handleComposeNavigateUp() {
		val currentDestination = composeDestination ?: return
		val previousDestination = composeNavigationStack.lastOrNull()
		if (currentDestination == SettingsDestination.Root) {
			finishFromComposeDestination(currentDestination)
			return
		}
		if (shouldKeepComposeHistory() && previousDestination != null) {
			onLeavingComposeDestination(currentDestination)
			composeNavigationStack.removeLast()
			openComposeDestination(
				destination = previousDestination,
				shouldRestoreFragment = false,
				pushCurrentToStack = false,
			)
			return
		}
		if (isMasterDetails) {
			onLeavingComposeDestination(currentDestination)
			openComposeDestination(
				destination = SettingsDestination.Root,
				shouldRestoreFragment = false,
				pushCurrentToStack = false,
			)
			return
		}
		finishFromComposeDestination(currentDestination)
	}

	private fun onLeavingComposeDestination(destination: SettingsDestination) {
	}

	private fun finishFromComposeDestination(destination: SettingsDestination) {
		onLeavingComposeDestination(destination)
		composeBackCallback.isEnabled = false
		finishAfterTransition()
	}

	private fun openUnifiedSourcesRepositoryFilePicker(kind: UnifiedSourceKind) {
		pendingUnifiedSourcesFileImportKind = kind
		openUnifiedSourcesRepositoryFile.launch(
			arrayOf(
				"application/json",
				"text/plain",
				"application/javascript",
				"text/javascript",
				"*/*",
			),
		)
	}

	private fun openUnifiedSourcesLocalJarPicker() {
		openUnifiedSourcesLocalJar.launch(
			arrayOf(
				"application/java-archive",
				"application/zip",
				"*/*",
			),
		)
	}

	private fun persistReadPermission(uri: Uri) {
		runCatching {
			contentResolver.takePersistableUriPermission(
				uri,
				Intent.FLAG_GRANT_READ_URI_PERMISSION,
			)
		}
	}

	private fun resolveInitialUnifiedSourcesDestination(intent: Intent?): SettingsDestination.UnifiedSources? {
		if (intent == null) {
			return null
		}
		if (intent.action == AppRouter.ACTION_MANAGE_SOURCES) {
			return SettingsDestination.UnifiedSources(
				initialRepositoryKind = intent.getStringExtra(EXTRA_UNIFIED_SOURCES_KIND)
					?.let { runCatching { enumValueOf<UnifiedSourceKind>(it) }.getOrNull() },
				initialRepositoryUrl = intent.getStringExtra(EXTRA_UNIFIED_SOURCES_URL),
			)
		}
		if (intent.action == Intent.ACTION_VIEW && intent.data?.host == HOST_ADD_REPO) {
			return SettingsDestination.UnifiedSources(
				initialRepositoryKind = UnifiedSourceKind.MIHON,
				initialRepositoryUrl = intent.data?.getQueryParameter("url"),
			)
		}
		return null
	}

	private fun refreshSuggestionsTags() {
		suggestionsExcludeTagsFlow.value =
			kototoroAppSettings.prefs.getString(AppSettings.KEY_SUGGESTIONS_EXCLUDE_TAGS, "") ?: ""
		suggestionsPreferredTagsFlow.value =
			kototoroAppSettings.prefs.getString(AppSettings.KEY_SUGGESTIONS_PREFERRED_TAGS, "") ?: ""
	}

	private fun clearSuperResolutionCache() {
		lifecycleScope.launch(Dispatchers.IO) {
			val srCacheDir = java.io.File(cacheDir, "sr_cache")
			var deletedCount = 0
			if (srCacheDir.exists() && srCacheDir.isDirectory) {
				srCacheDir.listFiles()?.forEach { file ->
					if (file.delete()) {
						deletedCount++
					}
				}
			}
			withContext(Dispatchers.Main) {
				Toast.makeText(
					this@SettingsActivity,
					getString(R.string.reader_super_resolution_cache_cleared) + " ($deletedCount files)",
					Toast.LENGTH_SHORT,
				).show()
			}
		}
	}

	private fun onDownloadsPagesDirectoryPicked(uri: Uri) {
		storageManager.takePermissions(uri)
		val doc = DocumentFile.fromTreeUri(this, uri)?.takeIf { it.canWrite() }
		kototoroAppSettings.setPagesSaveDir(doc?.uri)
		downloadsStorageTick.update { it + 1 }
	}

	private fun openTrackerNotificationsSettings(onUnsupported: () -> Unit) {
		when {
			android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O -> {
				val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
					.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
				if (!startSettingsActivitySafe(intent)) {
					onUnsupported()
				}
			}
			!trackerNotificationHelper.getAreNotificationsEnabled() -> {
				val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
					.setData(android.net.Uri.fromParts("package", packageName, null))
				if (!startSettingsActivitySafe(intent)) {
					onUnsupported()
				}
			}
			else -> {
				openDestination(SettingsDestination.NotificationSettings, null, false)
			}
		}
	}

	private fun openTrackerWarning(onUnsupported: () -> Unit) {
		val intent = Intent(Intent.ACTION_VIEW, "https://dontkillmyapp.com/".toUri())
		if (!startSettingsActivitySafe(intent)) {
			onUnsupported()
		}
	}

	private fun startTrackerIgnoreDozeActivity(): Boolean {
		return startIgnoreDozeActivity(this, ignoreTrackerDozeLauncher)
	}

	private fun updateDownloadsConstraints() {
		lifecycleScope.launch {
			runCatching {
				when (kototoroAppSettings.allowDownloadOnMeteredNetwork) {
					com.mangaverse.app.core.prefs.TriStateOption.ENABLED -> downloadsScheduler.updateConstraints(true)
					com.mangaverse.app.core.prefs.TriStateOption.ASK -> Unit
					com.mangaverse.app.core.prefs.TriStateOption.DISABLED -> downloadsScheduler.updateConstraints(false)
				}
			}.onFailure {
				it.printStackTrace()
			}
		}
	}

	private fun startDownloadsIgnoreDozeActivity(): Boolean {
		return startIgnoreDozeActivity(this, ignoreDownloadsDozeLauncher)
	}

	private fun testProxyConnection() {
		proxyTestJob?.cancel()
		proxyTestJob = lifecycleScope.launch {
			proxyTestSummaryFlow.value = getString(R.string.loading_)
			proxyIsTestRunningFlow.value = true
			try {
				withContext(Dispatchers.Default) {
					val request = Request.Builder()
						.get()
						.url("http://neverssl.com")
						.build()
					okHttpClient.newCall(request).await().use { response ->
						check(response.isSuccessful) { response.message }
					}
				}
				showProxyTestResult(null)
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				e.printStackTraceDebug()
				showProxyTestResult(e)
			} finally {
				proxyIsTestRunningFlow.value = false
				proxyTestSummaryFlow.value = null
			}
		}
	}

	private fun showProxyTestResult(error: Throwable?) {
		pendingSettingsDialog = SettingsDialogState.ProxyTestResult(
			message = error?.getDisplayMessage(resources) ?: getString(R.string.connection_ok),
		)
	}

	private fun openAboutLink(key: String): Boolean {
		val urlRes = when (key) {
			com.mangaverse.app.core.prefs.AppSettings.KEY_LINK_WEBLATE -> R.string.url_weblate
			com.mangaverse.app.core.prefs.AppSettings.KEY_LINK_GITHUB -> R.string.url_github
			com.mangaverse.app.core.prefs.AppSettings.KEY_LINK_DONATE -> R.string.url_donate
			com.mangaverse.app.core.prefs.AppSettings.KEY_LINK_MANUAL -> R.string.url_user_manual
			else -> return false
		}
		val title = when (key) {
			com.mangaverse.app.core.prefs.AppSettings.KEY_LINK_WEBLATE -> getString(R.string.about_app_translation_summary)
			com.mangaverse.app.core.prefs.AppSettings.KEY_LINK_GITHUB -> getString(R.string.source_code)
			com.mangaverse.app.core.prefs.AppSettings.KEY_LINK_DONATE -> getString(R.string.about_donate)
			com.mangaverse.app.core.prefs.AppSettings.KEY_LINK_MANUAL -> getString(R.string.user_manual)
			else -> null
		}
		return if (router.openExternalBrowser(getString(urlRes), title)) {
			true
		} else {
			Toast.makeText(this, R.string.operation_not_supported, Toast.LENGTH_SHORT).show()
			false
		}
	}

	private fun startSettingsActivitySafe(intent: Intent): Boolean {
		return runCatching {
			startActivity(intent)
		}.isSuccess
	}

	companion object {

		private const val HOST_ABOUT = "about"
		private const val HOST_ADD_REPO = "add-repo"
		const val EXTRA_USE_HORIZONTAL_ROUTE_TRANSITION = "use_horizontal_route_transition"
		const val ARG_PREF_KEY = "pref_key"
		private const val EXTRA_UNIFIED_SOURCES_KIND = "extra_unified_sources_kind"
		private const val EXTRA_UNIFIED_SOURCES_URL = "extra_unified_sources_url"
		private const val STATE_COMPOSE_DESTINATION = "compose_destination"
		private const val STATE_SOURCE_SETTINGS_SOURCE = "source_settings_source"
		private const val STATE_UNIFIED_SOURCES_KIND = "unified_sources_kind"
		private const val STATE_UNIFIED_SOURCES_URL = "unified_sources_url"
		private const val COMPOSE_DESTINATION_ROOT = "root"
		private const val COMPOSE_DESTINATION_APPEARANCE_SETTINGS = "appearance_settings"
		private const val COMPOSE_DESTINATION_APPEARANCE_BADGES_SETTINGS = "appearance_badges_settings"
		private const val COMPOSE_DESTINATION_APPEARANCE_SEARCH_FILTERS_SETTINGS = "appearance_search_filters_settings"
		private const val COMPOSE_DESTINATION_APPEARANCE_NAVIGATION_SETTINGS = "appearance_navigation_settings"
		private const val COMPOSE_DESTINATION_PANORAMA_SETTINGS = "panorama_settings"
		private const val COMPOSE_DESTINATION_SPACES_SETTINGS = "spaces_settings"
		private const val COMPOSE_DESTINATION_AI_SETTINGS = "ai_settings"
		private const val COMPOSE_DESTINATION_OCR_MODELS_SETTINGS = "ocr_models_settings"
		private const val COMPOSE_DESTINATION_AI_IMAGE_ENHANCEMENT_SETTINGS = "ai_image_enhancement_settings"
		private const val COMPOSE_DESTINATION_AI_VIDEO_ENHANCEMENT_SETTINGS = "ai_video_enhancement_settings"
		private const val COMPOSE_DESTINATION_PLAYBACK_SETTINGS = "playback_settings"
		private const val COMPOSE_DESTINATION_READER_SETTINGS = "reader_settings"
		private const val COMPOSE_DESTINATION_SOURCES_SETTINGS = "sources_settings"
		private const val COMPOSE_DESTINATION_SUGGESTIONS_SETTINGS = "suggestions_settings"
		private const val COMPOSE_DESTINATION_BACKUPS_SETTINGS = "backups_settings"
		private const val COMPOSE_DESTINATION_ENTITY_ORGANIZE_SETTINGS = "entity_organize_settings"
		private const val COMPOSE_DESTINATION_TRANSLATION_SETTINGS = "translation_settings"
		private const val COMPOSE_DESTINATION_TRANSLATION_API_SETTINGS = "translation_api_settings"
		private const val COMPOSE_DESTINATION_TRANSLATION_E2E_API_SETTINGS = "translation_e2e_api_settings"
		private const val COMPOSE_DESTINATION_STORAGE_AND_NETWORK_SETTINGS = "storage_and_network_settings"
		private const val COMPOSE_DESTINATION_CACHE_LIMITS_SETTINGS = "cache_limits_settings"
		private const val COMPOSE_DESTINATION_DATA_CLEANUP_SETTINGS = "data_cleanup_settings"
		private const val COMPOSE_DESTINATION_DOWNLOADS_SETTINGS = "downloads_settings"
		private const val COMPOSE_DESTINATION_TRACKER_SETTINGS = "tracker_settings"
		private const val COMPOSE_DESTINATION_NOTIFICATION_SETTINGS = "notification_settings"
		private const val COMPOSE_DESTINATION_SERVICES_SETTINGS = "services_settings"
		private const val COMPOSE_DESTINATION_PROXY_SETTINGS = "proxy_settings"
		private const val COMPOSE_DESTINATION_NAV_CONFIG_SETTINGS = "nav_config_settings"
		private const val COMPOSE_DESTINATION_ABOUT_SETTINGS = "about_settings"
		private const val COMPOSE_DESTINATION_SOURCE_SETTINGS = "source_settings"
		private const val COMPOSE_DESTINATION_UNIFIED_SOURCES = "unified_sources"
		private const val EXTRA_ENTITY_ORGANIZE_SELECTION = "entity_organize_selection"

		fun newUnifiedSourcesIntent(
			context: Context,
			initialRepositoryKind: UnifiedSourceKind? = null,
			initialRepositoryUrl: String? = null,
		): Intent {
			return Intent(context, SettingsActivity::class.java)
				.setAction(AppRouter.ACTION_MANAGE_SOURCES)
				.apply {
					if (initialRepositoryKind != null) {
						putExtra(EXTRA_UNIFIED_SOURCES_KIND, initialRepositoryKind.name)
					}
					if (initialRepositoryUrl != null) {
						putExtra(EXTRA_UNIFIED_SOURCES_URL, initialRepositoryUrl)
					}
				}
		}

		fun newEntityOrganizeIntent(
			context: Context,
			selectedContentIds: Set<Long> = emptySet(),
		): Intent {
			return Intent(context, SettingsActivity::class.java)
				.setAction(AppRouter.ACTION_ENTITY_ORGANIZE)
				.putExtra(
					EXTRA_ENTITY_ORGANIZE_SELECTION,
					encodeEntityOrganizeSelection(selectedContentIds),
				)
		}
	}

	private fun applyCloseRouteTransitionIfNeeded() {
		if (hasAppliedCloseRouteTransition) {
			return
		}
		if (!intent.getBooleanExtra(EXTRA_USE_HORIZONTAL_ROUTE_TRANSITION, false)) {
			return
		}
		hasAppliedCloseRouteTransition = true
		applyHorizontalRouteCloseTransition()
	}

	private fun Bundle.toComposeDestination(): SettingsDestination? {
		return when (getString(STATE_COMPOSE_DESTINATION)) {
			COMPOSE_DESTINATION_ROOT -> SettingsDestination.Root
			COMPOSE_DESTINATION_APPEARANCE_SETTINGS -> SettingsDestination.AppearanceSettings
			COMPOSE_DESTINATION_APPEARANCE_BADGES_SETTINGS -> SettingsDestination.AppearanceBadgesSettings
			COMPOSE_DESTINATION_APPEARANCE_SEARCH_FILTERS_SETTINGS ->
				SettingsDestination.AppearanceSearchFiltersSettings
			COMPOSE_DESTINATION_APPEARANCE_NAVIGATION_SETTINGS -> SettingsDestination.AppearanceNavigationSettings
			COMPOSE_DESTINATION_PANORAMA_SETTINGS -> SettingsDestination.PanoramaSettings
			COMPOSE_DESTINATION_SPACES_SETTINGS -> SettingsDestination.SpacesSettings
			COMPOSE_DESTINATION_AI_SETTINGS -> SettingsDestination.AISettings
			COMPOSE_DESTINATION_OCR_MODELS_SETTINGS -> SettingsDestination.OcrModelsSettings
			COMPOSE_DESTINATION_AI_IMAGE_ENHANCEMENT_SETTINGS -> SettingsDestination.AiImageEnhancementSettings
			COMPOSE_DESTINATION_AI_VIDEO_ENHANCEMENT_SETTINGS -> SettingsDestination.AiVideoEnhancementSettings
			COMPOSE_DESTINATION_PLAYBACK_SETTINGS -> SettingsDestination.PlaybackSettings
			COMPOSE_DESTINATION_READER_SETTINGS -> SettingsDestination.ReaderSettings
			COMPOSE_DESTINATION_SOURCES_SETTINGS -> SettingsDestination.SourcesSettings
			COMPOSE_DESTINATION_SUGGESTIONS_SETTINGS -> SettingsDestination.SuggestionsSettings
			COMPOSE_DESTINATION_BACKUPS_SETTINGS -> SettingsDestination.BackupsSettings
			COMPOSE_DESTINATION_ENTITY_ORGANIZE_SETTINGS -> SettingsDestination.EntityOrganizeSettings
			COMPOSE_DESTINATION_TRANSLATION_SETTINGS -> SettingsDestination.TranslationSettings
			COMPOSE_DESTINATION_TRANSLATION_API_SETTINGS -> SettingsDestination.TranslationApiSettings
			COMPOSE_DESTINATION_TRANSLATION_E2E_API_SETTINGS -> SettingsDestination.TranslationE2EApiSettings
			COMPOSE_DESTINATION_STORAGE_AND_NETWORK_SETTINGS -> SettingsDestination.StorageAndNetworkSettings
			COMPOSE_DESTINATION_CACHE_LIMITS_SETTINGS -> SettingsDestination.CacheLimitsSettings
			COMPOSE_DESTINATION_DATA_CLEANUP_SETTINGS -> SettingsDestination.DataCleanupSettings
			COMPOSE_DESTINATION_DOWNLOADS_SETTINGS -> SettingsDestination.DownloadsSettings
			COMPOSE_DESTINATION_TRACKER_SETTINGS -> SettingsDestination.TrackerSettings
			COMPOSE_DESTINATION_NOTIFICATION_SETTINGS -> SettingsDestination.NotificationSettings
			COMPOSE_DESTINATION_SERVICES_SETTINGS -> SettingsDestination.ServicesSettings
			COMPOSE_DESTINATION_PROXY_SETTINGS -> SettingsDestination.ProxySettings
			COMPOSE_DESTINATION_NAV_CONFIG_SETTINGS -> SettingsDestination.NavConfigSettings
			COMPOSE_DESTINATION_ABOUT_SETTINGS -> SettingsDestination.AboutSettings
			COMPOSE_DESTINATION_SOURCE_SETTINGS -> getString(STATE_SOURCE_SETTINGS_SOURCE)
				?.takeIf { it.isNotBlank() }
				?.let(SettingsDestination::SourceSettings)
			COMPOSE_DESTINATION_UNIFIED_SOURCES -> SettingsDestination.UnifiedSources(
				initialRepositoryKind = getString(STATE_UNIFIED_SOURCES_KIND)
					?.let { runCatching { enumValueOf<UnifiedSourceKind>(it) }.getOrNull() },
				initialRepositoryUrl = getString(STATE_UNIFIED_SOURCES_URL),
			)
			else -> null
		}
	}

	private fun observeFoldableState() {
		val foldableState = FoldableUtils.observeFoldableState(this, this)
		
		lifecycleScope.launch {
			foldableState.collect { unfolded ->
				if (unfolded != isFoldUnfolded) {
					isFoldUnfolded = unfolded
					adjustLayoutForFoldableState()
				}
			}
		}
	}

    private fun adjustLayoutForFoldableState() {
        // 设置页不改变屏幕方向，仅保持默认方向
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

		// 仅在折叠屏展开且窗口满足双栏宽度时重建，避免分屏窄窗口反复重建
		setLegacyTopBarVisible(false)
    }
}
