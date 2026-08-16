package com.mangaverse.app.search.ui.suggestion

import android.text.TextWatcher
import android.widget.TextView
import com.mangaverse.app.parsers.model.Content
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.parsers.model.ContentTag
import com.mangaverse.app.search.domain.SearchKind

interface SearchSuggestionListener : TextWatcher, TextView.OnEditorActionListener {

	fun onContentClick(manga: Content)

	fun onQueryClick(query: String, kind: SearchKind, submit: Boolean)

	fun onSourceToggle(source: ContentSource, isEnabled: Boolean)

	fun onSourceClick(source: ContentSource)

	fun onTagClick(tag: ContentTag)
}
