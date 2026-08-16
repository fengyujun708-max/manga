package com.mangaverse.app.reader.domain

data class TranslationLayerStateEvent(
	val pageId: Long,
	val state: TranslationLayerState,
)

enum class TranslationLayerState {
	IDLE,
	GENERATING,
	READY,
	FAILED,
}
