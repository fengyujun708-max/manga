package com.mangaverse.app.reader.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.mangaverse.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 超分辨率管理器 —— 改为调用后端 /api/enhance（模型在服务器，客户端零内置）。
 * 保留原有接口签名（processImage / release），调用方无需改动。
 */
@Singleton
class ReaderSuperResolutionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val TAG = "ReaderSuperResolutionManager"
    private val engineMutex = Mutex()
    private val cacheDir = File(context.cacheDir, "sr_cache").apply { mkdirs() }

    private var activeModelId: String? = null

    // 与旧 ncnn 路径保持一致的失败阈值语义（连续失败后暂停，重启 App 再试）
    private var consecutiveFailures = 0
    private val MAX_CONSECUTIVE_FAILURES = 3

    // 输入像素上限：4x 超分输出巨大，防止 OOM（沿用原限制）
    private val MAX_INPUT_PIXELS_ESRGAN = 1500L * 2100L  // ~3.15M px
    private val MAX_INPUT_PIXELS_CUGAN = 3000L * 4200L   // ~12.6M px

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // 服务器超分可能较慢
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun processImage(
        originalUri: Uri,
        modelId: String,
        noiseLevel: Int,
        cacheLimitMb: Int
    ): Uri? = withContext(Dispatchers.IO) {
        if (originalUri.scheme != "file") {
            return@withContext null
        }
        val originalFile = originalUri.toFile()
        if (!originalFile.exists()) return@withContext null

        val hash = "${originalFile.name}_${modelId}_sr".hashCode().toString()
        val outputFile = File(cacheDir, "sr_$hash.webp")

        if (outputFile.exists() && outputFile.length() > 0) {
            Log.d(TAG, "Using cached SR image: ${outputFile.name}")
            updateCacheLru(outputFile)
            return@withContext outputFile.toUri()
        }

        // 连续失败后暂停，避免反复请求拖慢阅读
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            Log.w(TAG, "SR engine disabled: $consecutiveFailures consecutive failures. " +
                    "Restart app to retry.")
            return@withContext null
        }

        Log.d(TAG, "Starting SR via server API for ${originalFile.name} with model $modelId")

        val resultBitmap: Bitmap? = try {
            engineMutex.withLock {
                if (!isActive) return@withLock null

                val isEsrgan = modelId.contains("realesrgan", ignoreCase = true)

                // 尺寸限制检查（沿用原逻辑，防止服务器返回超大图）
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(originalFile.absolutePath, boundsOpts)
                val inputPixels = boundsOpts.outWidth.toLong() * boundsOpts.outHeight.toLong()
                val maxPixels = if (isEsrgan) MAX_INPUT_PIXELS_ESRGAN else MAX_INPUT_PIXELS_CUGAN
                if (inputPixels > maxPixels) {
                    Log.d(TAG, "Skipping SR: input ${boundsOpts.outWidth}x${boundsOpts.outHeight} " +
                            "(${inputPixels / 1_000_000}M px) exceeds limit")
                    return@withLock null
                }

                // 解码为 PNG bytes 上传
                val decodeOpts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val originalBitmap = BitmapFactory.decodeFile(originalFile.absolutePath, decodeOpts)
                    ?: return@withLock null
                val pngBytes = java.io.ByteArrayOutputStream().use { bos ->
                    originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
                    originalBitmap.recycle()
                    bos.toByteArray()
                }

                // 调后端 /api/enhance
                val scale = if (isEsrgan) 4 else 2
                val enhancedBytes = callEnhanceApi(pngBytes, scale) ?: return@withLock null
                BitmapFactory.decodeByteArray(enhancedBytes, 0, enhancedBytes.size)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Server SR processing failed", e)
            consecutiveFailures++
            null
        }

        if (resultBitmap != null) {
            try {
                FileOutputStream(outputFile).use { out ->
                    resultBitmap.compress(Bitmap.CompressFormat.WEBP, 90, out)
                }
                consecutiveFailures = 0
                manageCache(cacheLimitMb)
                return@withContext outputFile.toUri()
            } catch (e: Exception) {
                Log.e(TAG, "Server SR Processing Save failed", e)
                outputFile.delete()
            } finally {
                resultBitmap.recycle()
            }
        }

        null
    }

    /** 调用后端超分接口 */
    private suspend fun callEnhanceApi(pngBytes: ByteArray, scale: Int): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "image", "input.png",
                        pngBytes.toRequestBody("image/png".toMediaType()),
                    )
                    .addFormDataPart("scale", scale.toString())
                    .build()
                val request = Request.Builder()
                    .url("${BuildConfig.MANGAVERSE_API_BASE_URL}/api/enhance")
                    .post(body)
                    .build()
                httpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.e(TAG, "enhance API HTTP ${resp.code}")
                        null
                    } else {
                        resp.body?.bytes()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "enhance API call failed", e)
                null
            }
        }
    }

    private fun updateCacheLru(file: File) {
        file.setLastModified(System.currentTimeMillis())
    }

    private fun manageCache(limitMb: Int) {
        if (limitMb < 0) return
        val limitBytes = limitMb * 1024L * 1024L
        val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var totalSize = files.sumOf { it.length() }

        for (file in files) {
            if (totalSize <= limitBytes) break
            totalSize -= file.length()
            file.delete()
        }
    }

    fun release() {
        GlobalScope.launch(Dispatchers.IO) {
            engineMutex.withLock {
                activeModelId = null
                consecutiveFailures = 0
            }
        }
    }
}
