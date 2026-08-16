package com.mangaverse.app.remotelist.ui

import androidx.lifecycle.SavedStateHandle
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.mangaverse.app.core.model.ContentSourceInfo
import com.mangaverse.app.explore.data.ContentSourcesRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ContentListSourceGateViewModelTest {
	private val dispatcher = UnconfinedTestDispatcher()

	@BeforeEach
	fun setUp() = Dispatchers.setMain(dispatcher)

	@AfterEach
	fun tearDown() = Dispatchers.resetMain()

	@Test
	fun `gate opens when restored source appears in registry`() = runTest {
		var available = false
		val registryChanges = MutableStateFlow<List<ContentSourceInfo>>(emptyList())
		val sourcesRepository = mockk<ContentSourcesRepository> {
			every { isSourceAvailable("JAR_TEST") } answers { available }
			every { observeEnabledSources() } returns registryChanges
		}
		val viewModel = ContentListSourceGateViewModel(
			SavedStateHandle(mapOf("sourceName" to "JAR_TEST")),
			sourcesRepository,
		)

		viewModel.isResolutionReady.value shouldBe false
		available = true
		registryChanges.value = listOf(mockk())

		withTimeout(2_000L) { viewModel.isResolutionReady.first { it } } shouldBe true
	}
}
