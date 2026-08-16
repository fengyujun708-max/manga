package com.mangaverse.app.core.model

import android.content.Context
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.text.inSpans
import com.mangaverse.app.R
import com.mangaverse.app.core.parser.external.ExternalContentSource
import com.mangaverse.app.core.util.ext.getDisplayName
import com.mangaverse.app.core.util.ext.toLocale
import com.mangaverse.app.core.util.ext.toLocaleOrNull

import com.mangaverse.app.core.parser.kotatsu.KotatsuParserSource
import com.mangaverse.app.core.db.entity.JsonSourceType
import com.mangaverse.app.parsers.model.ContentType
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.util.splitTwoParts
import org.json.JSONObject
import java.util.Locale

data object LocalMangaSource : ContentSource {
	override val name = "LOCAL"
	override val locale = ""
	override val contentType = ContentType.MANGA
}

val ContentSource.isLocal: Boolean
	get() = this == LocalMangaSource

data object UnknownContentSource : ContentSource {
	override val name = "UNKNOWN"
	override val locale = ""
	override val contentType = ContentType.OTHER
}

data object TestContentSource : ContentSource {
	override val name = "TEST"
	override val locale = ""
	override val contentType = ContentType.OTHER
}

fun ContentSource(name: String?): ContentSource {
	when (name ?: return UnknownContentSource) {
	UnknownContentSource.name -> return UnknownContentSource
	LocalMangaSource.name -> return LocalMangaSource
	TestContentSource.name -> return TestContentSource
	}
	if (name.startsWith("content:")) {
		val parts = name.substringAfter(':').splitTwoParts('/') ?: return UnknownContentSource
		return ExternalContentSource(packageName = parts.first, authority = parts.second)
	}
	com.mangaverse.app.core.extensions.GlobalExtensionManager.mangaSources.value.find { it.name == name }?.let { return com.mangaverse.app.core.parser.kotatsu.KotatsuParserSource(it) }
	com.mangaverse.app.core.extensions.GlobalExtensionManager.contentSources.value.find { it.name == name }?.let { return it }

	// Fallbacks: If not loaded yet, return stable AnonymousContentSource
	// Keep the original name so it isn't lost if the source loads later
	return AnonymousContentSource(name)
}

fun Collection<String>.toContentSources() = map(::ContentSource)

fun ContentSource.isNsfw(): Boolean = when (this) {
	is ContentSourceInfo -> mangaSource.isNsfw()
	is com.mangaverse.app.mihon.model.MihonMangaSource -> isNsfw
	else -> contentType in setOf(
		ContentType.HENTAI_MANGA,
		ContentType.HENTAI_NOVEL,
		ContentType.HENTAI_VIDEO,
	)
}

@get:StringRes
val ContentType.titleResId
	get() = when (this) {
		ContentType.MANGA -> R.string.content_type_manga
		ContentType.HENTAI_MANGA -> R.string.content_type_hentai_manga
		ContentType.HENTAI_NOVEL -> R.string.content_type_hentai_novel
		ContentType.HENTAI_VIDEO -> R.string.content_type_hentai_video
		ContentType.COMICS -> R.string.content_type_comics
		ContentType.VIDEO -> R.string.content_type_video
		ContentType.OTHER -> R.string.content_type_other
		ContentType.MANHWA -> R.string.content_type_manhwa
		ContentType.MANHUA -> R.string.content_type_manhua
		ContentType.NOVEL -> R.string.content_type_novel
		ContentType.ONE_SHOT -> R.string.content_type_one_shot
		ContentType.DOUJINSHI -> R.string.content_type_doujinshi
		ContentType.IMAGE_SET -> R.string.content_type_image_set
		ContentType.ARTIST_CG -> R.string.content_type_artist_cg
		ContentType.GAME_CG -> R.string.content_type_game_cg
	}

fun ContentType.getEnableSourceTitleResId(): Int = when (this) {
	ContentType.NOVEL, ContentType.HENTAI_NOVEL -> R.string.enable_source_novel
	ContentType.VIDEO, ContentType.HENTAI_VIDEO -> R.string.enable_source_video
	else -> R.string.enable_source_manga
}

fun ContentType.getUnsupportedSourceTitleResId(): Int = when (this) {
	ContentType.NOVEL, ContentType.HENTAI_NOVEL -> R.string.unsupported_novel_source
	ContentType.VIDEO, ContentType.HENTAI_VIDEO -> R.string.unsupported_video_source
	else -> R.string.unsupported_source
}

