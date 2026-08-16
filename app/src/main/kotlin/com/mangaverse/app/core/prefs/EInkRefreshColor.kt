package com.mangaverse.app.core.prefs

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.Keep

@Keep
enum class EInkRefreshColor(@ColorInt val colorInt: Int) {
	WHITE(Color.WHITE),
	BLACK(Color.BLACK),
}
