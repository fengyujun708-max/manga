package com.mangaverse.app.core.db.entity

import com.mangaverse.app.core.model.ContentSource
import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.core.model.resolvedContentTypeForSnapshot
import com.mangaverse.app.parsers.model.ContentRating
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.model.ContentState
import com.mangaverse.app.parsers.model.ContentTag
import com.mangaverse.app.parsers.model.SortOrder
import com.mangaverse.app.parsers.util.longHashCode
import com.mangaverse.app.parsers.util.mapToSet
import com.mangaverse.app.parsers.util.nullIfEmpty
import com.mangaverse.app.parsers.util.toArraySet
import com.mangaverse.app.parsers.util.toTitleCase

private const val VALUES_DIVIDER = '\n'

// Entity to model

fun TagEntity.toContentTag() = ContentTag(
	key = this.key,
	title = this.title.toTitleCase(),
	source = ContentSource(this.source),
)

fun Collection<TagEntity>.toContentTags() = mapToSet(TagEntity::toContentTag)

fun Collection<TagEntity>.toContentTagsList() = map(TagEntity::toContentTag)

fun MangaEntity.toContent(tags: Set<ContentTag>, chapters: List<ChapterEntity>?): Content {
    val persistedSource = ContentSource(this.source)
    val persistedChapters = chapters?.toContentChapters()
    return Content(
        id = this.id,
        title = this.title,
        altTitles = this.altTitles?.split(VALUES_DIVIDER)?.toArraySet().orEmpty(),
        state = this.state?.let { ContentState(it) },
        rating = this.rating,
        contentRating = ContentRating(this.contentRating)
            ?: if (isNsfw) ContentRating.ADULT else null,
        url = this.url,
        publicUrl = this.publicUrl,
        coverUrl = this.coverUrl,
        largeCoverUrl = this.largeCoverUrl,
        authors = this.authors?.split(VALUES_DIVIDER)?.toArraySet().orEmpty(),
        source = persistedSource,
        tags = tags.mapToSet { it.copy(source = persistedSource) },
        chapters = persistedChapters,
		description = this.description,
		sourceData = this.sourceData,
	)
}
fun MangaWithTags.toContent(chapters: List<ChapterEntity>? = null) = manga.toContent(tags.toContentTags(), chapters)

fun Collection<MangaWithTags>.toContentList() = map { it.toContent() }

fun ChapterEntity.toContentChapter() = ContentChapter(
	id = chapterId,
	title = title.nullIfEmpty(),
	number = number,
	volume = volume,
	url = url,
	scanlator = scanlator,
	uploadDate = uploadDate,
	branch = branch,
		source = ContentSource(source),
		sourceData = sourceData,
	)

fun Collection<ChapterEntity>.toContentChapters() = map { it.toContentChapter() }

// Model to entity

fun Content.toEntity() = MangaEntity(
	id = id,
	url = url,
	publicUrl = publicUrl,
	source = source.name,
	largeCoverUrl = largeCoverUrl,
	coverUrl = coverUrl.orEmpty(),
	altTitles = altTitles.joinToString(VALUES_DIVIDER.toString()),
	rating = rating,
	isNsfw = isNsfw(),
	contentRating = contentRating?.name,
	state = state?.name,
	title = title,
	authors = authors.joinToString(VALUES_DIVIDER.toString()),
	description = description,
	contentType = source.resolvedContentTypeForSnapshot()?.name,
	sourceData = sourceData,
)

fun ContentTag.toEntity() = TagEntity(
	title = title,
	key = key,
	source = source.name,
	id = "${key}_${source.name}".longHashCode(),
	isPinned = false, // for future use
)

fun Collection<ContentTag>.toEntities() = map(ContentTag::toEntity)

fun Iterable<IndexedValue<ContentChapter>>.toEntities(mangaId: Long) = map { (index, chapter) ->
	ChapterEntity(
		chapterId = chapter.id,
		mangaId = mangaId,
		title = chapter.title.orEmpty(),
		number = chapter.number,
		volume = chapter.volume,
		url = chapter.url,
		scanlator = chapter.scanlator,
		uploadDate = chapter.uploadDate,
			branch = chapter.branch,
			source = chapter.source.name,
			index = index,
			sourceData = chapter.sourceData,
		)
}

// Other

fun SortOrder(name: String, fallback: SortOrder): SortOrder = runCatching {
	SortOrder.valueOf(name)
}.getOrDefault(fallback)

fun ContentState(name: String): ContentState? = runCatching {
	ContentState.valueOf(name)
}.getOrNull()

fun ContentRating(name: String?): ContentRating? = runCatching {
	ContentRating.valueOf(name ?: return@runCatching null)
}.getOrNull()
