package com.mangaverse.app.details.ui.pager.pages

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mangaverse.app.core.util.ext.getItem
import com.mangaverse.app.list.ui.ContentSelectionDecoration

class PagesSelectionDecoration(context: Context) : ContentSelectionDecoration(context) {

	override fun getItemId(parent: RecyclerView, child: View): Long {
		val holder = parent.getChildViewHolder(child) ?: return RecyclerView.NO_ID
		val item = holder.getItem(PageThumbnail::class.java) ?: return RecyclerView.NO_ID
		return item.page.id
	}
}
