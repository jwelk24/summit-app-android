package com.summit.android.service

import android.content.Context
import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ReceiptLineItem(
    val name: String,
    val amount: Double
)

@Serializable
data class ReceiptDraft(
    val merchant: String,
    val date: String,
    val lineItems: List<ReceiptLineItem>,
    val subtotal: Double,
    val tax: Double,
    val tip: Double,
    val total: Double,
    val currencyCode: String
)

class ReceiptScanner(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "YOUR_GEMINI_API_KEY" // Placeholder
    )
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun scan(bitmap: Bitmap): ReceiptDraft? {
        val image = InputImage.fromBitmap(bitmap, 0)
        val visionText = recognizer.process(image).await()
        val rawText = visionText.text
        
        if (rawText.isBlank()) return null
        
        return parse(rawText)
    }

    private suspend fun parse(rawText: String): ReceiptDraft? {
        val prompt = """
            Convert this OCR'd receipt text into a structured JSON object.
            Return ONLY a JSON object with: 'merchant', 'date' (YYYY-MM-DD), 'lineItems' (list of name/amount), 'subtotal', 'tax', 'tip', 'total', and 'currencyCode'.
            
            Text:
            $rawText
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            val jsonText = response.text?.substringAfter("{")?.substringBeforeLast("}")?.let { "{$it}" }
            jsonText?.let { json.decodeFromString<ReceiptDraft>(it) }
        } catch (e: Exception) {
            null
        }
    }
}
