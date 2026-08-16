package com.mangaverse.app.core.exceptions.resolve

import okhttp3.Headers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.mangaverse.app.R
import com.mangaverse.app.core.exceptions.CloudFlareProtectedException
import com.mangaverse.app.core.model.TestContentSource

class ExceptionResolverTest {

    @Test
    fun `wrapped Cloudflare challenge remains resolvable`() {
        val challenge = CloudFlareProtectedException(
            url = "https://example.org/",
            source = TestContentSource,
            headers = Headers.headersOf("User-Agent", "test"),
        )

        assertEquals(
            R.string.captcha_solve,
            ExceptionResolver.getResolveStringId(java.io.IOException("wrapped", challenge)),
        )
    }
}
