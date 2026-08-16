package com.mangaverse.app.settings.sources.extensions

import com.mangaverse.app.extensions.repo.ExternalExtensionType

internal fun ExternalExtensionType.normalizePackageNameForMatching(packageName: String): String {
	return packageName
}
