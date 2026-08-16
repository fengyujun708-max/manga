package com.mangaverse.app.settings.sources.adapter

import com.mangaverse.app.core.ui.list.OnTipCloseListener
import com.mangaverse.app.settings.sources.model.SourceConfigItem

interface SourceConfigListener : OnTipCloseListener<SourceConfigItem.Tip> {

	fun onItemSettingsClick(item: SourceConfigItem.SourceItem)

	fun onItemLiftClick(item: SourceConfigItem.SourceItem)

	fun onItemShortcutClick(item: SourceConfigItem.SourceItem)

	fun onItemPinClick(item: SourceConfigItem.SourceItem)

	fun onItemEnabledChanged(item: SourceConfigItem.SourceItem, isEnabled: Boolean)

	fun onItemClick(item: SourceConfigItem.SourceItem)
}
