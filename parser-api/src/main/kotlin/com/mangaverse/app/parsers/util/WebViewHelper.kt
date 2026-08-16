package com.mangaverse.app.parsers.util

import com.mangaverse.app.parsers.ContentLoaderContext

public class WebViewHelper(
	private val context: ContentLoaderContext,
) {

	public suspend fun getLocalStorageValue(domain: String, key: String): String? {
		return context.evaluateJs("$SCHEME_HTTPS://$domain/", "window.localStorage.getItem(\"$key\")")
	}
}
