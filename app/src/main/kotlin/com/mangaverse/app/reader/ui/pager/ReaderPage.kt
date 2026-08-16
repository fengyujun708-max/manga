package com.mangaverse.app.reader.ui.pager

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlinx.parcelize.RawValue
import com.mangaverse.app.core.model.parcelable.ContentSourceParceler
import com.mangaverse.app.parsers.model.ContentPage
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.util.longHashCode

enum class ReaderPageSplit {
	NONE, LEFT, RIGHT
}

@Parcelize
@TypeParceler<ContentSource, ContentSourceParceler>
data class ReaderPage(
	val id: Long,
	val url: String,
	val preview: String?,
	val headers: @RawValue Map<String, String>?,
	val chapterId: Long,
	val index: Int,
	val source: ContentSource,
	val split: ReaderPageSplit = ReaderPageSplit.NONE,
	val reloadNonce: Long = 0L,
) : Parcelable {

	val readerKey: Long
		get() = "$chapterId#$index#$url#${split.name}#$reloadNonce".longHashCode()

	constructor(page: ContentPage, index: Int, chapterId: Long) : this(
		id = page.id,
		url = page.url,
		preview = page.preview,
		headers = page.headers,
		chapterId = chapterId,
		index = index,
		source = page.source,
		reloadNonce = 0L,
	)

	fun toContentPage() = ContentPage(
		id = id,
		url = url,
		preview = preview,
		headers = headers,
		source = source,
	)
}
