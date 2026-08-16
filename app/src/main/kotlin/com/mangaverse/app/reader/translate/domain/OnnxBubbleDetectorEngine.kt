package com.mangaverse.app.reader.translate.domain

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.mangaverse.app.BuildConfig
import kotlinx.coroutines.Dispatchers
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
import javax.inject.Singleton

/**
 * 气泡检测引擎 —— 改为调用后端 /api/ocr（bbox 即气泡区域）。
 * 保留 DetectionResult/DetectionAttempt 等数据类签名。
 */
@Singleton
class OnnxBubbleDetectorEngine @Inject constructor() {

	private val LOG_TAG = "OnnxBubbleDetectorEngine"

	enum class AttemptStatus { SUCCESS, MODEL_NOT_AVAILABLE, INFERENCE_FAILED, NO_BOXES }

	data class DetectedBox(
		val rect: Rect,
		val classId: Int,
		val score: Float,
	)

	data class DetectionResult(
		val boxes: List<DetectedBox>,
		val modelId: String,
		val backend: String,
		val parser: String,
		val rawBoxCount: Int,
		val decodedBoxCount: Int,
		val finalBoxCount: Int,
		val totalMs: Long,
	)

	data class DetectionAttempt(
		val status: AttemptStatus,
		val result: DetectionResult? = null,
		val modelId: String = "",
		val backend: String = "",
		val parser: String = "",
		val stage: String = "",
		val inputName: String = "",
		val inputShape: String = "",
		val outputNames: String = "",
		val error: String = "",
	)

	private val httpClient: OkHttpClient by lazy {
		OkHttpClient.Builder()
			.connectTimeout(10, TimeUnit.SECONDS)
			.readTimeout(60, TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS)
			.build()
	}

	suspend fun detect(bitmap: Bitmap): DetectionResult? {
		return detectAttempt(bitmap).result
	}

	suspend fun detectAttempt(bitmap: Bitmap): DetectionAttempt {
		return detectAttempt(bitmap, "")
	}

	suspend fun detectAttempt(
		bitmap: Bitmap,
		preferredModelId: String,
	): DetectionAttempt = withContext(Dispatchers.IO) {
		val startMs = System.currentTimeMillis()
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
					return@withContext DetectionAttempt(
						status = AttemptStatus.INFERENCE_FAILED,
						error = "HTTP ${resp.code}",
						backend = "server",
					)
				}
				val json = JSONObject(resp.body?.string().orEmpty())
				val arr = json.optJSONArray("results")
				val boxes = ArrayList<DetectedBox>()
				if (arr != null) {
					for (i in 0 until arr.length()) {
						val item = arr.optJSONObject(i) ?: continue
						val bbox = item.optJSONArray("bbox") ?: continue
						if (bbox.length() < 4) continue
						val rect = Rect(
							bbox.optDouble(0).toInt(), bbox.optDouble(1).toInt(),
							bbox.optDouble(2).toInt(), bbox.optDouble(3).toInt(),
						)
						boxes.add(DetectedBox(rect, classId = 0, score = item.optDouble("conf", 1.0).toFloat()))
					}
				}
				val totalMs = System.currentTimeMillis() - startMs
				DetectionAttempt(
					status = if (boxes.isEmpty()) AttemptStatus.NO_BOXES else AttemptStatus.SUCCESS,
					result = DetectionResult(
						boxes = boxes,
						modelId = "server_rapidocr",
						backend = "server",
						parser = "rapidocr",
						rawBoxCount = boxes.size,
						decodedBoxCount = boxes.size,
						finalBoxCount = boxes.size,
						totalMs = totalMs,
					),
					modelId = "server_rapidocr",
					backend = "server",
				)
			}
		} catch (e: Exception) {
			Log.e(LOG_TAG, "bubble detect failed", e)
			DetectionAttempt(
				status = AttemptStatus.INFERENCE_FAILED,
				error = e.message.orEmpty(),
				backend = "server",
			)
		}
	}
}
