package com.example.plugin.semanticsearch

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.core.model.MediaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Phase 10: Duplicate Cleaner Level 3 & 4
 */
class DeepDuplicateCleaner(private val semanticSearchEngine: SemanticSearchEngine) {

    /**
     * Level 3a: Metadata-based similarity grouping.
     *
     * Groups files that look similar based on normalized filename and content
     * metadata (OCR text / tags). This is NOT real visual/pixel comparison --
     * see findVisualDuplicatesReal() below for actual image-content matching.
     * Useful as a fast pre-filter or fallback when files have no decodable
     * image data (e.g. documents, or images that fail to decode).
     */
    fun findMetadataSimilarDuplicates(files: List<MediaFile>, similarityThreshold: Float = 0.85f): Map<String, List<MediaFile>> {
        if (files.isEmpty()) return emptyMap()

        val normalizedThreshold = similarityThreshold.coerceIn(0f, 1f)
        val groups = mutableListOf<MutableList<MediaFile>>()

        files.forEach { file ->
            val matchingGroup = groups.firstOrNull { group ->
                val representative = group.first()
                val similarity = calculateMetadataSimilarity(file, representative)
                similarity >= normalizedThreshold
            }

            if (matchingGroup != null) {
                matchingGroup.add(file)
            } else {
                groups.add(mutableListOf(file))
            }
        }

        return groups.mapIndexed { index, group -> "group_$index" to group }.toMap()
    }

    private fun calculateMetadataSimilarity(left: MediaFile, right: MediaFile): Float {
        val nameSimilarity = jaccardSimilarity(normalize(left.name), normalize(right.name))
        val contentSimilarity = jaccardSimilarity(
            normalize("${left.ocrText} ${left.tags}"),
            normalize("${right.ocrText} ${right.tags}")
        )

        return ((nameSimilarity * 0.65f) + (contentSimilarity * 0.35f)).coerceIn(0f, 1f)
    }