fun ContentType.getDomainTitleResId(): Int = when (this) {
	ContentType.NOVEL, ContentType.HENTAI_NOVEL -> R.string.domain_novel
	ContentType.VIDEO, ContentType.HENTAI_VIDEO -> R.string.domain_video
	else -> R.string.domain_manga
}

fun ContentType.getSaveTitleResId(): Int = when (this) {
	ContentType.NOVEL, ContentType.HENTAI_NOVEL -> R.string.save_manga_novel
	ContentType.VIDEO, ContentType.HENTAI_VIDEO -> R.string.save_manga_video
	else -> R.string.save_manga_manga
}

fun ContentType.getWholeWorkOptionResId(): Int = when (this) {
	ContentType.NOVEL, ContentType.HENTAI_NOVEL -> R.string.download_option_whole_manga_novel
	ContentType.VIDEO, ContentType.HENTAI_VIDEO -> R.string.download_option_whole_manga_video
	else -> R.string.download_option_whole_manga_manga
}

fun ContentType.getRecommendationTermResId(): Int = when (this) {
	ContentType.NOVEL, ContentType.HENTAI_NOVEL -> R.string.recommendation_novel
	ContentType.VIDEO, ContentType.HENTAI_VIDEO -> R.string.recommendation_video
	else -> R.string.recommendation_manga
}

tailrec fun ContentSource.unwrap(): ContentSource = if (this is ContentSourceInfo) {
	mangaSource.unwrap()
} else {
	this
}

fun ContentSource.getLocale(): Locale? = unwrap().locale.takeIf { it.isNotEmpty() }?.toLocaleOrNull()

fun ContentSource.getContentType(): ContentType = unwrap().contentType

fun ContentSource.resolvedContentTypeForSnapshot(): ContentType? {
	val resolved = unwrap()
	return resolved.contentType.takeUnless {
		resolved === UnknownContentSource ||
			(resolved is AnonymousContentSource && resolved.contentType == ContentType.OTHER)
	}
}

fun ContentSource.getSummary(context: Context, contentType: ContentType? = null): String? = when (val source = unwrap()) {
	is KotatsuParserSource,
	is com.mangaverse.app.mihon.model.MihonMangaSource -> {
		val resolvedContentType = contentType ?: getContentType()
		val type = context.getString(resolvedContentType.titleResId)
		val lang = source.locale.toLocale()
		val locale = lang.getDisplayName(context)
		val base = context.getString(R.string.source_summary_pattern, type, locale)
		appendOriginSuffix(context, base, source.getOriginLabel(context))
	}

	else -> {
		val resolvedContentType = contentType ?: getContentType()
		val type = context.getString(resolvedContentType.titleResId)
		val base = if (source.locale.isNotEmpty()) {
			val locale = source.locale.toLocale().getDisplayName(context)
			context.getString(R.string.source_summary_pattern, type, locale)
		} else type
		appendOriginSuffix(context, base, source.getOriginLabel(context))
	}
}

private fun appendOriginSuffix(context: Context, base: String, originLabel: String?): String {
	if (originLabel.isNullOrBlank()) {
		return base
	}
	val currentLanguage = context.resources.configuration.locales.get(0)?.language.orEmpty()
	val (open, close) = if (currentLanguage == "zh") "（" to "）" else "(" to ")"
	return "$base$open$originLabel$close"
}

fun ContentSource.getOriginLabel(context: Context): String? = when (this) {
	is ContentSourceInfo -> mangaSource.getOriginLabel(context)
	is KotatsuParserSource -> "Kotatsu"
	is ExternalContentSource -> context.getString(R.string.external_source)
	is com.mangaverse.app.mihon.model.MihonMangaSource -> "Mihon"
	is com.mangaverse.app.core.jsonsource.JsonContentSource,
	is com.mangaverse.app.core.jsonsource.JsonSourceListSource -> {
		val type = com.mangaverse.app.core.jsonsource.SourceTypeIdentifier().getSourceType(name)
		when (type) {
			com.mangaverse.app.core.jsonsource.SourceType.JSON_LEGADO -> "Legado"
			com.mangaverse.app.core.jsonsource.SourceType.JSON_JS -> "JS"
			else -> "JSON"
		}
	}
	else -> {
		val type = com.mangaverse.app.core.jsonsource.SourceTypeIdentifier().getSourceType(name)
		when {
			name.startsWith("TRACKING_") -> name.removePrefix("TRACKING_")
			type == com.mangaverse.app.core.jsonsource.SourceType.MIHON -> "Mihon"
			type == com.mangaverse.app.core.jsonsource.SourceType.JSON_LEGADO -> "Legado"
			type == com.mangaverse.app.core.jsonsource.SourceType.JSON_JS -> "JS"
			type == com.mangaverse.app.core.jsonsource.SourceType.EXTERNAL -> context.getString(R.string.external_source)
			type == com.mangaverse.app.core.jsonsource.SourceType.NATIVE -> null
			else -> null
		}
	}
}

