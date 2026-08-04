package com.example.core.data.network

import com.example.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val thinkingConfig: ThinkingConfig? = null
)

@Serializable
data class ThinkingConfig(
    val thinkingLevel: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

@Serializable
data class Candidate(
    val content: Content
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.1-pro-preview:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

/**
 * Optional cloud-enhanced tagging. Requires internet + user-provided API key. The
 * app must remain fully functional for tagging via generateTags()/suggestCategory()
 * without this.
 */
class GeminiService(private val apiService: GeminiApiService) {
    suspend fun analyzeMedia(query: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
            return@withContext "API Key not configured. Please add it to your secrets panel."
        }
        
        val request = GenerateContentRequest(
            contents = listOf(Content(
                parts = listOf(Part(text = "You are a smart media manager assistant. The user asks: $query"))
            )),
            generationConfig = GenerationConfig(
                temperature = 0.7f
            )
        )
        try {
            val response = apiService.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from Gemini."
        } catch (e: Exception) {
            "Error contacting Gemini API: ${e.message}"
        }
    }

    suspend fun generateAutoTags(
        fileName: String,
        fileType: String,
        mimeType: String,
        ocrText: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isEmpty()) {
            // Intelligent rule-based fallback when Gemini API key is not yet set
            val fallback = mutableListOf(fileType, mimeType.substringAfter("/").uppercase())
            if (fileName.contains("invoice", ignoreCase = true)) fallback.add("Financial")
            if (fileName.contains("receipt", ignoreCase = true)) fallback.add("Receipt")
            if (fileName.contains("holiday", ignoreCase = true) || fileName.contains("beach", ignoreCase = true)) fallback.add("Travel")
            if (ocrText.isNotBlank()) fallback.add("ScannedDoc")
            return@withContext fallback.distinct().joinToString(", ")
        }

        val prompt = """
            You are an AI metadata classification engine. Analyze the following media file details and generate 3 to 6 short, relevant, comma-separated tags for indexing and search.
            Respond strictly with a comma-separated list of tags without any intro, markdown, or quote marks.
            Filename: $fileName
            Type: $fileType
            MIME Type: $mimeType
            OCR Context: ${ocrText.take(200)}
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(
                parts = listOf(Part(text = prompt))
            )),
            generationConfig = GenerationConfig(
                temperature = 0.2f
            )
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val result = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!result.isNullOrBlank()) {
                result.replace("\n", "").removeSurrounding("\"")
            } else {
                "$fileType, $mimeType"
            }
        } catch (e: Exception) {
            val fallback = mutableListOf(fileType, mimeType.substringAfter("/").uppercase())
            if (ocrText.isNotBlank()) fallback.add("ScannedDoc")
            fallback.distinct().joinToString(", ")
        }
    }
}
