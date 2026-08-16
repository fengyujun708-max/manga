package com.mangaverse.app.list.domain

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.mangaverse.app.R
import com.mangaverse.app.core.db.entity.toEntity
import com.mangaverse.app.core.model.FavouriteCategory
import com.mangaverse.app.core.model.LocalMangaSource
import com.mangaverse.app.core.model.iconResId
import com.mangaverse.app.core.model.titleResId
import com.mangaverse.app.core.model.unwrap
import com.mangaverse.app.core.parser.external.ExternalContentSource
import com.mangaverse.app.core.parser.favicon.faviconUri
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentState
import com.mangaverse.app.parsers.model.ContentTag
import com.mangaverse.app.core.domain.model.ScrobblingStatus

sealed interface ListFilterOption {

	@get:StringRes
	val titleResId: Int

	@get:DrawableRes
	val iconResId: Int

	val titleText: CharSequence?

	val groupKey: String

	fun getIconData(): Any? = null

	data object Downloaded : ListFilterOption {

		override val titleResId: Int
			get() = R.string.on_device

		override val iconResId: Int
			get() = R.drawable.ic_storage

		override val titleText: CharSequence?
			get() = null

		override val groupKey: String
			get() = "_downloaded"
	}

	enum class Macro(
		@StringRes override val titleResId: Int,
		@DrawableRes override val iconResId: Int,
	) : ListFilterOption {

		COMPLETED(R.string.status_completed, R.drawable.ic_state_finished),
		NEW_CHAPTERS(R.string.new_chapters, R.drawable.ic_updated),
		MULTI_PROJECTION(R.string.filter_multi_projection, R.drawable.ic_list_group),
		BROKEN_PROJECTION(R.string.filter_broken_projection, R.drawable.ic_alert_outline),
		FAVORITE(R.string.favourites, R.drawable.ic_heart_outline),
		NSFW(R.string.nsfw, R.drawable.ic_nsfw),
		;

		override val titleText: CharSequence?
			get() = null

		override val groupKey: String
			get() = name
	}

	data class Branch(
		override val titleText: String?,
		val chaptersCount: Int,
	) : ListFilterOption {

		override val titleResId: Int
			get() = if (titleText == null) R.string.system_default else 0

		override val iconResId: Int
			get() = R.drawable.ic_language

		override val groupKey: String
			get() = "_branch"
	}

	data class Tag(
		val tag: ContentTag
	) : ListFilterOption {

		val tagId: Long = tag.toEntity().id

		override val titleResId: Int
			get() = 0

		override val iconResId: Int
			get() = R.drawable.ic_tag

		override val titleText: String
			get() = tag.title

		override val groupKey: String
			get() = "_tag"
	}

	data class Favorite(
		val category: FavouriteCategory
	) : ListFilterOption {

		override val titleResId: Int
			get() = 0

		override val iconResId: Int
			get() = R.drawable.ic_heart_outline

		override val titleText: String
			get() = category.title

		override val groupKey: String
			get() = "_favcat"
	}

	data class Source(
		val mangaSource: ContentSource
	) : ListFilterOption {
		override val titleResId: Int
			get() = when (mangaSource.unwrap()) {
				is ExternalContentSource -> R.string.external_source
				LocalMangaSource -> R.string.local_storage
				else -> 0
			}

		override val iconResId: Int
			get() = R.drawable.ic_web

		override val titleText: CharSequence?
			get() {
				val unwrapped = mangaSource.unwrap()
				return when (unwrapped) {
					is com.mangaverse.app.core.parser.kotatsu.KotatsuParserSource -> unwrapped.title
					is com.mangaverse.app.mihon.model.MihonMangaSource -> unwrapped.displayName
					is com.mangaverse.app.core.jsonsource.JsonContentSource -> unwrapped.displayName.ifBlank { unwrapped.name }
					is com.mangaverse.app.core.jsonsource.JsonSourceListSource -> unwrapped.displayName.ifBlank { unwrapped.name }
					else -> {
						if (unwrapped.name.startsWith("LOCAL") || unwrapped.name == "TEST") {
							mangaSource.name
						} else {
							val underlying = if (unwrapped is com.mangaverse.app.core.extensions.PluginContentSource) unwrapped.originalSource else unwrapped
							val titleMethod = try { underlying.javaClass.getMethod("getTitle") } catch (_: Exception) { null }
							if (titleMethod != null) {
								(titleMethod.invoke(underlying) as? String)?.takeIf { it.isNotBlank() } ?: mangaSource.name
							} else {
								mangaSource.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
							}
						}
					}
				}
			}

		override val groupKey: String
			get() = "_source"

		override fun getIconData() = mangaSource.faviconUri()
	}

	data class PublicationState(
		val state: ContentState,
	) : ListFilterOption {

		override val titleResId: Int
			get() = state.titleResId

		override val iconResId: Int
			get() = state.iconResId

		override val titleText: CharSequence?
			get() = null

		override val groupKey: String
			get() = "_publication_state"
	}

	data class ReadingStatus(
		val status: ScrobblingStatus,
	) : ListFilterOption {

		override val titleResId: Int
			get() = when (status) {
				ScrobblingStatus.PLANNED -> R.string.reading_status_planned
				ScrobblingStatus.READING -> R.string.reading_status_reading
				ScrobblingStatus.RE_READING -> R.string.reading_status_re_reading
				ScrobblingStatus.COMPLETED -> R.string.reading_status_completed
				ScrobblingStatus.ON_HOLD -> R.string.reading_status_on_hold
				ScrobblingStatus.DROPPED -> R.string.reading_status_dropped
			}

		override val iconResId: Int
			get() = when (status) {
				ScrobblingStatus.PLANNED -> R.drawable.ic_bookmark
				ScrobblingStatus.READING -> R.drawable.ic_read
				ScrobblingStatus.RE_READING -> R.drawable.ic_history
				ScrobblingStatus.COMPLETED -> R.drawable.ic_state_finished
				ScrobblingStatus.ON_HOLD -> R.drawable.ic_action_pause
				ScrobblingStatus.DROPPED -> R.drawable.ic_state_abandoned
			}

		override val titleText: CharSequence?
			get() = null

		override val groupKey: String
			get() = "_reading_status"
	}

	data class Inverted(
		val option: ListFilterOption,
		override val iconResId: Int,
		override val titleResId: Int,
		override val titleText: CharSequence?,
	) : ListFilterOption {

		override val groupKey: String
			get() = "_inv" + option.groupKey
	}

	companion object {

		val SFW
			get() = Inverted(
				option = Macro.NSFW,
				iconResId = R.drawable.ic_sfw,
				titleResId = R.string.sfw,
				titleText = null,
			)

		val NOT_FAVORITE
			get() = Inverted(
				option = Macro.FAVORITE,
				iconResId = R.drawable.ic_heart_off,
				titleResId = R.string.not_in_favorites,
				titleText = null,
			)
	}
}
