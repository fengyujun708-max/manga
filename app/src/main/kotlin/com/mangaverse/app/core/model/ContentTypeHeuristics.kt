package com.mangaverse.app.core.model

import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentChapter
import com.mangaverse.app.parsers.model.ContentType
import java.util.Locale

private val videoExtensions = setOf(
    ".m3u8",
    ".mp4",
    ".mkv",
    ".webm",
    ".ts",
    ".avi",
    ".mov",
    ".flv",
    ".wmv",
)

fun String?.looksLikeVideoUrl(): Boolean {
    val normalized = this?.trim()?.lowercase(Locale.ROOT).orEmpty()
    if (normalized.isBlank()) {
        return false
    }
    if (normalized.contains("/video/")) {
        return true
    }
    return videoExtensions.any { normalized.endsWith(it) || normalized.contains("$it?") || normalized.contains("$it#") }
}

fun ContentChapter.looksLikeVideoChapter(): Boolean {
    if (source.getContentType().let { it == ContentType.VIDEO || it == ContentType.HENTAI_VIDEO }) {
        return true
    }
    return url.looksLikeVideoUrl()
}

fun Content.looksLikeLocalVideoContent(): Boolean {
    if (source.getContentType().let { it == ContentType.VIDEO || it == ContentType.HENTAI_VIDEO }) {
        return true
    }
    if (url.looksLikeVideoUrl() || publicUrl.looksLikeVideoUrl()) {
        return true
    }
    return chapters?.any { it.looksLikeVideoChapter() } == true
}
