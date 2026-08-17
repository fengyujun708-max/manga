package com.mangaverse.app.reader.translate.domain

const val CTD_MODEL_ID = "comictextdetector_2025_onnx"

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import com.mangaverse.app.BuildConfig
import com.mangaverse.app.core.image.BitmapDecoderCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** 气泡文本检测器 —— 改为调用后端 /api/ocr。 */
class ComicTextDetectorOnnx @Inject constructor() : ReaderTextDetector {

	private val LOG_TAG = "ComicTextDetectorOnnx"
	private val httpClient: OkHttpClient by lazy {
		OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
	}

	override suspend fun detect(sourceUri: Uri): List<TextRegion> {
		val bitmap = runInterruptible(Dispatchers.IO) { runCatching { BitmapDecoderCompat.decode(sourceUri.toFile()) }.getOrNull() } ?: return emptyList()
		return try { detectServer(bitmap) } finally { bitmap.recycle() }
	}

	override suspend fun detect(bitmap: Bitmap): List<TextRegion> = detectServer(bitmap)

	private suspend fun detectServer(bitmap: Bitmap): List<TextRegion> = withContext(Dispatchers.IO) {
		try {
			val pngBytes = ByteArrayOutputStream().use { bos -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos); bos.toByteArray() }
			val body = MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("image", "page.png", pngBytes.toRequestBody("image/png".toMediaType())).build()
			val request = Request.Builder().url("${BuildConfig.MANGAVERSE_API_BASE_URL}/api/ocr").post(body).build()
			httpClient.newCall(request).execute().use { resp ->
				if (!resp.isSuccessful) { Log.w(LOG_TAG, "HTTP ${resp.code}"); return@use emptyList() }
				val json = JSONObject(resp.body?.string().orEmpty())
				val arr = json.optJSONArray("results")
				val out = ArrayList<TextRegion>(arr?.length() ?: 0)
				if (arr != null) for (i in 0 until arr.length()) {
					val item = arr.optJSONObject(i) ?: continue
					val bbox = item.optJSONArray("bbox") ?: continue
					if (bbox.length() < 4) continue
					out.add(TextRegion(
						rect = android.graphics.Rect(bbox.optDouble(0).toInt(), bbox.optDouble(1).toInt(), bbox.optDouble(2).toInt(), bbox.optDouble(3).toInt()),
						confidence = item.optDouble("conf", 1.0).toFloat(),
						detectorId = "server_rapidocr",
					))
				}
				out
			}
		} catch (e: Exception) { Log.e(LOG_TAG, "detect failed", e); emptyList() }
	}
}
