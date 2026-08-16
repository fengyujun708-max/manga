package com.mangaverse.app.core.ui

import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.mangaverse.app.R
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.ColorScheme
import com.mangaverse.app.core.prefs.InterfaceStyle
import com.mangaverse.app.core.ui.util.configureSafeAreaWindow

internal fun AppCompatActivity.applyKototoroActivityTheme(settings: AppSettings) {
	setTheme(settings.colorScheme.styleResId)
	if (settings.isAmoledTheme) {
		setTheme(R.style.ThemeOverlay_Kototoro_Amoled)
	}
	if (settings.interfaceStyle == InterfaceStyle.IOS && settings.colorScheme == ColorScheme.IOS) {
		setTheme(R.style.ThemeOverlay_Kototoro_IosPalette)
	}
	if (settings.interfaceStyle == InterfaceStyle.MATERIAL_3_EXPRESSIVE) {
		setTheme(R.style.ThemeOverlay_Kototoro_ExpressiveComponents)
	}
	when (settings.loadingCircleStyle) {
		AppSettings.LoadingCircleStyle.THICK_STRAIGHT ->
			setTheme(R.style.ThemeOverlay_Kototoro_Loading_ThickStraight)

		AppSettings.LoadingCircleStyle.THICK_WAVY ->
			setTheme(R.style.ThemeOverlay_Kototoro_Loading_ThickWavy)

		AppSettings.LoadingCircleStyle.THIN_STRAIGHT ->
			setTheme(R.style.ThemeOverlay_Kototoro_Loading_ThinStraight)

		AppSettings.LoadingCircleStyle.THIN_WAVY ->
			setTheme(R.style.ThemeOverlay_Kototoro_Loading_ThinWavy)
	}
	when (settings.popupRadius) {
		12 -> setTheme(R.style.ThemeOverlay_Kototoro_PopupRadius_12)
		16 -> setTheme(R.style.ThemeOverlay_Kototoro_PopupRadius_16)
		20 -> setTheme(R.style.ThemeOverlay_Kototoro_PopupRadius_20)
		24 -> setTheme(R.style.ThemeOverlay_Kototoro_PopupRadius_24)
	}
}

internal fun ComponentActivity.configureKototoroEdgeToEdge() {
	enableEdgeToEdge()
	WindowCompat.setDecorFitsSystemWindows(window, false)
	configureSafeAreaWindow()
}
