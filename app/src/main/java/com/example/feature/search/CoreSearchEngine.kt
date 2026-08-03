package com.example.feature.search

import com.example.core.data.repository.MediaFileRepository
import com.example.core.model.MediaFile
import com.example.plugin.semanticsearch.SemanticSearchEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CoreSearchEngine(
    private val mediaFileRepository: MediaFileRepository,
    private val semanticSearchEngine: SemanticSearchEngine? = null
) {
    /**
     * Performs a Hybrid Search.
     * Step 1: Pre-filter using blazing-fast Full Text Search (FTS4).
     * Step 2: If Semantic Search is available, re-rank the results based on similarity.
     */
    fun searchFiles(query: String): Flow<List<MediaFile>> {
        val ftsQuery = "*${query}*"
        return mediaFileRepository.searchFilesFts(ftsQuery).map { files ->
            if (semanticSearchEngine != null && files.isNotEmpty()) {
                // Re-rank based on semantic similarity
                files.sortedByDescending { file ->
                    val combinedText = "${file.name} ${file.tags} ${file.ocrText}"
                    semanticSearchEngine.calculateSimilarity(query, combinedText)
                }
            } else {
                files
            }
        }
    }
}
