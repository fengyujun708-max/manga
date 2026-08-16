package com.mangaverse.app.details.ui

import androidx.compose.material3.SnackbarDuration
import com.mangaverse.app.R
import com.mangaverse.app.core.exceptions.CloudFlareProtectedException
import com.mangaverse.app.core.exceptions.UnsupportedSourceException
import com.mangaverse.app.core.exceptions.resolve.ErrorObserver
import com.mangaverse.app.core.exceptions.resolve.ExceptionResolver
import com.mangaverse.app.core.prefs.SourceSettings
import com.mangaverse.app.core.util.ext.findCloudFlareException
import com.mangaverse.app.core.util.ext.getDisplayMessage
import com.mangaverse.app.core.util.ext.isNetworkError
import com.mangaverse.app.core.util.ext.isSerializable
import com.mangaverse.app.parsers.exception.NotFoundException
import com.mangaverse.app.parsers.exception.ParseException

class DetailsErrorObserver(
	override val activity: DetailsActivity,
	private val viewModel: DetailsViewModel,
	resolver: ExceptionResolver?,
) : ErrorObserver(
	activity.contentRoot, null, resolver,
	{ isResolved ->
		if (isResolved) {
			viewModel.reload()
		}
	},
) {

	override suspend fun emit(value: Throwable) {
		val cf = value.findCloudFlareException()
		if (cf is CloudFlareProtectedException && canResolve(cf)) {
			val autoDisabled = SourceSettings(host.context, cf.source).isCaptchaAutoResolveDisabled
			if (!autoDisabled) {
				val resolved = resolveNow(cf, tryAutoResolve = true)
				if (resolved) {
					viewModel.reload()
					return
				}
			}
		}
		val duration = if (value is NotFoundException || value is UnsupportedSourceException) {
			SnackbarDuration.Indefinite
		} else {
			SnackbarDuration.Short
		}
		var actionLabel: String? = null
		var action: (() -> Unit)? = null
		when {
			canResolve(value) -> {
				actionLabel = host.context.getString(getResolveStringId(value))
				action = { resolve(value) }
			}

			value is ParseException -> {
				val router = router()
				if (router != null && value.isSerializable()) {
					actionLabel = host.context.getString(R.string.details)
					action = { router.showErrorDialog(value) }
				}
			}

			value.isNetworkError() -> {
				actionLabel = host.context.getString(R.string.try_again)
				action = viewModel::reload
			}
		}
		activity.showDetailsMessage(
			message = value.getDisplayMessage(host.context.resources),
			duration = duration,
			actionLabel = actionLabel,
			onAction = action,
		)
	}
}
