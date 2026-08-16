package com.mangaverse.app.settings.sources.manage

import android.content.Context
import androidx.room.InvalidationTracker
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.mangaverse.app.R
import com.mangaverse.app.core.LocalizedAppContext
import com.mangaverse.app.core.db.TABLE_SOURCES
import com.mangaverse.app.core.model.getTitle
import com.mangaverse.app.core.model.isNsfw
import com.mangaverse.app.core.model.unwrap
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.util.ext.lifecycleScope
import com.mangaverse.app.explore.data.ContentSourcesRepository
import com.mangaverse.app.explore.data.SourcesSortOrder
import kotlinx.coroutines.flow.first
import com.mangaverse.app.parsers.model.ContentSource
import com.mangaverse.app.core.model.isLocal
import com.mangaverse.app.settings.sources.model.SourceConfigItem
import javax.inject.Inject

@ViewModelScoped
class SourcesListProducer @Inject constructor(
	lifecycle: ViewModelLifecycle,
	@LocalizedAppContext private val context: Context,
	private val repository: ContentSourcesRepository,
	private val settings: AppSettings,
) : InvalidationTracker.Observer(TABLE_SOURCES, com.mangaverse.app.core.db.TABLE_JSON_SOURCES) {

	private val scope = lifecycle.lifecycleScope
	private var query: String = ""
	val list = MutableStateFlow(emptyList<SourceConfigItem>())

	private var job = scope.launch(Dispatchers.Default) {
		list.value = buildList()
	}

	init {
		settings.observeChanges()
			.filter { it == AppSettings.KEY_TIPS_CLOSED || it == AppSettings.KEY_DISABLE_NSFW }
			.flowOn(Dispatchers.Default)
			.onEach { onInvalidated(emptySet()) }
			.launchIn(scope)
		repository.observeExternalExtensionChanges()
			.flowOn(Dispatchers.Default)
			.onEach { onInvalidated(emptySet()) }
			.launchIn(scope)
	}

	override fun onInvalidated(tables: Set<String>) {
		val prevJob = job
		job = scope.launch(Dispatchers.Default) {
			prevJob.cancelAndJoin()
			list.update { buildList() }
		}
	}

	fun setQuery(value: String) {
		this.query = value
		onInvalidated(emptySet())
	}

	private suspend fun buildList(): List<SourceConfigItem> {
		val enabledSources = repository.getEnabledSources().filter {
			val unwrapped = it.unwrap()
			!unwrapped.isLocal && unwrapped !is com.mangaverse.app.core.parser.external.ExternalContentSource
		}
		val pinned = repository.getPinnedSources().map { it.name }.toSet()
		val isNsfwDisabled = settings.isNsfwContentDisabled
		val isReorderAvailable = settings.sourcesSortOrder == SourcesSortOrder.MANUAL
		val isDisableAvailable = !settings.isAllSourcesEnabled
		val withTip = isReorderAvailable && settings.isTipEnabled(TIP_REORDER)
		val enabledSet = enabledSources.toSet()
		if (query.isNotEmpty()) {
			return enabledSources.mapNotNull {
				if (!it.getTitle(context).contains(query, ignoreCase = true)) {
					return@mapNotNull null
				}
				SourceConfigItem.SourceItem(
					source = it,
					isEnabled = it in enabledSet,
					isDraggable = false,
					isAvailable = !isNsfwDisabled || !it.isNsfw(),
					isPinned = it.name in pinned,
					isDisableAvailable = isDisableAvailable,
				)
			}.ifEmpty {
				listOf(SourceConfigItem.EmptySearchResult)
			}
		}
		val result = ArrayList<SourceConfigItem>(enabledSources.size + 1)
		if (enabledSources.isNotEmpty()) {
			if (withTip) {
				result += SourceConfigItem.Tip(
					TIP_REORDER,
					R.drawable.ic_tap_reorder,
					R.string.sources_reorder_tip,
				)
			}
			enabledSources.mapTo(result) {
				SourceConfigItem.SourceItem(
					source = it,
					isEnabled = true,
					isDraggable = isReorderAvailable,
					isAvailable = false,
					isPinned = it.name in pinned,
					isDisableAvailable = isDisableAvailable,
				)
			}
		}
		return result
	}

	companion object {

		const val TIP_REORDER = "src_reorder"
	}
}
