package com.mangaverse.app.reader.translate.domain

import android.util.Log
import com.mangaverse.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * 翻译引擎 —— 改为调用后端 /api/translate（模型在服务器，客户端零内置）。
 * 保留 translateBatch 接口签名，调用方无需改动。
 */
@Singleton
class OnnxReaderTranslationEngine @Inject constructor() {

	private val LOG_TAG = "OnnxReaderTranslationEngine"

	private val httpClient: OkHttpClient by lazy {
		OkHttpClient.Builder()
			.connectTimeout(10, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(30, TimeUnit.SECONDS)
			.build()
	}

	suspend fun translateBatch(
		texts: List<String>,
		sourceLang: String,
		targetLang: String,
		modelId: String,
	): Map<String, String> {
		if (texts.isEmpty()) return emptyMap()
		Log.d(LOG_TAG, "translateBatch server mode: texts=${texts.size} $sourceLang->$targetLang")

		// 分批调用后端（单次 500 字符限制）
		val result = LinkedHashMap<String, String>(texts.size)
		val batchSize = 8
		for (chunk in texts.chunked(batchSize)) {
			val chunkResult = translateChunk(chunk, sourceLang, targetLang)
			result.putAll(chunkResult)
		}
		// 未翻译的返回空串（调用方会跳过空结果）
		for (t in texts) {
			if (t !in result) result[t] = ""
		}
		return result
	}

	private suspend fun translateChunk(
		texts: List<String>,
		sourceLang: String,
		targetLang: String,
	): Map<String, String> = withContext(Dispatchers.IO) {
		val out = LinkedHashMap<String, String>()
		try {
			// 后端语义：from 缺省按英文处理；中文->目标语言时显式传 zh
			val from = normalizeSourceLang(sourceLang, targetLang)
			val to = normalizeTargetLang(targetLang)
			for (text in texts) {
				val payload = JSONObject()
					.put("text", text)
					.put("from_lang", from)
					.put("to_lang", to)
					.toString()
				val body = payload.toRequestBody("application/json".toMediaType())
				val request = Request.Builder()
					.url("${BuildConfig.MANGAVERSE_API_BASE_URL}/api/translate")
					.post(body)
					.build()
				httpClient.newCall(request).execute().use { resp ->
					if (resp.isSuccessful) {
						val json = JSONObject(resp.body?.string().orEmpty())
						val translated = json.optString("translated").trim()
						if (translated.isNotEmpty()) {
							out[text] = translated
						}
					} else {
						Log.w(LOG_TAG, "translate HTTP ${resp.code} for: ${text.take(20)}")
					}
				}
			}
		} catch (e: Exception) {
			Log.e(LOG_TAG, "server translate failed", e)
		}
		out
	}

	private fun normalizeSourceLang(sourceLang: String, targetLang: String): String {
		// 后端 MyMemory 需要具体语言码；auto 时按目标语言推断源语言
		val s = sourceLang.lowercase().trim()
		if (s in setOf("auto", "")) return "en"
		return s
	}

	private fun normalizeTargetLang(targetLang: String): String {
		val t = targetLang.lowercase().trim()
		return when {
			t.startsWith("zh") -> "zh"
			t.startsWith("ja") -> "ja"
			t.startsWith("ko") -> "ko"
			t.startsWith("fr") -> "fr"
			t.startsWith("de") -> "de"
			t.startsWith("ru") -> "ru"
			t.startsWith("es") -> "es"
			t.startsWith("pt") -> "pt"
			t.startsWith("it") -> "it"
			t.startsWith("th") -> "th"
			t.startsWith("vi") -> "vi"
			t.startsWith("id") -> "id"
			else -> t.substringBefore('-').ifBlank { "en" }
		}
	}
}
