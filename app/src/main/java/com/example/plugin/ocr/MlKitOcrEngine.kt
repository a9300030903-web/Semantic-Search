package com.example.plugin.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.coroutines.tasks.await
import java.io.IOException

class MlKitOcrEngine(private val context: Context) : OcrEngine {
    
    private val recognizer by lazy { TextRecognition.getClient() }

    override fun isAvailable(): Boolean {
        return true
    }

    override suspend fun extractTextFromImage(uri: Uri): Result<String> {
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
