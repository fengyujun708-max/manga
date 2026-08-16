package com.mangaverse.app.main.ui

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import com.mangaverse.app.R
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.observeAsFlow

class ExitCallback(
	private val activity: MainActivity,
	private val snackbarHost: View,
) : OnBackPressedCallback(false) {

	private var job: Job? = null
	private val isDisabledByTimeout = MutableStateFlow(false)

	init {
		activity.lifecycleScope.launch {
			combine(
				observeSettings(),
				isDisabledByTimeout,
			) { enabledInSettings, disabledTemporary ->
				enabledInSettings && !disabledTemporary
			}.collect {
				isEnabled = it
			}
		}
	}

	override fun handleOnBackPressed() {
		job?.cancel()
		job = activity.lifecycleScope.launch {
			resetExitConfirmation()
		}
	}

	private suspend fun resetExitConfirmation() {
		isDisabledByTimeout.value = true
		val snackbar = Snackbar.make(snackbarHost, R.string.confirm_exit, Snackbar.LENGTH_INDEFINITE)
		snackbar.show()
		delay(2000)
		snackbar.dismiss()
		isDisabledByTimeout.value = false
	}

	private fun observeSettings(): Flow<Boolean> = activity.settings
		.observeAsFlow(AppSettings.KEY_EXIT_CONFIRM) { isExitConfirmationEnabled }
		.flowOn(Dispatchers.Default)
}
