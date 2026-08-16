package com.mangaverse.app.main.ui.navigation3

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import com.mangaverse.app.space.domain.BuiltInSpaces

class SpaceNavigationStatesTest {

	@Test
	fun `disabled persistent navigation uses legacy manga state`() {
		resolveNavigationSpaceId(
			activeSpaceId = BuiltInSpaces.Anime,
			persistentNavigationEnabled = false,
		) shouldBe BuiltInSpaces.Manga
	}

	@Test
	fun `enabled persistent navigation follows active space`() {
		resolveNavigationSpaceId(
			activeSpaceId = BuiltInSpaces.Novel,
			persistentNavigationEnabled = true,
		) shouldBe BuiltInSpaces.Novel
	}
}
