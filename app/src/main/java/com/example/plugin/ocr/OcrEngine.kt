package com.example.plugin.ocr

import android.net.Uri

interface OcrEngine {
    /**
     * Checks if the OCR module is currently available/downloaded.
     */
    fun isAvailable(): Boolean

    /**
     * Extracts text from an image URI.
     */
    suspend fun extractTextFromImage(uri: Uri): Result<String>
}
