package com.mangaverse.app.settings

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.settings.compose.TranslationE2ESettingsScreen
import com.mangaverse.app.settings.support.TranslationApiSettingsSupport

@Composable
fun TranslationE2EApiSettingsRoute(
    settings: AppSettings,
    onFetchModelsClick: () -> Unit,
) {
    DisposableEffect(settings) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == AppSettings.KEY_READER_E2E_API_PROVIDER_PRESET) {
                TranslationApiSettingsSupport.applyApiProviderPreset(
                    sharedPreferences = sharedPreferences
                        ?: settings.prefs,
                    presetInput = settings.readerE2eApiProviderPreset,
                    forceOverride = true,
                    endpointKey = AppSettings.KEY_READER_E2E_API_ENDPOINT,
                    modelKey = AppSettings.KEY_READER_E2E_API_MODEL,
                )
            }
        }
        settings.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            settings.prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    TranslationE2ESettingsScreen(
        settings = settings,
        onFetchModels = onFetchModelsClick,
    )
}
