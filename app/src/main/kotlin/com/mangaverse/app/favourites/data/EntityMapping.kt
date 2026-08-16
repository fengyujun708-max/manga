package com.mangaverse.app.favourites.data

import com.mangaverse.app.core.db.entity.toContent
import com.mangaverse.app.core.db.entity.toContentTags
import com.mangaverse.app.core.model.FavouriteCategory
import com.mangaverse.app.list.domain.ListSortOrder
import java.time.Instant

fun FavouriteCategoryEntity.toFavouriteCategory(id: Long = categoryId.toLong()) = FavouriteCategory(
	id = id,
	title = title,
	sortKey = sortKey,
	order = ListSortOrder(order, ListSortOrder.NEWEST),
	createdAt = Instant.ofEpochMilli(createdAt),
	isTrackingEnabled = track,
	isVisibleInLibrary = isVisibleInLibrary,
)

fun FavouriteContent.toContent() = manga.toContent(tags.toContentTags(), null)

fun Collection<FavouriteContent>.toContentList() = map { it.toContent() }
