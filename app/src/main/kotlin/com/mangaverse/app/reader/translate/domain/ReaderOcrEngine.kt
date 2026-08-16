package com.mangaverse.app.reader.translate.domain

interface ReaderOcrService {

	suspend fun recognize(request: OcrRequest): List<OcrTextBlock>
}
