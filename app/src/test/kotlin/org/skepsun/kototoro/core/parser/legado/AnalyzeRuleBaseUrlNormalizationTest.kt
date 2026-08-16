package com.mangaverse.app.core.parser.legado

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import com.mangaverse.app.core.javascript.RhinoJavaScriptEngine
import com.mangaverse.app.core.model.jsonsource.LegadoBookSource
import com.mangaverse.app.core.network.jsonsource.LegadoHttpClient
import com.mangaverse.app.core.parser.legado.sandbox.LegadoSandbox
import java.net.CookieManager

class AnalyzeRuleBaseUrlNormalizationTest : FunSpec({

    test("evalJS - baseUrl 末尾数字补 / 以兼容 match(/\\/(\\d+)\\//)") {
        val httpClient = mockk<LegadoHttpClient>(relaxed = true)
        val cookieManager = CookieManager()
        val cookieJar = mockk<com.mangaverse.app.core.network.jsonsource.PersistentCookieJar>(relaxed = true)
        val androidContext = mockk<Context>(relaxed = true)

        val jsEngine = RhinoJavaScriptEngine(
            httpClient = httpClient,
            cookieManager = cookieManager,
            cookieJar = cookieJar,
            androidContext = androidContext
        )

        try {
            val source = LegadoBookSource(
                bookSourceName = "Test Source",
                bookSourceUrl = "https://www.mkzhan.com"
            )
            val sandbox = LegadoSandbox(jsEngine, httpClient, source)
            val analyzer = AnalyzeRule(content = "x", sandbox = sandbox, baseUrl = "https://www.mkzhan.com/comic/12345")

            val id = analyzer.evalJS("baseUrl.match(/\\/(\\d+)\\//)[1]", result = "x")
            id.toString() shouldBe "12345"
        } finally {
            jsEngine.dispose()
        }
    }
})

