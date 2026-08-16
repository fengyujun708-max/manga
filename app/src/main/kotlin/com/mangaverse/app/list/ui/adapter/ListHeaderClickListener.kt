package com.mangaverse.app.list.ui.adapter

import android.view.View
import com.mangaverse.app.list.ui.model.ListHeader

interface ListHeaderClickListener {

	fun onListHeaderClick(item: ListHeader, view: View)
}
