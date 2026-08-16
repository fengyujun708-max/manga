package com.mangaverse.app.local.data.output

import androidx.core.net.toFile
import androidx.core.net.toUri
import com.mangaverse.app.core.model.isLocal
import com.mangaverse.app.parsers.model.Content
import java.io.File

class LocalContentUtil(
	private val manga: Content,
	private val file: File,
) {

	suspend fun deleteChapters(ids: Set<Long>) {
		if (file.isDirectory) {
			LocalContentDirOutput(file, manga).use { output ->
				output.deleteChapters(ids)
				output.finish()
			}
		} else {
			LocalContentZipOutput.filterChapters(file, manga, ids)
		}
	}
}
