package com.mangaverse.app.core.parser

import com.mangaverse.app.core.model.UnknownContentSource
import com.mangaverse.app.mihon.MihonExtensionManager
import com.mangaverse.app.mihon.model.MihonMangaSource
import com.mangaverse.app.parsers.model.ContentSource
import javax.inject.Inject

class MihonContentSourceResolver @Inject constructor(
	private val mihonExtensionManager: MihonExtensionManager,
) : ContentSourceResolver {

	override fun supports(source: ContentSource): Boolean {
		return source !is MihonMangaSource && source != UnknownContentSource && (
			source.name.startsWith(MIHON_PREFIX) ||
				findByDisplayName(source.name) != null
			)
	}

	override fun resolve(source: ContentSource): ContentSource? {
		if (!supports(source)) {
			return null
		}
		android.util.Log.d("MihonResolver", "Resolving source: ${source.name}")
		val resolved = if (source.name.startsWith(MIHON_PREFIX)) {
			mihonExtensionManager.getMihonMangaSourceByName(source.name)
		} else {
			findByDisplayName(source.name)
		}
		android.util.Log.d("MihonResolver", "Resolved result: $resolved")
		return resolved
	}

	private fun findByDisplayName(name: String): MihonMangaSource? {
		return mihonExtensionManager.getMihonMangaSources()
			.singleOrNull { source -> source.displayName == name }
	}

	private companion object {
		private const val MIHON_PREFIX = "MIHON_"
	}
}
