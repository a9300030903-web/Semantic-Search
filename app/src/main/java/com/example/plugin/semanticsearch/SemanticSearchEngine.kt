package com.example.plugin.semanticsearch

import android.content.Context
import android.util.LruCache
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Semantic Search Engine using TFLite embedding & Cosine / TF-IDF similarity.
 */
class SemanticSearchEngine(private val context: Context) {

    private var interpreter: Interpreter? = null
    
    // Cache for embeddings to avoid recomputing for the same text
    private val embeddingCache = LruCache<String, FloatArray>(1000)

    fun initialize() {
        try {
            val assetFileDescriptor = context.assets.openFd("sentence_encoder.tflite")
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val mappedByteBuffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            
            interpreter = Interpreter(mappedByteBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
            interpreter = null
        }
    }

    /**
     * Embeds a string into a float array using the TFLite model.
     */
    fun embed(text: String): FloatArray {
        interpreter?.let { tflite ->
            // Use cache if available
            embeddingCache.get(text)?.let { return it }

            // Dummy implementation of sentence embedding using a MiniLM-style model
            val outputArray = Array(1) { FloatArray(384) }
            try {
                tflite.run(arrayOf(text), outputArray)
                val embedding = outputArray[0]
                embeddingCache.put(text, embedding)
                return embedding
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return FloatArray(0)
    }

    /**
     * Calculates semantic similarity between query and target text.
     */
    fun calculateSimilarity(query: String, targetText: String): Float {
        val qEmbed = embed(query)
        val tEmbed = embed(targetText)

        val semanticScore = if (qEmbed.isNotEmpty() && tEmbed.isNotEmpty()) {
            cosineSimilarity(qEmbed, tEmbed)
        } else {
            0f
        }

        val lexicalScore = calculateLexicalSimilarity(query, targetText)

        return if (interpreter != null) {
            // Blend both scores: 70% semantic, 30% lexical
            (semanticScore * 0.7f) + (lexicalScore * 0.3f)
        } else {
            lexicalScore
        }
    }
    
    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        return if (normA > 0 && normB > 0) {
            dotProduct / (sqrt(normA) * sqrt(normB))
        } else {
            0f
        }
    }

    /**
     * Calculates lexical similarity between query and target text.
     * Uses TF-IDF weighted overlap to avoid penalizing longer documents.
     */
    fun calculateLexicalSimilarity(query: String, targetText: String): Float {
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
