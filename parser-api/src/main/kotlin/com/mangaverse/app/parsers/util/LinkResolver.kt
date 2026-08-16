package com.mangaverse.app.parsers.util

import okhttp3.HttpUrl
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentSource

public interface LinkResolver {
    public val link: HttpUrl
    public suspend fun getSource(): ContentSource?
    public suspend fun getContent(): Content?
}
