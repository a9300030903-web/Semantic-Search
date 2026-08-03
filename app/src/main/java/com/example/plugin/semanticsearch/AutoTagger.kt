package com.example.plugin.semanticsearch

/**
 * Phase 10: AI Intelligence - Auto Tags and Smart Categories
 */
class AutoTagger(private val semanticSearchEngine: SemanticSearchEngine) {

    // Pre-defined broad categories
    private val standardCategories = listOf(
        "Invoices", "Receipts", "Personal Identity", "Family Photos", "Screenshots", "Memes", "Work Documents"
    )

    /**
     * Suggests a category based on the extracted text and filename.
     */
    fun suggestCategory(filename: String, extractedText: String): String {
        val combinedContext = "$filename $extractedText"
        
        var bestCategory = "Uncategorized"
        var highestScore = 0f

        for (category in standardCategories) {
            val score = semanticSearchEngine.calculateSimilarity(category, combinedContext)
            if (score > highestScore && score > 0.3f) { // Arbitrary threshold for mockup
                highestScore = score
                bestCategory = category
            }
        }
        
        return bestCategory
    }

    /**
     * Extracts comma-separated tags based on keyword frequency or semantic mapping.
     */
    fun generateTags(extractedText: String): String {
        // Simplified mockup logic: just return some words longer than 5 chars
        val words = extractedText.split(Regex("\\s+")).filter { it.length > 5 }
        return words.take(5).joinToString(",")
    }
}
