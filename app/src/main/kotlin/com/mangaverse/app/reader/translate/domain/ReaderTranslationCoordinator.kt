package com.mangaverse.app.reader.translate.domain

import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import com.mangaverse.app.core.prefs.AppSettings
import com.mangaverse.app.core.prefs.ReaderTranslationMode
import com.mangaverse.app.core.util.ext.awaitCancellable
import com.mangaverse.app.core.util.ext.printStackTraceDebug
import com.mangaverse.app.reader.translate.data.ReaderTranslationTextCache

internal class ReaderTranslationCoordinator(
	private val settings: AppSettings,
	private val textCache: ReaderTranslationTextCache,
	private val onnxTranslationEngine: OnnxReaderTranslationEngine,
	private val okHttpClient: OkHttpClient,
	private val jsonMediaType: MediaType,
	private val defaultOpenAiModel: String,
	private val openAiTranslationSystemPrompt: String,
	private val maxOpenAiBatchSize: Int,
	private val thinkTagRegex: Regex,
	private val buildTextCacheKey: (String, String, String) -> String,
	private val sanitizeTranslation: (String) -> String,
	private val isAcceptableTranslation: (String, String, String, String) -> Boolean,
	private val log: (() -> String) -> Unit,
	private val oneLine: (String, Int) -> String,
) {

	suspend fun translateBlocksCached(
		texts: List<String>,
		sourceLang: String,
		targetLang: String,
	): Map<String, String> {
		if (texts.isEmpty()) return emptyMap()
		val uniqueTexts = texts.distinct()
		val translated = LinkedHashMap<String, String>(uniqueTexts.size)
		val misses = ArrayList<String>(uniqueTexts.size)

		for (text in uniqueTexts) {
			val cacheKey = buildTextCacheKey(text, sourceLang, targetLang)
			val cached = textCache[cacheKey]
			if (!cached.isNullOrBlank()) {
				val sanitized = sanitizeTranslation(cached)
				if (sanitized.isNotBlank()) {
					translated[text] = sanitized
					if (sanitized != cached) {
						textCache[cacheKey] = sanitized
					}
					log { "translate cache hit src=${oneLine(text, 140)} out=${oneLine(sanitized, 140)}" }
				} else {
					textCache[cacheKey] = ""
					misses.add(text)
					log { "translate cache rejected src=${oneLine(text, 140)} out=${oneLine(sanitized, 140)}" }
				}
			} else {
				misses.add(text)
			}
		}
		if (misses.isEmpty()) return translated

		val resolvedSourceLang = if (sourceLang.trim().lowercase() == "auto") {
			val sampleText = misses.filter { it.isNotBlank() }.joinToString("\n").take(500)
			if (sampleText.isNotBlank()) {
				detectLanguage(sampleText) ?: "en"
			} else {
				"en"
			}
		} else {
			sourceLang
		}
		android.util.Log.d("ReaderTranslationCoordinator", "translateBlocksCached: resolved source language to $resolvedSourceLang")

		val mode = settings.readerTranslationMode
		val onnxModelId = settings.readerTranslationOnnxModelId.trim()
		android.util.Log.d("ReaderTranslationCoordinator", "translateBlocksCached: mode=$mode, onnxModelId='$onnxModelId', misses.size=${misses.size}")
		if (mode != ReaderTranslationMode.API_ONLY && onnxModelId.isNotBlank()) {
			val needOnnx = misses.filter { translated[it].isNullOrBlank() }
			android.util.Log.d("ReaderTranslationCoordinator", "translateBlocksCached: calling ONNX for ${needOnnx.size} texts")
			if (needOnnx.isNotEmpty()) {
				val onnxMap = runCatching {
					onnxTranslationEngine.translateBatch(needOnnx, resolvedSourceLang, targetLang, onnxModelId)
				}.onFailure {
					if (it is kotlinx.coroutines.CancellationException) throw it
					it.printStackTraceDebug()
					log { "translate onnx failed: ${it.message.orEmpty()}" }
					android.util.Log.e("ReaderTranslationCoordinator", "translateBlocksCached: ONNX failed: ${it.message}", it)
				}.getOrDefault(emptyMap())
				android.util.Log.d("ReaderTranslationCoordinator", "translateBlocksCached: ONNX returned ${onnxMap.size} results")
				for (text in needOnnx) {
					val onnxText = onnxMap[text]?.trim().orEmpty()
					android.util.Log.d("ReaderTranslationCoordinator", "translateBlocksCached: ONNX result for '${text.take(50)}...': '${onnxText.take(50)}...' (length=${onnxText.length})")
					if (onnxText.isNotBlank()) {
						val sanitized = sanitizeTranslation(onnxText)
						if (isAcceptableTranslation(text, sanitized, sourceLang, targetLang)) {
							translated[text] = sanitized
							textCache[buildTextCacheKey(text, sourceLang, targetLang)] = sanitized
							log { "translate onnx hit src=${oneLine(text, 140)} out=${oneLine(sanitized, 140)}" }
							android.util.Log.d("ReaderTranslationCoordinator", "translateBlocksCached: ONNX accepted")
						} else {
							log { "translate onnx rejected src=${oneLine(text, 140)} out=${oneLine(sanitized, 140)}" }
							android.util.Log.d("ReaderTranslationCoordinator", "translateBlocksCached: ONNX rejected by isAcceptableTranslation")
						}
					} else {
						android.util.Log.d("ReaderTranslationCoordinator", "translateBlocksCached: ONNX returned blank")
					}
				}
			}
		}

		if (mode != ReaderTranslationMode.API_ONLY) {
			val needLocal = misses.filter { translated[it].isNullOrBlank() }
			if (needLocal.isNotEmpty()) {  // 只有还有未翻译的文本时才用 ML Kit
				log { "translate local requested size=${needLocal.size}" }
				var localResults = runCatching {
					log { "translate local batch calling translateLocalBatch..." }
					translateLocalBatch(needLocal, resolvedSourceLang, targetLang)
				}.onFailure {
					if (it is kotlinx.coroutines.CancellationException) throw it
					it.printStackTraceDebug()
					log { "translate local batch failed: ${it.message.orEmpty()}" }
				}.getOrDefault(emptyMap())
				log { "translate local batch returned ${localResults.size} results" }
				if (needLocal.isNotEmpty() && localResults.values.none { it.isNotBlank() }) {
					log { "translate local batch empty, fallback to per-item translation" }
					localResults = coroutineScope {
						needLocal.map { text ->
							async {
								val local = runCatching {
									translateLocal(text, resolvedSourceLang, targetLang)
								}.onFailure {
									if (it is kotlinx.coroutines.CancellationException) throw it
									log { "translate local fallback failed src=${oneLine(text, 140)} err=${it.message.orEmpty()}" }
								}.getOrDefault("").trim()
								text to local
							}
						}.awaitAll().toMap()
					}
				}
				for ((text, local) in localResults) {
					val raw = local.trim()
					if (raw.isNotBlank()) {
						val sanitized = sanitizeTranslation(raw)
						if (isAcceptableTranslation(text, sanitized, sourceLang, targetLang)) {
							translated[text] = sanitized
							textCache[buildTextCacheKey(text, sourceLang, targetLang)] = sanitized
							log { "translate local hit src=${oneLine(text, 140)} out=${oneLine(sanitized, 140)}" }
						} else {
							log { "translate local rejected src=${oneLine(text, 140)} out=${oneLine(sanitized, 140)}" }
						}
					}
				}
			} else {
				log { "translate local skipped, all texts already translated by ONNX" }
			}
		}

		if (mode == ReaderTranslationMode.LOCAL_ONLY) {
			log { "translate mode=LOCAL_ONLY, skip api fallback" }
			for (text in uniqueTexts) {
				translated.putIfAbsent(text, "")
			}
			return translated
		}

		if (mode != ReaderTranslationMode.LOCAL_ONLY) {
			val needApi = misses.filter { translated[it].isNullOrBlank() }
			if (needApi.isNotEmpty()) {
				val apiMap = translateBatchByApi(needApi, resolvedSourceLang, targetLang)
				for (text in needApi) {
					val apiText = apiMap[text]?.trim().orEmpty()
					if (apiText.isNotBlank()) {
						val sanitized = sanitizeTranslation(apiText)
						if (sanitized.isNotBlank()) {
							translated[text] = sanitized
							textCache[buildTextCacheKey(text, sourceLang, targetLang)] = sanitized
							log { "translate api hit src=${oneLine(text, 140)} out=${oneLine(sanitized, 140)}" }
						} else {
							log { "translate api rejected src=${oneLine(text, 140)} out=${oneLine(sanitized, 140)}" }
						}
					}
				}
			}
		}

		for (text in uniqueTexts) {
			translated.putIfAbsent(text, "")
		}
		return translated
	}

	private suspend fun translateBatchByApi(
		texts: List<String>,
		sourceLang: String,
		targetLang: String,
	): Map<String, String> {
		val endpoint = resolveTranslationApiEndpoint()
		if (endpoint.isBlank() || texts.isEmpty()) {
			return texts.associateWith { "" }
		}

		return if (isOpenAiCompatibleChatCompletionsEndpoint(endpoint)) {
			translateBatchByOpenAi(texts, sourceLang, targetLang)
		} else {
			val map = LinkedHashMap<String, String>(texts.size)
			for (text in texts) {
				map[text] = translateByApi(text, sourceLang, targetLang)
			}
			map
		}
	}

	private suspend fun translateBatchByOpenAi(
		texts: List<String>,
		sourceLang: String,
		targetLang: String,
	): Map<String, String> {
		if (texts.isEmpty()) return emptyMap()
		val mapped = LinkedHashMap<String, String>(texts.size)
		val batches = buildOpenAiMicroBatches(texts)
		log { "openai batch requests count=${batches.size} texts=${texts.size}" }
		for (batch in batches) {
			if (batch.size == 1) {
				val text = batch.first()
				mapped[text] = requestOpenAiSingle(text, sourceLang, targetLang)
				continue
			}
			val batchMap = requestOpenAiBatch(batch, sourceLang, targetLang)
			if (batchMap.isEmpty()) {
				batch.forEach { text ->
					mapped[text] = requestOpenAiSingle(text, sourceLang, targetLang)
				}
				continue
			}
			for (text in batch) {
				mapped[text] = batchMap[text].orEmpty()
			}
		}
		return mapped
	}

	private suspend fun requestOpenAiBatch(
		texts: List<String>,
		sourceLang: String,
		targetLang: String,
	): Map<String, String> {
		if (texts.isEmpty()) return emptyMap()
		val endpoint = resolveTranslationApiEndpoint()
		val apiKey = settings.readerTranslationApiKey.trim()
		val model = settings.readerTranslationApiModel.trim().ifBlank { defaultOpenAiModel }
		val userPrompt = buildString {
			appendLine("Translate manga OCR text from $sourceLang to $targetLang.")
			appendLine("Return strict JSON only.")
			appendLine("Use this array format:")
			appendLine("""[{"id":1,"translation":"..."},{"id":2,"translation":"..."}]""")
			appendLine("Keep ids unchanged. If unreadable or uncertain, use empty translation.")
			appendLine()
			appendLine("Texts:")
			texts.forEachIndexed { index, text ->
				appendLine("${index + 1}. $text")
			}
		}
		val payload = JSONObject().apply {
			put("model", model)
			put("temperature", 0)
			if (isDeepSeekEndpoint(endpoint)) {
				put("thinking", JSONObject().put("type", "disabled"))
			}
			put(
				"messages",
				JSONArray()
					.put(JSONObject().put("role", "system").put("content", openAiTranslationSystemPrompt))
					.put(JSONObject().put("role", "user").put("content", userPrompt))
			)
		}
		return runCatching {
			withContext(Dispatchers.IO) {
				val requestBuilder = Request.Builder()
					.url(endpoint)
					.post(payload.toString().toRequestBody(jsonMediaType))
					.header("Content-Type", "application/json")
				TranslationApiProviderCatalog.applyAuthentication(
					requestBuilder,
					settings.readerTranslationApiProviderPreset,
					apiKey,
				)
				applyCustomHeaders(requestBuilder)
				val response = okHttpClient.newCall(requestBuilder.build()).await()
				response.use { resp ->
					val rawBody = resp.body.readJsonTextUtf8()
					if (!resp.isSuccessful) {
						log { "openai batch request failed code=${resp.code} msg=${resp.message} body=${oneLine(rawBody, 300)}" }
						return@use emptyMap()
					}
					if (rawBody.isBlank()) return@use emptyMap()
					val json = runCatching { JSONObject(rawBody) }.getOrNull() ?: return@use emptyMap()
					val content = extractOpenAiMessageContent(json).orEmpty()
					if (content.isBlank()) return@use emptyMap()
					log { "openai batch raw reply=${oneLine(content, 400)}" }
					val parsed = parseBatchTranslationJson(content, texts.size)
					if (parsed.isEmpty()) return@use emptyMap()
					LinkedHashMap<String, String>(texts.size).apply {
						texts.forEachIndexed { index, text ->
							put(text, sanitizeTranslation(parsed[index + 1].orEmpty()))
						}
					}
				}
			}
		}.onFailure {
			if (it is kotlinx.coroutines.CancellationException) throw it
			log { "openai batch request failed size=${texts.size} err=${it.message.orEmpty()}" }
		}.getOrDefault(emptyMap())
	}

	private suspend fun requestOpenAiSingle(
		text: String,
		sourceLang: String,
		targetLang: String,
	): String {
		if (text.isBlank()) return ""
		val endpoint = resolveTranslationApiEndpoint()
		val apiKey = settings.readerTranslationApiKey.trim()
		val model = settings.readerTranslationApiModel.trim().ifBlank { defaultOpenAiModel }
		val userPrompt = buildString {
			appendLine("Translate manga OCR text from $sourceLang to $targetLang.")
			appendLine("Only output the translation itself.")
			appendLine("If unreadable or uncertain, output nothing.")
			appendLine("Keep short screams natural.")
			append(text)
		}
		val payload = JSONObject().apply {
			put("model", model)
			put("temperature", 0)
			if (isDeepSeekEndpoint(endpoint)) {
				put("thinking", JSONObject().put("type", "disabled"))
			}
			put(
				"messages",
				JSONArray()
					.put(JSONObject().put("role", "system").put("content", openAiTranslationSystemPrompt))
					.put(JSONObject().put("role", "user").put("content", userPrompt))
			)
		}

		return runCatching {
			withContext(Dispatchers.IO) {
				val requestBuilder = Request.Builder()
					.url(endpoint)
					.post(payload.toString().toRequestBody(jsonMediaType))
					.header("Content-Type", "application/json")
				TranslationApiProviderCatalog.applyAuthentication(
					requestBuilder,
					settings.readerTranslationApiProviderPreset,
					apiKey,
				)
				applyCustomHeaders(requestBuilder)
				val response = okHttpClient.newCall(requestBuilder.build()).await()
				response.use { resp ->
					val rawBody = resp.body.readJsonTextUtf8()
					if (!resp.isSuccessful) {
						log { "openai request failed code=${resp.code} msg=${resp.message} body=${oneLine(rawBody, 300)}" }
						return@use ""
					}
					if (rawBody.isBlank()) return@use ""
					val json = runCatching { JSONObject(rawBody) }.getOrNull() ?: return@use ""
					val content = extractOpenAiMessageContent(json).orEmpty()
					if (content.isBlank()) return@use ""
					log { "openai raw reply=${oneLine(content, 400)}" }
					sanitizeTranslation(content)
				}
			}
		}.onFailure {
			if (it is kotlinx.coroutines.CancellationException) throw it
			log { "openai single request failed src=${oneLine(text, 140)} err=${it.message.orEmpty()}" }
		}.getOrDefault("")
	}

	private suspend fun translateLocal(text: String, sourceLang: String, targetLang: String): String {
		// 改为调后端 /api/translate（模型在服务器，客户端零内置）
		return try {
			val from = if (sourceLang.trim().lowercase() == "auto") "en" else sourceLang
			val payload = org.json.JSONObject()
				.put("text", text)
				.put("from_lang", from)
				.put("to_lang", targetLang)
				.toString()
			val request = okhttp3.Request.Builder()
				.url("${com.mangaverse.app.BuildConfig.MANGAVERSE_API_BASE_URL}/api/translate")
				.post(payload.toRequestBody(jsonMediaType))
				.build()
			withTimeout(30_000) {
				okHttpClient.newCall(request).execute().use { resp ->
					if (resp.isSuccessful) {
						org.json.JSONObject(resp.body?.string().orEmpty()).optString("translated").ifBlank { text }
					} else {
						text
					}
				}
			}
		} catch (_: Exception) {
			text
		}
	}

	private suspend fun translateLocalBatch(
		texts: List<String>,
		sourceLang: String,
		targetLang: String,
	): Map<String, String> {
		android.util.Log.d("ReaderTranslationCoordinator", "translateLocalBatch entered: texts.size=${texts.size}, source=$sourceLang, target=$targetLang")
		if (texts.isEmpty()) return emptyMap()

		// 如果源语言是 auto，先检测第一段文本的语言
		val resolvedSourceLang = if (sourceLang.trim().lowercase() == "auto") {
			android.util.Log.d("ReaderTranslationCoordinator", "translateLocalBatch detecting language...")
			val sampleText = texts.filter { it.isNotBlank() }.joinToString("\n").take(500)
			if (sampleText.isNotBlank()) {
				detectLanguage(sampleText) ?: "en"
			} else {
				"en"
			}
		} else {
			sourceLang
		}
		android.util.Log.d("ReaderTranslationCoordinator", "translateLocalBatch resolved source language: $resolvedSourceLang")

		val source = resolveMlKitLanguage(resolvedSourceLang)
		val target = resolveMlKitLanguage(targetLang)
		android.util.Log.d("ReaderTranslationCoordinator", "translateLocalBatch ML Kit languages: source=$source, target=$target")
		if (source == null || target == null) {
			log { "translate local batch skip unsupported source=$resolvedSourceLang target=$targetLang size=${texts.size}" }
			android.util.Log.w("ReaderTranslationCoordinator", "translateLocalBatch unsupported languages, returning empty")
			return texts.associateWith { "" }
		}
		android.util.Log.d("ReaderTranslationCoordinator", "translateLocalBatch calling server API...")
		val from = if (resolvedSourceLang.trim().lowercase() == "auto") "en" else resolvedSourceLang
		val results = LinkedHashMap<String, String>(texts.size)
		for (text in texts) {
			val out = translateLocal(text, from, targetLang).trim()
			results[text] = out
		}
		android.util.Log.d("ReaderTranslationCoordinator", "translateLocalBatch done translated=${results.count { it.value.isNotBlank() }}/${texts.size}")
		log { "translate local batch done translated=${results.count { it.value.isNotBlank() }}/${texts.size}" }
		return results
	}

	/**
	 * 使用 ML Kit Language Identification 检测文本语言
	 * 返回 BCP-47 语言标签（如 "en", "zh", "ja"），失败返回 null
	 */
	private suspend fun detectLanguage(text: String): String? {
		return null
	}

			if (result == "und") {
				log { "language detection undetermined for text=${oneLine(text, 100)}" }
				null
			} else {
				log { "language detected: $result for text=${oneLine(text, 100)}" }
				result
			}
		} catch (e: Exception) {
			log { "language detection failed: ${e.message.orEmpty()}" }
			null
		} finally {
			languageIdentifier.close()
		}
	}

	private fun resolveMlKitLanguage(languageTag: String): String? {
		return languageTag.trim().lowercase().substringBefore("-").ifBlank { null }
	}

