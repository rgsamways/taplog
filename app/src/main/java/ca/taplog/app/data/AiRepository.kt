package ca.taplog.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import ca.taplog.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

data class AssetIdentificationResult(
    val code: String?,
    val confidence: String,
    val reasoning: String
)

class AiRepository {

    private val client = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun identifyAsset(
        imagePath: String,
        vertical: VerticalConfig
    ): AssetIdentificationResult? = withContext(Dispatchers.IO) {
        val file = File(imagePath)
        try {
            val base64Image = encodeImageToBase64(file) ?: return@withContext null
            val prompt = buildPrompt(vertical)

            val requestBody = JSONObject().apply {
                put("model", "claude-sonnet-4-6")
                put("max_tokens", 256)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "image")
                                put("source", JSONObject().apply {
                                    put("type", "base64")
                                    put("media_type", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", prompt)
                            })
                        })
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            parseResponse(body)
        } catch (_: Exception) {
            null
        } finally {
            file.delete()
        }
    }

    private fun encodeImageToBase64(file: File): String? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)

            val maxDim = 1024
            val scale = maxOf(
                options.outWidth.toFloat() / maxDim,
                options.outHeight.toFloat() / maxDim,
                1f
            )
            val sampleSize = scale.toInt().coerceAtLeast(1)

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                ?: return null

            val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                val w = (bitmap.width * ratio).toInt()
                val h = (bitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, w, h, true).also {
                    if (it !== bitmap) bitmap.recycle()
                }
            } else bitmap

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            scaled.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildPrompt(vertical: VerticalConfig): String {
        val typeList = vertical.assetTypeRegistry.joinToString("\n") {
            "- ${it.code}: ${it.label} — ${it.description}"
        }
        return """
You are identifying a physical asset in a regulated trades inspection app.

The inspector is registering an asset in the ${vertical.displayName} vertical.
The available asset types are:
$typeList

Look at this photo and identify which asset type best matches what you see.
Respond with a JSON object only:
{
  "code": "ASSET_TYPE_CODE",
  "confidence": "HIGH" | "MEDIUM" | "LOW",
  "reasoning": "one sentence"
}

If you cannot identify the asset with at least MEDIUM confidence, set code to null.
        """.trimIndent()
    }

    private fun parseResponse(body: String): AssetIdentificationResult? {
        return try {
            val root = JSONObject(body)
            val text = root.getJSONArray("content").getJSONObject(0).getString("text")
            // Strip markdown code fences if present
            val clean = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val result = JSONObject(clean)
            AssetIdentificationResult(
                code = if (result.isNull("code")) null else result.getString("code"),
                confidence = result.optString("confidence", "LOW"),
                reasoning = result.optString("reasoning", "")
            )
        } catch (_: Exception) {
            null
        }
    }
}
