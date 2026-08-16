package com.mangaverse.app.settings.sources.unified

import com.mangaverse.app.extensions.repo.ExternalExtensionType

object UnifiedRecommendedRepositories {
	const val UMA_REPOSITORY_URL = "https://github.com/InvalidDavid/UMA"

	private val extensionRepoCapabilities = setOf(
		UnifiedRepositoryCapability.REFRESH,
		UnifiedRepositoryCapability.VERSIONED_INDEX,
		UnifiedRepositoryCapability.INSTALL_PACKAGE,
		UnifiedRepositoryCapability.TRUST_FINGERPRINT,
	)

	private val jarRepoCapabilities = setOf(
		UnifiedRepositoryCapability.REFRESH,
		UnifiedRepositoryCapability.VERSIONED_INDEX,
		UnifiedRepositoryCapability.INSTALL_PACKAGE,
	)

	private val jsonRepoCapabilities = setOf(
		UnifiedRepositoryCapability.REFRESH,
		UnifiedRepositoryCapability.IMPORT_JSON_LIST,
	)

	val all: List<UnifiedRecommendedRepository> = listOf(
			UnifiedRecommendedRepository(
				kind = UnifiedSourceKind.JAR,
				name = "Kototoro Parsers",
				url = "https://raw.githubusercontent.com/skepsun/kototoro-parsers/repo/index.min.json",
				locationType = UnifiedRepositoryLocationType.REMOTE_URL,
				capabilities = jarRepoCapabilities,
			),
			UnifiedRecommendedRepository(
				kind = UnifiedSourceKind.JAR,
				name = "Yakateam Parsers",
				url = "https://raw.githubusercontent.com/skepsun/k-parsers-y/repo/index.min.json",
				locationType = UnifiedRepositoryLocationType.REMOTE_URL,
				capabilities = jarRepoCapabilities,
			),
			UnifiedRecommendedRepository(
				kind = UnifiedSourceKind.JAR,
				name = "Redo Parsers",
				url = "https://raw.githubusercontent.com/skepsun/k-parsers-r/repo/index.min.json",
				locationType = UnifiedRepositoryLocationType.REMOTE_URL,
				capabilities = jarRepoCapabilities,
			),
			UnifiedRecommendedRepository(
				kind = UnifiedSourceKind.JAR,
				name = "UMA (Tsuki Parsers)",
				url = UMA_REPOSITORY_URL,
				locationType = UnifiedRepositoryLocationType.REMOTE_URL,
				capabilities = jarRepoCapabilities,
				note = "Loaded from the latest GitHub release; requires the Tsuki compatibility layer",
			),
		UnifiedRecommendedRepository(
			kind = UnifiedSourceKind.MIHON,
			name = "Keiyoushi",
			url = "https://github.com/keiyoushi/extensions/raw/repo/index.pb",
			locationType = UnifiedRepositoryLocationType.REMOTE_URL,
			capabilities = extensionRepoCapabilities,
		),
		UnifiedRecommendedRepository(
			kind = UnifiedSourceKind.MIHON,
			name = "Yuzono Manga Repo",
			url = "https://raw.githubusercontent.com/yuzono/manga-repo/repo/index.min.json",
			locationType = UnifiedRepositoryLocationType.REMOTE_URL,
			capabilities = extensionRepoCapabilities,
		),
		UnifiedRecommendedRepository(
			kind = UnifiedSourceKind.MIHON,
			name = "CopyManga Copy20",
			url = "https://raw.githubusercontent.com/LittleSurvival/copymanga-copy20/repo/index.min.json",
			locationType = UnifiedRepositoryLocationType.REMOTE_URL,
			capabilities = extensionRepoCapabilities,
			note = "Chinese-site coverage",
		),
		UnifiedRecommendedRepository(
			kind = UnifiedSourceKind.LEGADO,
			name = "XIU2 Yuedu",
			url = "https://cdn.jsdmirror.com/gh/XIU2/Yuedu/shuyuan",
			locationType = UnifiedRepositoryLocationType.REMOTE_URL,
			capabilities = jsonRepoCapabilities,
			note = "Legado sources usually cannot expose reliable per-source versions",
		),
	)

	fun byKind(kind: UnifiedSourceKind): List<UnifiedRecommendedRepository> {
		return all.filter { it.kind == kind }
	}

	fun byExternalType(type: ExternalExtensionType): List<UnifiedRecommendedRepository> {
		return byKind(
			when (type) {
				ExternalExtensionType.MIHON -> UnifiedSourceKind.MIHON
				ExternalExtensionType.JAR -> UnifiedSourceKind.JAR
			},
		)
	}
}
