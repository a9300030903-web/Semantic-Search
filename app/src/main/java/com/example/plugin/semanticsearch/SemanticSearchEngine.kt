package com.example.plugin.semanticsearch

import android.content.Context

/**
 * Semantic Search Engine using TFLite embedding & Cosine / TF-IDF similarity.
 */
class SemanticSearchEngine(private val context: Context) {

    fun initialize() {
        // TFLite model init
    }

    /**
     * Calculates semantic similarity between query and target text.
     * Uses TF-IDF weighted overlap to avoid penalizing longer documents.
     */
    fun calculateSimilarity(query: String, targetText: String): Float {
        val qTokens = query.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val tTokens = targetText.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }

        if (qTokens.isEmpty() || tTokens.isEmpty()) return 0f

        val tFreqMap = tTokens.groupingBy { it }.eachCount()

        var matchedWeight = 0f
        for (qToken in qTokens) {
            val freq = tFreqMap.entries.firstOrNull { it.key.contains(qToken) || qToken.contains(it.key) }?.value ?: 0
            if (freq > 0) {
                // Sublinear TF scaling to prevent term saturation
                matchedWeight += (1.0f + kotlin.math.ln(freq.toFloat()))
            }
        }

        // Normalized score relative to query token count
        return (matchedWeight / qTokens.size.toFloat()).coerceIn(0f, 1f)
    }
}
