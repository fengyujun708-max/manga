package com.mangaverse.app.settings.nav

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mangaverse.app.R
import com.mangaverse.app.core.ui.theme.KototoroTheme
import com.mangaverse.app.settings.SettingsActivity
import com.mangaverse.app.settings.compose.NavConfigScreen

@Composable
fun NavConfigRoute(
	viewModel: NavConfigViewModel,
	modifier: Modifier = Modifier,
) {
	val configuredItems = viewModel.configuredItems.collectAsStateWithLifecycle().value
	val availableItems = viewModel.availableItems.collectAsStateWithLifecycle().value
	val canShowAddAction = viewModel.canShowAddAction.collectAsStateWithLifecycle().value
	val canAddAction = viewModel.canAddAction.collectAsStateWithLifecycle().value

	NavConfigScreen(
		configuredItems = configuredItems,
		availableItems = availableItems,
		canShowAddAction = canShowAddAction,
		canAddAction = canAddAction,
		onAddItem = viewModel::addItem,
		onRemoveItem = viewModel::removeItem,
		onMoveUp = viewModel::moveUp,
		onMoveDown = viewModel::moveDown,
		modifier = modifier,
	)
}
