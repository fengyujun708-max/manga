package com.mangaverse.app.settings.sources.extensions

import com.mangaverse.app.extensions.repo.RepoAvailableExtension

internal fun RepoAvailableExtension.isNewerThanInstalled(installedVersionCode: Long?): Boolean {
	return installedVersionCode != null && versionCode > installedVersionCode
}
