package com.example.service

import android.content.Context
import com.example.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class CloudGeminiScanner(private val context: Context) {
    
    // Using gemini-3.1-pro-preview as gemini-1.5-pro is prohibited per system instructions.
    private val model = GenerativeModel(
        modelName = "gemini-3.1-pro-preview",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    /**
     * Downloads the PDF from the provided URL and analyzes it using Gemini.
     */
    suspend fun analyzePdf(pdfUrl: String, dtc: String): String = withContext(Dispatchers.IO) {
        try {
            // 1. Download PDF to ByteArray (Blob)
            val pdfData = URL(pdfUrl).readBytes()
            
            // 2. Pass to Gemini
            val response = model.generateContent(
                content {
                    blob("application/pdf", pdfData)
                    text("Analyze the following PDF manual and provide troubleshooting steps for DTC $dtc.")
                }
            )
            
            response.text ?: "No response from AI"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
