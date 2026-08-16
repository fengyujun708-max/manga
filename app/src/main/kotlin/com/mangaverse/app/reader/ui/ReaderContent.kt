package com.mangaverse.app.reader.ui

import com.mangaverse.app.reader.ui.pager.ReaderPage

data class ReaderContent(
	val pages: List<ReaderPage>,
	val state: ReaderState?
)