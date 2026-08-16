package com.mangaverse.app.parsers.exception

import okio.IOException
import com.mangaverse.app.parsers.InternalParsersApi
import com.mangaverse.app.parsers.model.ContentSource

/**
 * Authorization is required for access to the requested content
 */
public class AuthRequiredException @InternalParsersApi @JvmOverloads constructor(
	public val source: ContentSource,
	cause: Throwable? = null,
) : IOException("Authorization required", cause)
