package com.mangaverse.app.search.domain

import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import com.mangaverse.app.R
import com.mangaverse.app.core.jsonsource.SourceType
import com.mangaverse.app.explore.ui.model.SourceTag

val ALL_SOURCE_TYPES: Set<SourceType> = setOf(
	SourceType.NATIVE,
	SourceType.JSON_LEGADO,
	SourceType.MIHON,
)

data class SourceTypeOption(
	val type: SourceType,
	@StringRes val titleRes: Int,
	@DrawableRes val iconRes: Int,
)

val SOURCE_TYPE_OPTIONS: List<SourceTypeOption> = listOf(
	SourceTypeOption(SourceType.NATIVE, R.string.source_type_native, R.drawable.ic_source_builtin),
	SourceTypeOption(SourceType.MIHON, R.string.source_type_mihon, R.drawable.ic_source_mihon),
	SourceTypeOption(SourceType.JSON_LEGADO, R.string.source_type_legado, R.drawable.ic_source_legado),
)

fun sourceTypesFromTags(tags: Set<SourceTag>): Set<SourceType> {
	if (tags.isEmpty()) {
		return ALL_SOURCE_TYPES
	}
	val result = mutableSetOf<SourceType>()
	tags.forEach { tag ->
		when (tag) {
			SourceTag.BUILTIN -> result.add(SourceType.NATIVE)
			SourceTag.MIHON -> result.add(SourceType.MIHON)
			SourceTag.LEGADO -> result.add(SourceType.JSON_LEGADO)
			SourceTag.PINNED -> result.addAll(ALL_SOURCE_TYPES)
		}
	}
	return if (result.isEmpty()) ALL_SOURCE_TYPES else result
}

fun sourceTypesFromNames(names: Collection<String>?): Set<SourceType>? {
	if (names.isNullOrEmpty()) return null
	val types = names.mapNotNull { name ->
		runCatching { SourceType.valueOf(name) }.getOrNull()
	}.toSet()
	val filtered = types.intersect(ALL_SOURCE_TYPES)
	return filtered.ifEmpty { null }
}

fun sourceTypesToNames(types: Set<SourceType>): ArrayList<String> {
	return ArrayList(types.map { it.name })
}
