package com.mangaverse.app.stats.data

data class WorkStatsSummaryRow(
	val entityId: Long,
	val totalPages: Int,
	val averageTimePerPage: Long,
	val entryCount: Int,
)
