package com.mangaverse.app.list.ui.model

import androidx.annotation.StringRes
import com.mangaverse.app.R
import com.mangaverse.app.core.exceptions.resolve.ExceptionResolver
import com.mangaverse.app.core.util.ext.getDisplayIcon
import com.mangaverse.app.core.util.ext.getCauseUrl
import com.mangaverse.app.core.util.ext.findCloudFlareException
import com.mangaverse.app.parsers.util.ifZero

fun Throwable.toErrorState(canRetry: Boolean = true, @StringRes secondaryAction: Int = 0) = ErrorState(
	exception = this,
	icon = getDisplayIcon(),
	canRetry = canRetry,
	buttonText = ExceptionResolver.getResolveStringId(this).ifZero { R.string.try_again },
	secondaryButtonText = secondaryAction.takeIf { it != 0 }
		?: getCauseUrl()?.let { R.string.open_in_browser }
		?: findCloudFlareException()?.url?.let { R.string.open_in_browser }
		?: 0,
)

fun Throwable.toErrorFooter() = ErrorFooter(
	exception = this,
)

operator fun ListModel.plus(list: List<ListModel>): List<ListModel> {
	val result = ArrayList<ListModel>(list.size + 1)
	result.add(this)
	result.addAll(list)
	return result
}

operator fun ListModel.plus(other: ListModel): List<ListModel> = listOf(this, other)
