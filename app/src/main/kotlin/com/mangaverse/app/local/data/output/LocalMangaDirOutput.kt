package com.mangaverse.app.local.data.output

import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.internal.closeQuietly
import com.mangaverse.app.core.model.isLocal
import com.mangaverse.app.core.util.MimeTypes
import com.mangaverse.app.core.util.ext.MimeType
import com.mangaverse.app.core.util.ext.deleteAwait
import com.mangaverse.app.core.util.ext.takeIfReadable
import com.mangaverse.app.core.util.ext.toFileNameSafe
import com.mangaverse.app.core.util.ext.toFileOrNull
import com.mangaverse.app.core.zip.ZipOutput
import com.mangaverse.app.local.data.ContentIndex
import com.mangaverse.app.local.data.input.LocalContentParser
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.util.nullIfEmpty
import java.io.File

class LocalContentDirOutput(
	rootFile: File,
	manga: Content,
) : LocalContentOutput(rootFile) {

	val chaptersOutput = HashMap<ContentChapter, ZipOutput>()
	val index = ContentIndex(File(rootFile, ENTRY_NAME_INDEX).takeIfReadable()?.readText())
	private val mutex = Mutex()

	init {
		if (!manga.isLocal) {
			index.setContentInfo(manga)
		}
	}

	override suspend fun mergeWithExisting() = Unit

	override suspend fun addCover(file: File, type: MimeType?) = mutex.withLock {
		val name = buildString {
			append("cover")
			MimeTypes.getExtension(type)?.let { ext ->
				append('.')
				append(ext)
			}
		}
		runInterruptible(Dispatchers.IO) {
			// Ensure rootFile directory exists
			if (!rootFile.exists()) {
				rootFile.mkdirs()
			}
			file.copyTo(File(rootFile, name), overwrite = true)
		}
		index.setCoverEntry(name)
		flushIndex()
	}

	override suspend fun addPage(chapter: IndexedValue<ContentChapter>, file: File, pageNumber: Int, type: MimeType?) =
		mutex.withLock {
			val output = chaptersOutput.getOrPut(chapter.value) {
				// Ensure rootFile directory exists before creating chapter CBZ
				if (!rootFile.exists()) {
					rootFile.mkdirs()
				}
				ZipOutput(File(rootFile, chapterFileName(chapter) + SUFFIX_TMP))
			}
			val name = buildString {
				append(FILENAME_PATTERN.format(chapter.value.branch.hashCode(), chapter.index + 1, pageNumber))
				MimeTypes.getExtension(type)?.let { ext ->
					append('.')
					append(ext)
				}
			}
			runInterruptible(Dispatchers.IO) {
				output.put(name, file)
			}
			index.addChapter(chapter, chapterFileName(chapter), null)
		}

	override suspend fun putChapterImages(chapterId: Long, remoteImages: Map<String, String>) =
		mutex.withLock {
			index.putChapterImages(chapterId, remoteImages)
			flushIndex()
		}

	override suspend fun flushChapter(chapter: ContentChapter): Boolean = mutex.withLock {
		val output = chaptersOutput.remove(chapter) ?: return@withLock false
		output.flushAndFinish()
		flushIndex()
		true
	}

	override suspend fun finish() = mutex.withLock {
		flushIndex()
		for (output in chaptersOutput.values) {
			output.flushAndFinish()
		}
		chaptersOutput.clear()
	}

	override suspend fun cleanup() = mutex.withLock {
		for (output in chaptersOutput.values) {
			output.file.deleteAwait()
		}
	}

	override fun close() {
		for (output in chaptersOutput.values) {
			output.closeQuietly()
		}
	}

	suspend fun deleteChapters(ids: Set<Long>) = mutex.withLock {
		val chapters = checkNotNull(
			(index.getContentInfo() ?: LocalContentParser(rootFile).getContent(withDetails = true).manga).chapters,
		) {
			"No chapters found"
		}.withIndex()
		val victimsIds = ids.toMutableSet()
		for (chapter in chapters) {
			if (chapter.value.id !in victimsIds) {
				continue
			}
			val chapterFile = index.getChapterFileName(chapter.value.id)?.let {
				File(rootFile, it)
			} ?: chapter.value.url?.toUri()?.toFileOrNull()
			if (chapterFile == null) {
				// A cancelled download can leave remote chapter metadata without a completed local file.
				victimsIds.remove(chapter.value.id)
				continue
			}
			val contentRoot = rootFile.canonicalFile.toPath()
			val chapterPath = chapterFile.canonicalFile.toPath()
			check(chapterFile.exists() && chapterPath != contentRoot && chapterPath.startsWith(contentRoot)) {
				"Chapter file is missing or outside the content directory: $chapterFile"
			}
			chapterFile.deleteAwait()
			index.removeChapter(chapter.value.id)
			victimsIds.remove(chapter.value.id)
		}
		check(victimsIds.isEmpty()) {
			"${victimsIds.size} of ${ids.size} chapters was not removed: not found"
		}
	}

	fun setIndex(newIndex: ContentIndex) {
		index.setFrom(newIndex)
	}

	private suspend fun ZipOutput.flushAndFinish() = runInterruptible(Dispatchers.IO) {
		val e: Throwable? = try {
			finish()
			null
		} catch (e: Throwable) {
			e
		} finally {
			close()
		}
		if (e == null) {
			val resFile = File(file.absolutePath.removeSuffix(SUFFIX_TMP))
			file.renameTo(resFile)
		} else {
			file.delete()
			throw e
		}
	}

	private fun chapterFileName(chapter: IndexedValue<ContentChapter>): String {
		index.getChapterFileName(chapter.value.id)?.let {
			return it
		}
		val baseName = buildString {
			append(chapter.index)
			chapter.value.title?.nullIfEmpty()?.let {
				append('_')
				append(it.toFileNameSafe())
			}
			if (length > 32) {
				deleteRange(31, lastIndex)
			}
		}
		var i = 0
		while (true) {
			val name = (if (i == 0) baseName else baseName + "_$i") + ".cbz"
			if (!File(rootFile, name).exists()) {
				return name
			}
			i++
		}
	}

	private suspend fun flushIndex() = runInterruptible(Dispatchers.IO) {
		File(rootFile, ENTRY_NAME_INDEX).writeText(index.toString())
	}


	companion object {

		private const val FILENAME_PATTERN = "%08d_%04d%04d"
	}
}
