package com.mangaverse.app.settings.sources.unified

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.Json
import com.mangaverse.app.core.LocalizedAppContext
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.core.db.TABLE_JSON_SOURCES
import com.mangaverse.app.core.db.TABLE_SOURCES
import com.mangaverse.app.core.db.entity.JsonSourceEntity
import com.mangaverse.app.core.db.entity.JsonSourceType
import com.mangaverse.app.core.db.entity.MangaSourceEntity
import com.mangaverse.app.core.extensions.GlobalExtensionManager
import com.mangaverse.app.core.extensions.PluginContentSource
import com.mangaverse.app.core.extensions.PluginMangaSource
import com.mangaverse.app.core.jsonsource.JsonContentSource
import com.mangaverse.app.core.jsonsource.JsonSourceImportMetadata
import com.mangaverse.app.core.jsonsource.JsonSourceListSource
import com.mangaverse.app.core.jsonsource.JsonSourceManager
import com.mangaverse.app.core.model.getContentType
import com.mangaverse.app.core.model.ContentSourceAvailability
import com.mangaverse.app.core.model.getLocale
import com.mangaverse.app.core.model.getTitle
import com.mangaverse.app.core.model.isBroken
import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.core.model.jsonsource.LegadoBookSource
import com.mangaverse.app.core.parser.kotatsu.KotatsuParserSource
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.observeAsFlow
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.explore.data.SourceAvailabilityRepository
import com.mangaverse.app.extensions.repo.ExternalExtensionRepo
import com.mangaverse.app.extensions.repo.ExternalExtensionRepoRepository
import com.mangaverse.app.extensions.repo.ExternalExtensionType
import com.mangaverse.app.mihon.MihonExtensionManager
import com.mangaverse.app.mihon.model.MihonMangaSource
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.settings.sources.extensions.normalizeExtensionLanguageCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedSourceCatalogRepository @Inject constructor(
	@ApplicationContext private val appContext: Context,
	@LocalizedAppContext private val localizedContext: Context,
	private val database: MangaDatabase,
	private val settings: AppSettings,
	private val contentSourcesRepository: ContentSourcesRepository,
	private val sourceAvailabilityRepository: SourceAvailabilityRepository,
	private val jsonSourceManager: JsonSourceManager,
	private val extensionRepoRepository: ExternalExtensionRepoRepository,
	private val mihonExtensionManager: MihonExtensionManager,
	private val json: Json,
) {

	fun observeState(): Flow<UnifiedSourceCatalogState> {
		return combine(
			observeRepositories(),
			observePackages(),
			observeSources(),
		) { repositories, packages, sources ->
			UnifiedSourceCatalogState(
				repositories = repositories,
				packages = packages,
				sources = sources,
			)
		}
	}

	fun observeRepositories(): Flow<List<UnifiedSourceRepositoryItem>> {
		val externalRepos = combine(
			extensionRepoRepository.observeByType(ExternalExtensionType.JAR),
			extensionRepoRepository.observeByType(ExternalExtensionType.MIHON),
		) { jar, mihon ->
			jar + mihon
		}
		return combine(
			externalRepos,
			database.getJsonSourceDao().observeAll(),
		) { external, jsonSources ->
			val configured = external.map { it.toUnifiedRepositoryItem(isPreset = false) } +
				jsonSources.toJsonRepositoryItems()

			configured.withPresetRepositories()
		}
	}

	fun observePackages(): Flow<List<UnifiedSourcePackageItem>> {
		val apkPackages = mihonExtensionManager.installedExtensions.map { extensions ->
			extensions.map { extension ->
				UnifiedSourcePackageItem(
					id = packageId(UnifiedSourceKind.MIHON, extension.pkgName),
					kind = UnifiedSourceKind.MIHON,
					name = extension.appName.removePrefix("Tachiyomi: "),
					packageName = extension.pkgName,
					repositoryId = null,
					repositoryName = null,
					versionName = extension.versionName,
					versionCode = extension.versionCode,
					libVersion = extension.libVersion,
					language = extension.lang.normalizeExtensionLanguageCode(),
					isInstalled = true,
					isNsfw = extension.isNsfw,
					sourceCount = extension.sources.size,
					sourceNames = extension.sources.map { it.name },
					installLocation = if (extension.isManagedLocal) {
						UnifiedSourcePackageInstallLocation.LOCAL_APK
					} else {
						UnifiedSourcePackageInstallLocation.SYSTEM
					},
				)
			}
		}

		val jarPackages = combine(
			GlobalExtensionManager.mangaSources,
			GlobalExtensionManager.contentSources,
		) { mangaSources, contentSources ->
			val versionPrefs = appContext.getSharedPreferences("jar_plugin_versions", Context.MODE_PRIVATE)
			(mangaSources.map { it.jarName to it.name } + contentSources.map { it.jarName to it.name })
				.groupBy(keySelector = { it.first }, valueTransform = { it.second })
				.map { (jarName, sourceNames) ->
					val packageName = jarName.removeSuffix(".jar")
					UnifiedSourcePackageItem(
						id = packageId(UnifiedSourceKind.JAR, packageName),
						kind = UnifiedSourceKind.JAR,
						name = packageName,
						packageName = packageName,
						repositoryId = null,
						repositoryName = null,
						versionName = versionPrefs.getLong(packageName, 1L).toString(),
						versionCode = versionPrefs.getLong(packageName, 1L),
						libVersion = 1.0,
						language = null,
						isInstalled = true,
						isNsfw = false,
						sourceCount = sourceNames.size,
						sourceNames = sourceNames.sorted(),
					)
				}
		}

		val jsonPackages = database.getJsonSourceDao().observeAll().map { sources ->
			sources.toJsonPackageItems()
		}

		return combine(apkPackages, jarPackages, jsonPackages) { apk, jar, json ->
			(apk + jar + json).sortedWith(compareBy({ it.kind.ordinal }, { it.name.lowercase() }))
		}
	}

	fun observeSources(): Flow<List<UnifiedSourceItem>> {
		val dbChanges = database.invalidationTracker.createFlow(TABLE_SOURCES, TABLE_JSON_SOURCES)
			.onStart { emit(emptySet()) }
		val runtimeChanges = observeRuntimeSourceChanges()
		val settingsChanges = settings.observeAsFlow(AppSettings.KEY_SOURCES_ENABLED_ALL) {
			isAllSourcesEnabled
		}
		return combine(dbChanges, runtimeChanges, settingsChanges) { _, _, _ -> Unit }
			.mapLatest { buildSourceItems() }
			.combine(sourceAvailabilityRepository.observeAvailability()) { sources, availability ->
				sources.map { source ->
					source.copy(
						testAvailability = availability[source.id] ?: ContentSourceAvailability.UNKNOWN,
					)
				}
			}
	}

	private fun observeRuntimeSourceChanges(): Flow<Unit> {
		val apkChanges = mihonExtensionManager.installedExtensions.map { Unit }
		val jarChanges = combine(
			GlobalExtensionManager.mangaSources,
			GlobalExtensionManager.contentSources,
		) { _, _ -> Unit }
		return combine(apkChanges, jarChanges) { _, _ -> Unit }
			.onStart { emit(Unit) }
	}

	private suspend fun buildSourceItems(): List<UnifiedSourceItem> {
		val availableSources = contentSourcesRepository.getAllAvailableSourcesForListing()
		val sourceEntities = database.getSourcesDao().findAll().associateBy { it.source }
		val jsonSummaries = database.getJsonSourceDao().observeAllSummaries().first()
		val jsonById = jsonSummaries.associateBy { it.id }
		val jsonEntities = database.getJsonSourceDao().observeAll().first()
		val jsonEntityById = jsonEntities.associateBy { it.id }
		val sourceMap = LinkedHashMap<String, ContentSource>()
		availableSources.forEach { sourceMap[it.name] = it }
		val installedApkSources = getInstalledApkSources()
		installedApkSources.forEach { sourceMap[it.name] = it }
		jsonSummaries.forEach { sourceMap[it.id] = JsonSourceListSource(it) }

		val items = sourceMap.values
			.map { source ->
				val jsonSummary = jsonById[source.name]
				val jsonEntity = jsonEntityById[source.name]
				val sourceEntity = sourceEntities[source.name]
				source.toUnifiedSourceItem(sourceEntity, jsonSummary, jsonEntity)
			}
			.sortedWith(compareBy({ it.kind.ordinal }, { it.title.lowercase() }))
		Log.d(
			"UnifiedSourceCatalog",
			"buildSourceItems available=${availableSources.size} installedApk=${installedApkSources.size} " +
				"mihonWrapped=${mihonExtensionManager.getMihonMangaSources().size} " +
				"mihonInstalled=${mihonExtensionManager.installedExtensions.value.size} " +
				"mihonItems=${items.count { it.kind == UnifiedSourceKind.MIHON }} total=${items.size}",
		)
		return items
	}

	private fun getInstalledApkSources(): List<ContentSource> {
		return mihonExtensionManager.getMihonMangaSources()
	}

	private fun ContentSource.toUnifiedSourceItem(
		sourceEntity: MangaSourceEntity?,
		jsonSummary: com.mangaverse.app.core.db.entity.JsonSourceSummary?,
		jsonEntity: JsonSourceEntity?,
	): UnifiedSourceItem {
		val kind = resolveKind()
		val packageRef = resolvePackageRef(jsonSummary, jsonEntity)
		val repositoryRef = resolveRepositoryRef(jsonEntity)
		return UnifiedSourceItem(
			id = name,
			kind = kind,
			source = this,
			title = getTitle(localizedContext),
			language = resolveLanguage(),
			contentType = getContentType(),
			repositoryId = repositoryRef?.id,
			repositoryName = repositoryRef?.title,
			packageId = packageRef?.first,
			packageName = packageRef?.second,
			isEnabled = jsonSummary?.enabled ?: (settings.isAllSourcesEnabled || sourceEntity?.isEnabled == true),
			isPinned = jsonSummary?.isPinned ?: (sourceEntity?.isPinned == true),
			isAvailable = true,
			isInstalled = kind != UnifiedSourceKind.NATIVE,
			isNsfw = isNsfw(),
			isBroken = isBroken,
		)
	}

	private fun ContentSource.resolveLanguage(): String? {
		val rawLanguage = getLocale()?.language
			?: locale.takeIf { it.isNotBlank() }
		return rawLanguage?.normalizeExtensionLanguageCode()
	}

	private fun ContentSource.resolveKind(): UnifiedSourceKind {
		return when (this) {
			is JsonContentSource -> entity.type.toUnifiedKind()
			is JsonSourceListSource -> when {
				name.startsWith("JSON_LEGADO_") || name.startsWith("JSON_LEGADO_M_") -> UnifiedSourceKind.LEGADO
				name.startsWith("JSON_JS_") -> UnifiedSourceKind.JS
				else -> UnifiedSourceKind.LEGADO
			}
			is MihonMangaSource -> UnifiedSourceKind.MIHON
			is PluginContentSource -> UnifiedSourceKind.JAR
			is KotatsuParserSource -> if (delegate is PluginMangaSource) UnifiedSourceKind.JAR else UnifiedSourceKind.NATIVE
			else -> UnifiedSourceKind.NATIVE
		}
	}

	private fun ContentSource.resolvePackageRef(
		jsonSummary: com.mangaverse.app.core.db.entity.JsonSourceSummary?,
		jsonEntity: JsonSourceEntity?,
	): Pair<String, String>? {
		return when (this) {
			is MihonMangaSource -> packageId(UnifiedSourceKind.MIHON, pkgName) to pkgName
			is PluginContentSource -> {
				val packageName = jarName.removeSuffix(".jar")
				packageId(UnifiedSourceKind.JAR, packageName) to packageName
			}
			is KotatsuParserSource -> {
				val pluginSource = delegate as? PluginMangaSource ?: return null
				val packageName = pluginSource.jarName.removeSuffix(".jar")
				packageId(UnifiedSourceKind.JAR, packageName) to packageName
			}
			is JsonContentSource -> entity.jsonPackageRef()
			is JsonSourceListSource -> jsonEntity?.jsonPackageRef() ?: jsonSummary?.jsonPackageRef()
			else -> null
		}
	}

	private fun ContentSource.resolveRepositoryRef(jsonEntity: JsonSourceEntity?): JsonRepositoryRef? {
		return when (this) {
			is JsonContentSource -> entity.jsonRepositoryRef()
			is JsonSourceListSource -> jsonEntity?.jsonRepositoryRef()
			else -> null
		}
	}

	private fun com.mangaverse.app.core.db.entity.JsonSourceSummary.jsonPackageRef(): Pair<String, String>? {
		return when (type) {
			JsonSourceType.LEGADO -> packageId(UnifiedSourceKind.LEGADO, "imported") to "Imported Legado JSON"
			JsonSourceType.JS -> packageId(UnifiedSourceKind.JS, id) to name
		}
	}

	private fun JsonSourceEntity.jsonRepositoryItem(): UnifiedSourceRepositoryItem? {
		val ref = jsonRepositoryRef() ?: return null
		return UnifiedSourceRepositoryItem(
			id = ref.id,
			kind = ref.kind,
			name = ref.title,
			url = ref.locator,
			locationType = resolveLocationType(ref.locator),
			website = ref.locator,
			isConfigured = true,
			isPreset = false,
			capabilities = setOf(
				UnifiedRepositoryCapability.REFRESH,
				UnifiedRepositoryCapability.IMPORT_JSON_LIST,
			),
		)
	}

	private fun JsonSourceType.toUnifiedKind(): UnifiedSourceKind {
		return when (this) {
			JsonSourceType.LEGADO -> UnifiedSourceKind.LEGADO
			JsonSourceType.JS -> UnifiedSourceKind.JS
		}
	}

	private fun ExternalExtensionRepo.toUnifiedRepositoryItem(isPreset: Boolean): UnifiedSourceRepositoryItem {
		val kind = type.toUnifiedKind()
		val repositoryUrl = when (type) {
			ExternalExtensionType.MIHON -> if (baseUrl.endsWith("/index.pb", ignoreCase = true)) {
				baseUrl
			} else {
				"$baseUrl/index.min.json"
			}
			else -> "$baseUrl/index.min.json"
		}
		return UnifiedSourceRepositoryItem(
			id = repositoryId(kind, baseUrl),
			kind = kind,
			name = displayName,
			url = repositoryUrl,
			locationType = UnifiedRepositoryLocationType.REMOTE_URL,
			website = website,
			isConfigured = true,
			isPreset = isPreset,
			capabilities = type.repositoryCapabilities(),
			version = version,
			lastSuccessAt = lastSuccessAt,
			lastError = lastError,
		)
	}

	private fun ExternalExtensionType.toUnifiedKind(): UnifiedSourceKind {
		return when (this) {
			ExternalExtensionType.MIHON -> UnifiedSourceKind.MIHON
			ExternalExtensionType.JAR -> UnifiedSourceKind.JAR
		}
	}

	private fun ExternalExtensionType.repositoryCapabilities(): Set<UnifiedRepositoryCapability> {
		val base = setOf(
			UnifiedRepositoryCapability.REFRESH,
			UnifiedRepositoryCapability.VERSIONED_INDEX,
			UnifiedRepositoryCapability.INSTALL_PACKAGE,
		)
		return if (this == ExternalExtensionType.JAR) {
			base
		} else {
			base + UnifiedRepositoryCapability.TRUST_FINGERPRINT
		}
	}

	private fun List<UnifiedSourceRepositoryItem>.withPresetRepositories(): List<UnifiedSourceRepositoryItem> {
		val configuredWithPresetFlag = map { item ->
			val matchedPreset = UnifiedRecommendedRepositories.all.firstOrNull { preset ->
				preset.kind == item.kind && normalizeRepositoryUrl(preset.url) == normalizeRepositoryUrl(item.url)
			}
			if (matchedPreset != null) {
				item.copy(isPreset = true, url = matchedPreset.url)
			} else {
				item
			}
		}.distinctBy { it.id }
		val configuredIds = configuredWithPresetFlag.mapTo(mutableSetOf()) { it.id }
		val missingPresets = UnifiedRecommendedRepositories.all
			.filter { preset -> repositoryId(preset.kind, preset.url) !in configuredIds }
			.map { preset ->
				UnifiedSourceRepositoryItem(
					id = repositoryId(preset.kind, preset.url),
					kind = preset.kind,
					name = preset.name,
					url = preset.url,
					locationType = preset.locationType,
					website = preset.url,
					isConfigured = false,
					isPreset = true,
					capabilities = preset.capabilities,
				)
			}
		return (configuredWithPresetFlag + missingPresets)
			.sortedWith(compareBy({ it.kind.ordinal }, { !it.isConfigured }, { it.name.lowercase() }))
	}

	private fun List<JsonSourceEntity>.toJsonRepositoryItems(): List<UnifiedSourceRepositoryItem> {
		return asSequence()
			.filter { it.type == JsonSourceType.LEGADO }
			.mapNotNull { entity -> entity.jsonRepositoryItem() }
			.distinctBy { it.id }
			.toList()
	}

	private fun List<JsonSourceEntity>.toJsonPackageItems(): List<UnifiedSourcePackageItem> {
		val result = mutableListOf<UnifiedSourcePackageItem>()
		val legado = filter { it.type == JsonSourceType.LEGADO }
		if (legado.isNotEmpty()) {
			result += legado.toGroupedJsonPackageItems(
				kind = UnifiedSourceKind.LEGADO,
				fallbackPackageKey = "imported",
				fallbackPackageName = "Imported Legado JSON",
			)
		}

		filter { it.type == JsonSourceType.JS }.forEach { entity ->
			result += UnifiedSourcePackageItem(
				id = packageId(UnifiedSourceKind.JS, entity.id),
				kind = UnifiedSourceKind.JS,
				name = entity.name,
				packageName = entity.id,
				repositoryId = null,
				repositoryName = null,
				versionName = null,
				versionCode = null,
				language = null,
				isInstalled = true,
				isNsfw = false,
				sourceCount = 1,
				sourceNames = listOf(entity.name),
				iconUrl = null,
			)
		}

		return result
	}

	private fun List<JsonSourceEntity>.toGroupedJsonPackageItems(
		kind: UnifiedSourceKind,
		fallbackPackageKey: String,
		fallbackPackageName: String,
	): List<UnifiedSourcePackageItem> {
		return groupBy { entity ->
			entity.jsonPackageRef() ?: (packageId(kind, fallbackPackageKey) to fallbackPackageName)
		}.map { (packageRef, sources) ->
			val repositoryRef = sources.firstNotNullOfOrNull { source -> source.jsonRepositoryRef() }
			UnifiedSourcePackageItem(
				id = packageRef.first,
				kind = kind,
				name = packageRef.second,
				packageName = null,
				repositoryId = repositoryRef?.id,
				repositoryName = repositoryRef?.title,
				versionName = null,
				versionCode = null,
				language = null,
				isInstalled = true,
				isNsfw = false,
				sourceCount = sources.size,
				sourceNames = sources.map { it.name }.sorted(),
				iconUrl = sources.firstNotNullOfOrNull { it.iconUrl },
			)
		}
	}

	private fun JsonSourceEntity.jsonPackageRef(): Pair<String, String>? {
		return when (type) {
			JsonSourceType.LEGADO -> {
				val repoRef = jsonRepositoryRef()
				packageId(UnifiedSourceKind.LEGADO, repoRef?.id ?: "imported") to (repoRef?.title ?: "Imported Legado JSON")
			}
			JsonSourceType.JS -> packageId(UnifiedSourceKind.JS, id) to name
		}
	}

	private fun JsonSourceEntity.jsonRepositoryRef(): JsonRepositoryRef? {
		return when (type) {
			JsonSourceType.LEGADO -> {
				val metadata = JsonSourceImportMetadata.parse(config) ?: return null
				val locator = metadata.sourceLocator?.trim()?.takeIf { it.isNotBlank() } ?: return null
				val title = metadata.sourceTitle?.trim()?.takeIf { it.isNotBlank() }
					?: repositoryTitleFromUrl(locator, fallback = "Legado")
				JsonRepositoryRef(
					id = repositoryId(UnifiedSourceKind.LEGADO, locator),
					kind = UnifiedSourceKind.LEGADO,
					locator = locator,
					title = title,
				)
			}
			JsonSourceType.JS -> null
		}
	}

	private fun repositoryTitleFromUrl(url: String, fallback: String): String {
		val uri = runCatching { Uri.parse(url) }.getOrNull()
		val host = uri?.host?.trim().orEmpty()
		val tail = uri?.lastPathSegment?.trim().orEmpty()
		return when {
			host.isNotBlank() && tail.isNotBlank() -> "$host / $tail"
			host.isNotBlank() -> host
			tail.isNotBlank() -> tail
			else -> fallback
		}
	}

	private data class JsonRepositoryRef(
		val id: String,
		val kind: UnifiedSourceKind,
		val locator: String,
		val title: String,
	)
}

private fun repositoryId(kind: UnifiedSourceKind, url: String): String {
	return "repo:${kind.name}:${normalizeRepositoryUrl(url)}"
}

private fun packageId(kind: UnifiedSourceKind, value: String): String {
	return "package:${kind.name}:${value.trim()}"
}

private fun normalizeRepositoryUrl(url: String): String {
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

private fun resolveLocationType(locator: String): UnifiedRepositoryLocationType {
	return when {
		locator.startsWith("content://", ignoreCase = true) -> UnifiedRepositoryLocationType.LOCAL_FILE
		locator.startsWith("file://", ignoreCase = true) -> UnifiedRepositoryLocationType.LOCAL_FILE
		locator.startsWith("http://", ignoreCase = true) -> UnifiedRepositoryLocationType.REMOTE_URL
		locator.startsWith("https://", ignoreCase = true) -> UnifiedRepositoryLocationType.REMOTE_URL
		else -> UnifiedRepositoryLocationType.INLINE_IMPORT
	}
}
