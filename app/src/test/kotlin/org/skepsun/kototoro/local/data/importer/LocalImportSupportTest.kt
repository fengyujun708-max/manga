package com.mangaverse.app.local.data.importer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LocalImportSupportTest {

	@Test
	fun `supports archive files`() {
		assertTrue(LocalImportSupport.supportsFileName("series.cbz"))
		assertTrue(LocalImportSupport.supportsFileName("archive.zip"))
	}

	@Test
	fun `classifies file names as manga`() {
		assertEquals(LocalImportKind.MANGA, LocalImportSupport.classifyFileName("archive.cbz"))
		assertEquals(LocalImportKind.MANGA, LocalImportSupport.classifyFileName("episode.mkv"))
		assertEquals(LocalImportKind.MANGA, LocalImportSupport.classifyFileName("chapter.txt"))
	}

	@Test
	fun `derives stable folder name from imported file name`() {
		assertEquals("archive", LocalImportSupport.contentFolderName("archive.cbz"))
		assertEquals("README", LocalImportSupport.contentFolderName("README"))
	}
}
