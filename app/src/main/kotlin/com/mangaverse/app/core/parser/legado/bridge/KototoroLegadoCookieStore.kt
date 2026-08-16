package com.mangaverse.app.core.parser.legado.bridge

import com.mangaverse.app.core.javascript.LegadoCookieAPI
import com.mangaverse.app.core.parser.legado.runtime.LegadoCookieStore

/**
 * 基于现有 LegadoCookieAPI 的 runtime CookieStore 适配器。
 */
class KototoroLegadoCookieStore(
    private val cookieApi: LegadoCookieAPI,
) : LegadoCookieStore {

    override fun getCookie(urlOrKey: String): String? {
        return cookieApi.getCookie(urlOrKey)
    }

    override fun setCookie(urlOrKey: String, cookie: String?) {
        cookieApi.setCookie(urlOrKey, cookie)
    }

    override fun replaceCookie(urlOrKey: String, cookie: String) {
        cookieApi.replaceCookie(urlOrKey, cookie)
    }

    override fun removeCookie(urlOrKey: String) {
        cookieApi.removeCookie(urlOrKey)
    }
}
