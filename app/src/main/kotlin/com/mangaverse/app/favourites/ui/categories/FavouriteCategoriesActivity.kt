package com.mangaverse.app.favourites.ui.categories

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.mangaverse.app.core.exceptions.resolve.SnackbarErrorObserver
import com.mangaverse.app.core.nav.router
import com.mangaverse.app.core.ui.BaseComposeActivity
import com.mangaverse.app.core.util.ext.observeEvent
import com.mangaverse.app.favourites.ui.categories.compose.FavouriteCategoriesScreen

@AndroidEntryPoint
class FavouriteCategoriesActivity :
	BaseComposeActivity() {

	private val viewModel by viewModels<FavouritesCategoriesViewModel>()
	private var selectedIds by mutableStateOf<Set<Long>>(emptySet())

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		viewModel.onError.observeEvent(this, SnackbarErrorObserver(window.decorView, null, exceptionResolver, null))
		setComposeContent {
			FavouriteCategoriesScreen(
				items = viewModel.content.collectAsStateWithLifecycle().value,
				selectedIds = selectedIds,
				onSelectionChanged = { selectedIds = it },
				onAdd = { router.openFavoriteCategoryCreate() },
				onOpenAll = { router.openFavorites() },
				onOpenCategory = { router.openFavorites(it) },
				onEditCategory = { router.openFavoriteCategoryEdit(it.id) },
				onShowAllChanged = viewModel::setAllCategoriesVisible,
				onSetVisible = viewModel::setIsVisible,
				onDelete = viewModel::deleteCategories,
				onSaveOrder = viewModel::saveOrder,
			)
		}
	}
}
