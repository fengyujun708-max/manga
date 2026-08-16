package com.mangaverse.app.search.ui

import android.content.Context
import com.mangaverse.app.R
import com.mangaverse.app.core.jsonsource.SourceType
import com.mangaverse.app.core.ui.dialog.buildAlertDialog
import com.mangaverse.app.search.domain.ALL_SOURCE_TYPES
import com.mangaverse.app.search.domain.SOURCE_TYPE_OPTIONS

fun showSourceTypeDialog(
	context: Context,
	current: Set<SourceType>,
	onApply: (Set<SourceType>) -> Unit,
) {
	val options = SOURCE_TYPE_OPTIONS
	val labels = options.map { context.getString(it.titleRes) }.toTypedArray()
	val selected = current.toMutableSet()
	val checked = BooleanArray(options.size) { index ->
		options[index].type in current
	}
	buildAlertDialog(context) {
		setTitle(R.string.source_type)
		setMultiChoiceItems(labels, checked) { _, which, isChecked ->
			val type = options[which].type
			if (isChecked) {
				selected.add(type)
			} else {
				selected.remove(type)
			}
		}
		setNegativeButton(android.R.string.cancel, null)
		setPositiveButton(android.R.string.ok) { _, _ ->
			val resolved = if (selected.isEmpty()) ALL_SOURCE_TYPES else selected
			onApply(resolved)
		}
	}.show()
}