    private fun normalize(value: String): String {
        return value.lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    private fun jaccardSimilarity(left: String, right: String): Float {
        if (left.isBlank() || right.isBlank()) return 0f

        val leftTokens = left.split(Regex("\\s+")).toSet()
        val rightTokens = right.split(Regex("\\s+")).toSet()
        val intersection = leftTokens.intersect(rightTokens).size
        val union = leftTokens.union(rightTokens).size

        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    /**
     * Level 3b: Real Visual Similarity via difference hash (dHash).
     *
     * Reads actual image bytes and compares visual content using a 64-bit
     * perceptual hash, so it catches near-identical photos (burst shots,
     * re-saved/re-compressed copies, screenshots of the same image)
     * regardless of filename or metadata. This is the real Level 3 check;
     * findMetadataSimilarDuplicates() above is a separate, faster fallback.
     *
     * Runs on Dispatchers.Default, is cancellation-aware, and downsamples
     * images before decoding to avoid OOM on large photos.
     */
    suspend fun findVisualDuplicatesReal(
        files: List<MediaFile>,
        similarityThreshold: Float = 0.90f
    ): Map<String, List<MediaFile>> = withContext(Dispatchers.Default) {
        val imageFiles = files.filter { it.mimeType.startsWith("image/") }
        if (imageFiles.size < 2) return@withContext emptyMap()

        // Step 1: compute a 64-bit dHash per image. Skip files that fail to
        // decode (corrupt/unsupported) instead of crashing the whole scan.
        val hashes = mutableMapOf<String, Long>() // fileId -> hash
        for (file in imageFiles) {
            ensureActive()
            val hash = try {
                computeDHash(file.path)
            } catch (e: Exception) {
                null
            }
            if (hash != null) hashes[file.id.toString()] = hash
        }

        // Step 2: map similarityThreshold (0.70f loose -> 0.95f near-exact)
        // onto a maximum allowed Hamming distance out of 64 bits.
        val maxHammingDistance = ((1f - similarityThreshold.coerceIn(0f, 1f)) * 64).toInt()
            .coerceIn(0, 64)

        // Step 3: group by Hamming distance using union-find, with the same
        // O(N^2)-with-cap safeguard used in findSemanticDuplicates().
        val validEntries = hashes.entries.toList()
        val maxComparisons = 2000
        var comparisons = 0
        val parent = validEntries.indices.toMutableList()

        fun find(x: Int): Int {
            var root = x
            while (parent[root] != root) root = parent[root]
            return root
        }
        fun union(a: Int, b: Int) {
            val ra = find(a); val rb = find(b)
            if (ra != rb) parent[ra] = rb
        }

        outer@ for (i in validEntries.indices) {
            ensureActive()
            for (j in i + 1 until validEntries.size) {
                ensureActive()
                if (comparisons++ > maxComparisons) break@outer
                val distance = java.lang.Long.bitCount(validEntries[i].value xor validEntries[j].value)
                if (distance <= maxHammingDistance) {
                    union(i, j)
                }
            }
        }

        // Step 4: materialize groups, only keep groups with 2+ members.
        val groupsByRoot = mutableMapOf<Int, MutableList<MediaFile>>()
        val fileById = imageFiles.associateBy { it.id.toString() }
        validEntries.forEachIndexed { index, entry ->
            val root = find(index)
            val file = fileById[entry.key] ?: return@forEachIndexed
            groupsByRoot.getOrPut(root) { mutableListOf() }.add(file)
        }

        return@withContext groupsByRoot.values
            .filter { it.size >= 2 }
            .mapIndexed { idx, group -> "visual_group_$idx" to group }
            .toMap()
    }

    /**
     * Computes a 64-bit difference hash (dHash) for an image file.
     * Downscale to 9x8 grayscale, compare each pixel to its right neighbor
     * -> 8x8 = 64 bits, 1 if pixel is brighter than its neighbor. Resilient
     * to resizing and mild recompression, sensitive to real visual content.
     */
    private fun computeDHash(filePath: String): Long? {
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) return null

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(filePath, boundsOptions)
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > 200 || boundsOptions.outHeight / sampleSize > 200) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val source = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return null

        val small = try {
            Bitmap.createScaledBitmap(source, 9, 8, true)
        } finally {
            if (!source.isRecycled) source.recycle()
        }

        var hash = 0L
        var bitIndex = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val leftGray = grayscale(small.getPixel(x, y))
                val rightGray = grayscale(small.getPixel(x + 1, y))
                if (leftGray > rightGray) {
                    hash = hash or (1L shl bitIndex)
                }
                bitIndex++
            }
        }
        small.recycle()
        return hash
    }

    private fun grayscale(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    /**
     * Level 4: Semantic AI Duplicate Detection
     * Finds files that mean the same thing (e.g., two different scans of the same invoice).
     *
     * Audited for ANRs, Cancellation, and O(N^2) Memory / Thread Starvation.
     * @param similarityThreshold Slider value from Blueprint: 70% loose (0.7f) -> 95% near-exact (0.95f)
     */
    suspend fun findSemanticDuplicates(
        files: List<MediaFile>,
        similarityThreshold: Float = 0.85f
    ): List<Pair<MediaFile, MediaFile>> = withContext(Dispatchers.Default) {
        val duplicates = mutableListOf<Pair<MediaFile, MediaFile>>()

        val validFiles = files.filter { it.ocrText.isNotBlank() || it.tags.isNotBlank() }
        val maxComparisons = 500 // Cap comparisons to safeguard scalability on huge directories

        var comparisons = 0
        outer@ for (i in 0 until validFiles.size - 1) {
            ensureActive()
            for (j in i + 1 until validFiles.size) {
                ensureActive()
                if (comparisons++ > maxComparisons) break@outer

                val f1 = validFiles[i]
                val f2 = validFiles[j]

                val text1 = "${f1.name} ${f1.ocrText}"
                val text2 = "${f2.name} ${f2.ocrText}"

                val similarity = semanticSearchEngine.calculateSimilarity(text1, text2)
                if (similarity >= similarityThreshold) {
                    duplicates.add(Pair(f1, f2))
                }
            }
        }

        return@withContext duplicates
    }
}
