package com.mangaverse.app.settings

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.settings.compose.TranslationApiSettingsScreen
import com.mangaverse.app.settings.support.TranslationApiSettingsSupport

@Composable
fun TranslationApiSettingsRoute(
    settings: AppSettings,
    onFetchModelsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(settings) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == AppSettings.KEY_READER_TRANSLATION_API_PROVIDER_PRESET) {
                TranslationApiSettingsSupport.applyApiProviderPreset(
                    sharedPreferences = sharedPreferences ?: settings.prefs,
                    presetInput = settings.readerTranslationApiProviderPreset,
                    forceOverride = true,
                )
            }
        }
        settings.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            settings.prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    TranslationApiSettingsScreen(
        settings = settings,
        onFetchModelsClick = onFetchModelsClick,
        modifier = modifier,
    )
}
