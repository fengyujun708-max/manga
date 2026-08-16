package com.mangaverse.app.settings.sources.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.mangaverse.app.extensions.repo.ExternalExtensionType
import com.mangaverse.app.extensions.repo.RepoAvailableExtension
import com.mangaverse.app.mihon.MihonExtensionManager

internal data class InstalledExtensionVersionInfo(
	val versionCode: Long,
	val libVersion: Double,
	val versionName: String,
)

private inline fun <T> Flow<List<T>>.mapInstalledExtensionInfo(
	crossinline packageNameOf: (T) -> String,
	crossinline versionCodeOf: (T) -> Long,
	crossinline libVersionOf: (T) -> Double,
	crossinline versionNameOf: (T) -> String,
): Flow<Map<String, InstalledExtensionVersionInfo>> {
	return map { list ->
		list.associate { ext ->
			packageNameOf(ext) to InstalledExtensionVersionInfo(
				versionCode = versionCodeOf(ext),
				libVersion = libVersionOf(ext),
				versionName = versionNameOf(ext),
			)
		}
	}
}

private inline fun <T> Flow<List<T>>.mapInstalledExtensionEntries(
	crossinline packageNameOf: (T) -> String,
	crossinline appNameOf: (T) -> String,
	crossinline versionNameOf: (T) -> String,
	crossinline versionCodeOf: (T) -> Long,
	crossinline libVersionOf: (T) -> Double,
	crossinline languageOf: (T) -> String,
	crossinline isNsfwOf: (T) -> Boolean,
	crossinline sourceNamesOf: (T) -> List<String>,
): Flow<List<InstalledExtensionEntry>> {
	return map { list ->
		list.map { ext ->
			InstalledExtensionEntry(
				pkgName = packageNameOf(ext),
				name = appNameOf(ext),
				versionName = versionNameOf(ext),
				versionCode = versionCodeOf(ext),
				libVersion = libVersionOf(ext),
				lang = languageOf(ext),
				isNsfw = isNsfwOf(ext),
				sourceNames = sourceNamesOf(ext),
			)
		}
	}
}

internal fun observeInstalledExtensionInfoMap(
	type: ExternalExtensionType,
	mihonExtensionManager: MihonExtensionManager,
): Flow<Map<String, InstalledExtensionVersionInfo>> {
	return when (type) {
		ExternalExtensionType.MIHON -> mihonExtensionManager.installedExtensions.mapInstalledExtensionInfo(
			packageNameOf = { it.pkgName },
			versionCodeOf = { it.versionCode },
			libVersionOf = { it.libVersion },
			versionNameOf = { it.versionName },
		)

		ExternalExtensionType.JAR -> flowOf(emptyMap()) // Unused by non-APK ecosystems right now
	}
}

internal fun observeInstalledExtensionEntries(
	type: ExternalExtensionType,
	mihonExtensionManager: MihonExtensionManager,
	context: android.content.Context? = null,
): Flow<List<InstalledExtensionEntry>> {
	return when (type) {
		ExternalExtensionType.MIHON -> observeMihonInstalledExtensionEntries(mihonExtensionManager)

		ExternalExtensionType.JAR -> {
			val prefs = context?.getSharedPreferences("jar_plugin_versions", android.content.Context.MODE_PRIVATE)
			combine(
				com.mangaverse.app.core.extensions.GlobalExtensionManager.mangaSources,
				com.mangaverse.app.core.extensions.GlobalExtensionManager.contentSources
			) { manga, content ->
				val allJarNames = (manga.map { it.jarName } + content.map { it.jarName }).distinct()
				allJarNames.map { jarName ->
					val pkg = jarName.removeSuffix(".jar")
					val version = prefs?.getLong(pkg, 1L) ?: 1L
					InstalledExtensionEntry(
						pkgName = pkg,
						name = pkg,
						versionName = "1.0",
						versionCode = version,
						libVersion = 1.0,
						lang = "all",
						isNsfw = false,
						sourceNames = emptyList(),
					)
				}
			}
		}
	}
}

internal fun observeMihonInstalledExtensionEntries(
	mihonExtensionManager: MihonExtensionManager,
): Flow<List<InstalledExtensionEntry>> {
	return mihonExtensionManager.installedExtensions.mapInstalledExtensionEntries(
		packageNameOf = { it.pkgName },
		appNameOf = { it.appName.removePrefix("Tachiyomi: ") },
		versionNameOf = { it.versionName },
		versionCodeOf = { it.versionCode },
		libVersionOf = { it.libVersion },
		languageOf = { it.lang },
		isNsfwOf = { it.isNsfw },
		sourceNamesOf = { ext -> ext.sources.map { it.name } },
	)
}

internal fun RepoAvailableExtension.resolveAvailableState(
	installedInfo: InstalledExtensionVersionInfo?,
	isInstalling: Boolean,
): AvailableExtensionState {
	return when {
		isInstalling -> AvailableExtensionState.INSTALLING
		installedInfo == null -> AvailableExtensionState.AVAILABLE
		isNewerThanInstalled(installedInfo.versionCode) -> AvailableExtensionState.UPDATE_AVAILABLE
		else -> AvailableExtensionState.INSTALLED
	}
}
