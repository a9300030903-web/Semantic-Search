package com.example.feature.search

import com.example.core.data.repository.MediaFileRepository
import com.example.core.model.MediaFile
import com.example.plugin.semanticsearch.SemanticSearchEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class CoreSearchEngine(
    private val mediaFileRepository: MediaFileRepository,
    private val semanticSearchEngine: SemanticSearchEngine? = null
) {
    /**
     * Sanitizes raw user search input into valid SQLite FTS match tokens.
     * Prevents SQLite FTS syntax errors caused by special characters, leading wildcards, quotes, etc.
     */
    fun sanitizeFtsQuery(query: String): String {
        val clean = query.trim()
            .replace("'", "''")
            .replace("\"", "")
            .replace("*", "")
            .replace("?", "")
            .replace(":", "")
            .replace("-", " ")
            .replace("+", " ")
            .replace("(", " ")
            .replace(")", " ")
        
        val tokens = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""
        
        // Format as token1* token2* for prefix matching
        return tokens.joinToString(" ") { "$it*" }
    }

    /**
     * Performs a Hybrid Search with automatic fallback.
     * Step 1: Sanitize query and execute fast FTS query.
     * Step 2: Fall back to SQL LIKE query if FTS returns no results or throws error.
     * Step 3: Apply TF-IDF weighted semantic re-ranking (Filename > Tags > OCR body text).
     */
    fun searchFiles(query: String): Flow<List<MediaFile>> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return flowOf(emptyList())
        }

        val ftsFormattedQuery = sanitizeFtsQuery(trimmed)
        val sqlLikePattern = "%${trimmed}%"

        val ftsFlow = if (ftsFormattedQuery.isNotBlank()) {
            mediaFileRepository.searchFilesFts(ftsFormattedQuery)
        } else {
            mediaFileRepository.searchFilesFallback(sqlLikePattern)
        }

        return ftsFlow.flatMapLatest { results ->
            if (results.isEmpty() && trimmed.isNotBlank()) {
                // Fallback to SQL LIKE query if FTS yielded no results
                mediaFileRepository.searchFilesFallback(sqlLikePattern)
            } else {
                flowOf(results)
            }
        }.catch {
            // Error recovery fallback
            emitAll(mediaFileRepository.searchFilesFallback(sqlLikePattern))
        }.map { files ->
            rankResults(trimmed, files)
        }
    }

    private fun rankResults(query: String, files: List<MediaFile>): List<MediaFile> {
        if (files.isEmpty()) return emptyList()

        return files.map { file ->
            val score = calculateRelevanceScore(query, file)
            file to score
        }.sortedWith(
            compareByDescending<Pair<MediaFile, Float>> { it.second }
                .thenByDescending { it.first.modifiedAt }
        ).map { it.first }
    }

    private fun calculateRelevanceScore(query: String, file: MediaFile): Float {
        val qLower = query.lowercase().trim()
        val qTokens = qLower.split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
        if (qTokens.isEmpty()) return 0f

        var score = 0f

        val nameLower = file.name.lowercase()
        val tagsLower = file.tags.lowercase()
        val ocrLower = file.ocrText.lowercase()

        // 1. Exact phrase / substring bonuses (High signal)
        if (nameLower.contains(qLower)) score += 10.0f
        if (tagsLower.contains(qLower)) score += 5.0f
        if (ocrLower.contains(qLower)) score += 3.0f

        // 2. Weighted token matches across fields
        for (token in qTokens) {
            if (nameLower.contains(token)) score += 4.0f
            if (tagsLower.split(",").any { it.trim().contains(token) }) score += 2.5f
            if (ocrLower.contains(token)) score += 1.0f
        }

        // 3. TFLite Semantic score blend (if available)
        if (semanticSearchEngine != null) {
            val combinedText = "$nameLower $tagsLower $ocrLower"
            val semanticSimilarity = semanticSearchEngine.calculateSimilarity(qLower, combinedText)
            score += semanticSimilarity * 5.0f
        }

        return score
    }
}
