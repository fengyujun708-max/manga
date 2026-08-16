package com.mangaverse.app.core.exceptions

import com.mangaverse.app.parsers.model.Content

class UnsupportedSourceException(
	message: String?,
	val manga: Content?,
) : IllegalArgumentException(message)
