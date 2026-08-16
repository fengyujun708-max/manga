package com.mangaverse.app.local.ui

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import com.mangaverse.app.R
import com.mangaverse.app.core.nav.router

class LocalListMenuProvider(
	private val appRouter: com.mangaverse.app.core.nav.AppRouter,
	private val onImportClick: Function0<Unit>,
) : MenuProvider {

	override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
		menuInflater.inflate(R.menu.opt_local, menu)
		menuInflater.inflate(R.menu.opt_list, menu)
	}

	override fun onPrepareMenu(menu: Menu) {
		super.onPrepareMenu(menu)
		menu.findItem(R.id.action_filter)?.isVisible = appRouter.isFilterSupported()
	}

	override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
		return when (menuItem.itemId) {
			R.id.action_import -> {
				onImportClick()
				true
			}

			R.id.action_directories -> {
				appRouter.openDirectoriesSettings()
				true
			}

			R.id.action_filter -> {
				appRouter.showFilterSheet()
				true
			}

			R.id.action_list_mode -> {
				appRouter.showListConfigSheet(com.mangaverse.app.list.ui.config.ListConfigSection.General)
				true
			}

			else -> false
		}
	}
}
