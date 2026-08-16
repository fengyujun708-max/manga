package com.mangaverse.app.search.ui.suggestion

import android.text.Editable
import android.view.KeyEvent
import android.widget.TextView
import androidx.core.net.toUri
import com.google.android.material.search.SearchView
import com.mangaverse.app.core.nav.AppRouter
import com.mangaverse.app.core.parser.ContentLinkResolver
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentTag
import com.mangaverse.app.search.domain.SearchKind

class SearchSuggestionListenerImpl(
	private val router: AppRouter,
	private val searchView: SearchView,
	private val viewModel: SearchSuggestionViewModel,
) : SearchSuggestionListener {

	override fun onContentClick(manga: Content) {
		router.openResolvedDetails(manga)
	}

	override fun onQueryClick(query: String, kind: SearchKind, submit: Boolean) {
		if (submit && query.isNotEmpty()) {
			if (kind == SearchKind.SIMPLE && ContentLinkResolver.isValidLink(query)) {
				router.openDetails(query.toUri())
			} else {
				router.openSearch(
					query = query,
					kind = kind,
					sourceTypes = viewModel.getSourceTypes(),
					contentKinds = viewModel.getContentKinds(),
				)
				if (kind != SearchKind.TAG) {
					viewModel.saveQuery(query)
				}
			}
			searchView.hide()
		} else {
			searchView.setText(query)
		}
	}

	override fun onTagClick(tag: ContentTag) {
		router.openSearch(
			query = tag.title,
			kind = SearchKind.TAG,
			sourceTypes = viewModel.getSourceTypes(),
			contentKinds = viewModel.getContentKinds(),
		)
	}

	override fun onSourceToggle(source: ContentSource, isEnabled: Boolean) {
		viewModel.onSourceToggle(source, isEnabled)
	}

	override fun onSourceClick(source: ContentSource) {
		router.openList(source, null, null)
	}

	override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

	override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

	override fun afterTextChanged(s: Editable?) {
		viewModel.onQueryChanged(s?.toString().orEmpty())
	}

	override fun onEditorAction(
		v: TextView?,
		actionId: Int,
		event: KeyEvent?
	): Boolean {
		val query = v?.text?.toString()
		if (query.isNullOrEmpty()) {
			return false
		}
		onQueryClick(query, SearchKind.SIMPLE, true)
		return true
	}
}
