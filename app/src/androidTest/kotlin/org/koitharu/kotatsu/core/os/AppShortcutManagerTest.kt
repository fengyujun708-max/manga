package com.mangaverse.app.core.os

import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.mangaverse.app.SampleData
import com.mangaverse.app.awaitForIdle
import com.mangaverse.app.core.db.MangaDatabase
import com.mangaverse.app.history.data.HistoryRepository
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppShortcutManagerTest {

	@get:Rule
	var hiltRule = HiltAndroidRule(this)

	@Inject
	lateinit var historyRepository: HistoryRepository

	@Inject
	lateinit var appShortcutManager: AppShortcutManager

	@Inject
	lateinit var database: MangaDatabase

	@Before
	fun setUp() {
		hiltRule.inject()
		database.clearAllTables()
	}

	@Test
	fun testUpdateShortcuts() = runTest {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
			return@runTest
		}
		database.invalidationTracker.addObserver(appShortcutManager)
		awaitUpdate()
		assertTrue(getShortcuts().isEmpty())
		historyRepository.addOrUpdate(
			manga = testContent(),
			chapterId = 1L,
			page = 4,
			scroll = 2,
			percent = 0.3f,
			force = false,
		)
		awaitUpdate()

		val shortcuts = getShortcuts()
		assertEquals(1, shortcuts.size)
	}

	private fun getShortcuts(): List<ShortcutInfo> {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		val manager = checkNotNull(context.getSystemService<ShortcutManager>())
		return manager.dynamicShortcuts.filterNot { it.id == "com.squareup.leakcanary.dynamic_shortcut" }
	}

	private suspend fun awaitUpdate() {
		val instrumentation = InstrumentationRegistry.getInstrumentation()
		instrumentation.awaitForIdle()
		appShortcutManager.await()
	}

	private fun testContent() = com.mangaverse.app.parsers.model.Content(
		id = 1L,
		title = "Test Manga",
		altTitles = emptySet(),
		url = "/test",
		publicUrl = "https://example.test/test",
		rating = 0f,
		contentRating = null,
		coverUrl = null,
		tags = emptySet(),
		state = null,
		authors = emptySet(),
		source = object : com.mangaverse.app.parsers.model.ContentSource {
			override val name: String = "TEST"
			override val locale: String = ""
			override val contentType = com.mangaverse.app.parsers.model.ContentType.MANGA
		},
	)
}
