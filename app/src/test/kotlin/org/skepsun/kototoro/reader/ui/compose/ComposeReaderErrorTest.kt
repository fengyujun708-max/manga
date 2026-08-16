package com.mangaverse.app.reader.ui.compose

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.mangaverse.app.R

class ComposeReaderErrorTest {

	@Test
	fun `uses resolver action label when available`() {
		assertEquals(R.string.captcha_solve, resolveReaderErrorActionStringId(R.string.captcha_solve))
	}

	@Test
	fun `falls back to retry label for ordinary errors`() {
		assertEquals(R.string.try_again, resolveReaderErrorActionStringId(0))
	}
}