fun ContentSource.getTitle(context: Context): String {
	val baseTitle = when (val source = unwrap()) {
		is KotatsuParserSource -> source.title
		LocalMangaSource -> context.getString(R.string.local_storage)
		TestContentSource -> context.getString(R.string.test_parser)
		is ExternalContentSource -> source.resolveName(context)
		is com.mangaverse.app.core.jsonsource.JsonContentSource -> source.displayName.ifBlank { source.name }
		is com.mangaverse.app.core.jsonsource.JsonSourceListSource -> source.displayName.ifBlank { source.name }
		is com.mangaverse.app.mihon.model.MihonMangaSource -> source.displayName
		else -> {
			// Try to handle anonymous wrappers for JSON or Mihon sources
			if (source.name.startsWith("MIHON_")) {
				"Loading Mihon source..."
			} else if (source.name.startsWith("JSON_")) {
				"Loading JSON source..."
			} else if (source.name.startsWith("TRACKING_")) {
				source.name.removePrefix("TRACKING_")
			} else {
				// If it's a dynamic plugin, it might have a title property reflectively, or we use name
				val underlying = if (source is com.mangaverse.app.core.extensions.PluginContentSource) source.originalSource else source
				val titleMethod = try { underlying.javaClass.getMethod("getTitle") } catch(e: Exception) { null }
				if (titleMethod != null) {
					titleMethod.invoke(underlying) as? String ?: source.name
				} else {
					source.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
				}
			}
		}
	}
	return if (this.isBroken) {
		"$baseTitle (${context.getString(R.string.source_broken)})"
	} else {
		baseTitle
	}
}

fun ContentSource.getStableIdentityKey(): String {
	return when (val source = unwrap()) {
		is com.mangaverse.app.mihon.model.MihonMangaSource ->
			"mihon:${source.pkgName}:${source.sourceId}:${source.language}:${source.contentType.name}"
		is com.mangaverse.app.core.parser.external.ExternalContentSource ->
			"external:${source.packageName}:${source.authority}"
		is com.mangaverse.app.core.jsonsource.JsonContentSource ->
			"json:${source.entity.id}:${source.entity.type}:${source.contentType.name}"
		is com.mangaverse.app.core.jsonsource.JsonSourceListSource ->
			"json_list:${source.name}:${source.contentType.name}"
		is KotatsuParserSource ->
			"kotatsu:${source.name}:${source.locale}:${source.contentType.name}"
		else ->
			"${source::class.java.name}:${source.name}:${source.locale}:${source.contentType.name}"
	}
}

val ContentSource.isBroken: Boolean
	get() {
		val unwrapped = this.unwrap()
		return when (unwrapped) {
			is KotatsuParserSource -> unwrapped.isBroken
			is com.mangaverse.app.core.extensions.PluginContentSource -> unwrapped.isBroken
			else -> {
				com.mangaverse.app.core.extensions.GlobalExtensionManager.contentSources.value.find { it.originalSource == unwrapped || it.name == unwrapped.name }?.isBroken == true ||
				com.mangaverse.app.core.extensions.GlobalExtensionManager.mangaSources.value.find { it.originalSource == unwrapped || it.name == unwrapped.name }?.isBroken == true
			}
		}
	}


fun SpannableStringBuilder.appendIcon(textView: TextView, @DrawableRes resId: Int): SpannableStringBuilder {
	val icon = ContextCompat.getDrawable(textView.context, resId) ?: return this
	icon.setTintList(textView.textColors)
	val size = textView.lineHeight
	icon.setBounds(0, 0, size, size)
	val alignment = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		ImageSpan.ALIGN_CENTER
	} else {
		ImageSpan.ALIGN_BOTTOM
	}
	return inSpans(ImageSpan(icon, alignment)) { append(' ') }
}

private class AnonymousContentSource(override val name: String) : ContentSource {
	override val locale: String = ""
	override val contentType: ContentType get() = when {
		name.startsWith("JSON_LEGADO_M_") -> ContentType.MANGA
		name.startsWith("JSON_LEGADO_") -> ContentType.NOVEL
		name.startsWith("MIHON_") -> ContentType.MANGA
		else -> ContentType.OTHER
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ContentSource) return false
		return name == other.name
	}

	override fun hashCode(): Int = name.hashCode()
	
	override fun toString(): String = "AnonymousContentSource(name=$name)"
}
