package com.baseerat.vision.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Offline vision engine.
 *
 * Current implementation uses Google ML Kit (on-device):
 * - Text Recognition (OCR)
 * - Image Labeling
 *
 * This gives a solid, fully offline baseline that works well for
 * accessibility. Architecture is ready to plug a real local VLM later
 * (e.g. llama.cpp Android / ONNX vision model).
 */
class VisionEngine(private val context: Context) {

    data class Result(
        val summary: String,
        val labels: List<String>,
        val fullText: String,
        val detailed: String
    )

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val imageLabeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.6f)
                .build()
        )
    }

    suspend fun describe(bitmap: Bitmap, detailed: Boolean = false, language: String = "en"): Result =
        withContext(Dispatchers.Default) {
            val image = InputImage.fromBitmap(bitmap, 0)

            // Run OCR + Labeling in parallel conceptually (sequential for simplicity & battery)
            val textResult = runCatching { textRecognizer.process(image).await() }.getOrNull()
            val labelResult = runCatching { imageLabeler.process(image).await() }.getOrNull()

            val fullText = textResult?.text?.trim().orEmpty()
            val labels = labelResult
                ?.sortedByDescending { it.confidence }
                ?.take(8)
                ?.map { it.text }
                .orEmpty()

            buildResult(fullText, labels, detailed, language)
        }

    suspend fun describe(uri: Uri, detailed: Boolean = false, language: String = "en"): Result =
        withContext(Dispatchers.IO) {
            val stream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = stream.use { BitmapFactory.decodeStream(it) }
                ?: throw IllegalArgumentException("Cannot decode image")
            describe(bitmap, detailed, language)
        }

    private fun buildResult(
        fullText: String,
        labels: List<String>,
        detailed: Boolean,
        language: String
    ): Result {
        val isUrdu = language == "ur"

        val labelPart = if (labels.isNotEmpty()) {
            if (isUrdu) "شناخت شدہ اشیاء: ${labels.joinToString("، ")}"
            else "Detected objects: ${labels.joinToString(", ")}"
        } else ""

        val textPart = when {
            fullText.isNotBlank() -> {
                if (isUrdu) "نظر آنے والا متن: $fullText"
                else "Visible text: $fullText"
            }
            else -> if (isUrdu) "اس تصویر میں کوئی واضح متن نہیں ملا۔" else "No clear text found in the image."
        }

        val summary = buildString {
            if (labels.isNotEmpty()) {
                append(
                    if (isUrdu) "یہ تصویر ممکنہ طور پر ${labels.take(3).joinToString("، ")} دکھاتی ہے۔ "
                    else "This image likely shows ${labels.take(3).joinToString(", ")}. "
                )
            }
            if (fullText.isNotBlank()) {
                append(
                    if (isUrdu) "اس میں متن بھی موجود ہے۔ "
                    else "It also contains text. "
                )
            }
            if (isEmpty()) {
                append(
                    if (isUrdu) "تصویر کا واضح مواد پہچانا نہیں جا سکا۔"
                    else "Could not clearly identify the main content of the image."
                )
            }
        }.trim()

        val detailedText = buildString {
            appendLine(summary)
            if (labelPart.isNotBlank()) {
                appendLine()
                appendLine(labelPart)
            }
            appendLine()
            append(textPart)
        }.trim()

        return Result(
            summary = summary,
            labels = labels,
            fullText = fullText,
            detailed = if (detailed) detailedText else summary
        )
    }

    fun close() {
        runCatching { textRecognizer.close() }
        runCatching { imageLabeler.close() }
    }
}
