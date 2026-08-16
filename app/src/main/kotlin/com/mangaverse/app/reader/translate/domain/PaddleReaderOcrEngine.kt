package com.mangaverse.app.reader.translate.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import com.mangaverse.app.BuildConfig
import com.mangaverse.app.core.image.BitmapDecoderCompat
import com.mangaverse.app.core.prefs.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * OCR 引擎 —— 改为调用后端 /api/ocr（RapidOCR onnx 在服务器，客户端零内置）。
 * 保留 ReaderOcrService / ReaderTextDetector / ReaderTextRecognizer 接口。
 */
class PaddleReaderOcrEngine @Inject constructor(
	private val settings: AppSettings,
	@ApplicationContext private val context: Context,
) : ReaderOcrService, ReaderTextDetector, ReaderTextRecognizer {

	private val LOG_TAG = "PaddleReaderOcrEngine"

	private val httpClient: OkHttpClient by lazy {
		OkHttpClient.Builder()
			.connectTimeout(10, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS)
			.build()
	}

	override suspend fun recognize(request: OcrRequest): List<OcrTextBlock> {
		val bitmap = decode(request.sourceUri) ?: return emptyList()
		return try {
			recognizeServer(bitmap, request.roi)
		} finally {
			bitmap.recycle()
		}
	}

	override suspend fun detect(sourceUri: Uri): List<TextRegion> {
		val bitmap = decode(sourceUri) ?: return emptyList()
		return try {
			detectServer(bitmap)
		} finally {
			bitmap.recycle()
		}
	}

	override suspend fun detect(bitmap: Bitmap): List<TextRegion> = detectServer(bitmap)

	override suspend fun recognize(sourceUri: Uri, regions: List<TextRegion>): List<OcrTextBlock> {
		val bitmap = decode(sourceUri) ?: return emptyList()
		return try {
			recognizeServer(bitmap, null)
		} finally {
			bitmap.recycle()
		}
	}

	override suspend fun recognize(bitmap: Bitmap, regions: List<TextRegion>): List<OcrTextBlock> {
		return recognizeServer(bitmap, null)
	}

	suspend fun recognize(
		sourceUri: Uri,
		regions: List<TextRegion>,
		automaticLanguage: String?,
	): List<OcrTextBlock> {
		val bitmap = decode(sourceUri) ?: return emptyList()
		return try {
			recognizeServer(bitmap, null)
		} finally {
			bitmap.recycle()
		}
	}

	private suspend fun decode(uri: Uri): Bitmap? = runInterruptible(Dispatchers.IO) {
		runCatching { BitmapDecoderCompat.decode(uri.toFile()) }.getOrNull()
	}

	/** 调后端 /api/ocr：返回 [{bbox:[x1,y1,x2,y2], text, conf}] */
	private suspend fun recognizeServer(bitmap: Bitmap, roi: Rect?): List<OcrTextBlock> = withContext(Dispatchers.IO) {
		try {
			val pngBytes = ByteArrayOutputStream().use { bos ->
				bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
				bos.toByteArray()
			}
			val body = MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("image", "page.png", pngBytes.toRequestBody("image/png".toMediaType()))
				.build()
			val request = Request.Builder()
				.url("${BuildConfig.MANGAVERSE_API_BASE_URL}/api/ocr")
				.post(body)
				.build()
			httpClient.newCall(request).execute().use { resp ->
				if (!resp.isSuccessful) {
					Log.w(LOG_TAG, "ocr API HTTP ${resp.code}")
					return@use emptyList()
				}
				val json = org.json.JSONObject(resp.body?.string().orEmpty())
				val arr = json.optJSONArray("results") ?: JSONArray()
				val out = ArrayList<OcrTextBlock>(arr.length())
				for (i in 0 until arr.length()) {
					val item = arr.optJSONObject(i) ?: continue
					val bbox = item.optJSONArray("bbox")
					if (bbox == null || bbox.length() < 4) continue
					val rect = Rect(
						bbox.optDouble(0).toInt(),
						bbox.optDouble(1).toInt(),
						bbox.optDouble(2).toInt(),
						bbox.optDouble(3).toInt(),
					)
					// ROI 过滤：只保留与 ROI 相交的文字块
					if (roi != null && !Rect.intersects(rect, roi)) continue
					val text = item.optString("text").trim()
					if (text.isEmpty()) continue
					out.add(
						OcrTextBlock(
							text = text,
							boundingBox = rect,
							confidence = item.optDouble("conf", 1.0).toFloat(),
							detectorId = "server_rapidocr",
						)
					)
				}
				out
			}
		} catch (e: Exception) {
			Log.e(LOG_TAG, "server ocr failed", e)
			emptyList()
		}
	}

	/** 调后端 /api/ocr 用 bbox 构造检测区域（气泡定位） */
	private suspend fun detectServer(bitmap: Bitmap): List<TextRegion> = withContext(Dispatchers.IO) {
		try {
			val blocks = recognizeServer(bitmap, null)
			blocks.map { block ->
				val rect = block.boundingBox ?: return@map null
				TextRegion(
					rect = rect,
					confidence = block.confidence,
					detectorId = "server_rapidocr",
				)
			}.filterNotNull()
		} catch (e: Exception) {
			Log.e(LOG_TAG, "server detect failed", e)
			emptyList()
		}
	}
}
