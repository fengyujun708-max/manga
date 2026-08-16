package com.mangaverse.app.settings


import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import com.mangaverse.app.R
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.settings.compose.AISettingsScreen
import com.mangaverse.app.core.ui.theme.KototoroTheme

@Composable
fun AISettingsRoute(
    onOpenOcrModels: () -> Unit,
    onOpenApiSettings: () -> Unit,
    onOpenTranslationSettings: () -> Unit,
    onOpenImageEnhancementSettings: () -> Unit,
) {
    AISettingsScreen(
        onOpenOcrModels = onOpenOcrModels,
        onOpenApiSettings = onOpenApiSettings,
        onOpenTranslationSettings = onOpenTranslationSettings,
        onOpenImageEnhancementSettings = onOpenImageEnhancementSettings,
    )
}
