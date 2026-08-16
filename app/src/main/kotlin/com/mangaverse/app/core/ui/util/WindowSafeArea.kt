package com.mangaverse.app.core.ui.util

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowManager
import com.mangaverse.app.core.util.ext.getThemeColor

fun Activity.configureSafeAreaWindow() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }
    val backgroundColor = getThemeColor(android.R.attr.colorBackground, Color.BLACK)
    window.setBackgroundDrawable(ColorDrawable(backgroundColor))
    window.decorView.setBackgroundColor(backgroundColor)
}
