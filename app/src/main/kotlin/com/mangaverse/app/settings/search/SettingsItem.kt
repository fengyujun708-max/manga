package com.mangaverse.app.settings.search

import com.mangaverse.app.list.ui.model.ListModel
import com.mangaverse.app.settings.SettingsDestination

data class SettingsItem(
	val key: String,
	val title: CharSequence,
	val breadcrumbs: List<String>,
	val destination: SettingsDestination,
) : ListModel {

	override fun areItemsTheSame(other: ListModel): Boolean {
		return other is SettingsItem && other.key == key && other.destination == destination
	}
}
