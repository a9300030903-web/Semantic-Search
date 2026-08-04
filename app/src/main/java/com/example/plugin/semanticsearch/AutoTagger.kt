package com.example.plugin.semanticsearch

import com.example.core.data.network.GeminiService
import com.example.core.model.MediaFile

/**
 * Phase 10: AI Intelligence - Auto Tags and Smart Categories using Gemini AI API & Local Engine
 */
class AutoTagger(private val semanticSearchEngine: SemanticSearchEngine) {

    // Pre-defined broad categories
    private val standardCategories = listOf(
        "Invoices", "Receipts", "Personal Identity", "Family Photos", "Screenshots", "Memes", "Work Documents", "Media"
    )

    var isCloudTaggingEnabled: Boolean = false

    /**
     * Suggests a category based on the extracted text and filename.
     */
    fun suggestCategory(filename: String, extractedText: String): String {
        val combinedContext = "$filename $extractedText"
        
        var bestCategory = "Uncategorized"
        var highestScore = 0f

        for (category in standardCategories) {
            val score = semanticSearchEngine.calculateSimilarity(category, combinedContext)
            if (score > highestScore && score > 0.3f) {
                highestScore = score
                bestCategory = category
            }
        }
        
        return bestCategory
    }

    /**
     * Optional cloud-enhanced tagging. Requires internet + user-provided API key. The
     * app must remain fully functional for tagging via generateTags()/suggestCategory()
     * without this.
     */
    suspend fun generateTagsWithGemini(file: MediaFile, geminiService: GeminiService): String {
        return geminiService.generateAutoTags(
            fileName = file.name,
            fileType = file.type,
            mimeType = file.mimeType,
            ocrText = file.ocrText
        )
    }

    /**
     * Extracts comma-separated tags based on keyword frequency or semantic mapping.
     */
    fun generateTags(extractedText: String): String {
        val words = extractedText.split(Regex("\\s+")).filter { it.length > 5 }
        return words.take(5).joinToString(", ")
    }
}

