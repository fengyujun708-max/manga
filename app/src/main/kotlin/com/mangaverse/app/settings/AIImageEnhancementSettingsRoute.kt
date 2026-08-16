package com.mangaverse.app.settings


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mangaverse.app.R
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.util.ext.viewLifecycleScope
import com.mangaverse.app.reader.translate.data.OnnxModelCategory
import com.mangaverse.app.reader.translate.data.OnnxModelManager
import com.mangaverse.app.reader.translate.data.OnnxOfficialModelCatalog
import com.mangaverse.app.settings.compose.AIImageEnhancementSettingsScreen
import com.mangaverse.app.settings.compose.SettingsChoiceOption
import com.mangaverse.app.core.ui.theme.KototoroTheme
import javax.inject.Inject

@Composable
fun AIImageEnhancementSettingsRoute(
    settings: AppSettings,
    onnxModelManager: OnnxModelManager,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val ncnnModels = buildList {
        addAll(
            OnnxOfficialModelCatalog.models
                .filter { it.category == OnnxModelCategory.IMAGE_SUPER_RESOLUTION }
                .map {
                    val suffix = if (onnxModelManager.isModelDownloaded(it.id)) {
                        ""
                    } else {
                        context.getString(R.string.reader_translation_ocr_model_selection_not_downloaded_suffix)
                    }
                    SettingsChoiceOption(it.id, it.title + suffix)
                },
        )
    }

    AIImageEnhancementSettingsScreen(
        settings = settings,
        ncnnModels = ncnnModels,
        modifier = modifier,
    )
}
