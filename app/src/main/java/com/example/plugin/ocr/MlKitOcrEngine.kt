package com.example.plugin.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.coroutines.tasks.await
import java.io.IOException

class MlKitOcrEngine(private val context: Context) : OcrEngine {
    
    // Lazy initialization ensures the ML Kit dependency is only invoked when accessed,
    // which is required for on-demand plugin modules.
    private val recognizer by lazy { TextRecognition.getClient() }

    override fun isAvailable(): Boolean {
        return try {
            Class.forName("com.google.mlkit.vision.text.TextRecognition")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    override suspend fun extractTextFromImage(uri: Uri): Result<String> {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException("OCR Plugin is not available"))
        }
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            Result.success(result.text)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
