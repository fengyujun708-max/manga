package com.mangaverse.app.local.data.importer

import com.mangaverse.app.local.data.hasZipExtension

enum class LocalImportKind {
	MANGA,
}

internal object LocalImportSupport {

	fun supportsFileName(fileName: String): Boolean {
		return hasZipExtension(fileName) || classifyFileName(fileName) != LocalImportKind.MANGA
	}

	fun classifyFileName(fileName: String): LocalImportKind {
		return LocalImportKind.MANGA
	}

	fun contentFolderName(fileName: String): String {
		return fileName.substringBeforeLast('.').ifBlank { fileName }
	}
}
