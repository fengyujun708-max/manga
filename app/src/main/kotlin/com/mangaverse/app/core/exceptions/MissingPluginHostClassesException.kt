package com.mangaverse.app.core.exceptions

class MissingPluginHostClassesException(
	val pluginName: String,
	val hostName: String,
	val missingClassNames: List<String>,
) : RuntimeException()
