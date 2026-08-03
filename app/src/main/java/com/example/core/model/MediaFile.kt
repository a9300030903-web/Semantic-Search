package com.example.core.model

/**
 * Pure Domain Entity representing a Media File.
 * Free of ORM (Room) or serialization framework annotations.
 */
data class MediaFile(
    val id: Int = 0,
    val name: String,
    val path: String,
    val type: String, // e.g., "Video", "Image", "Document"
    val mimeType: String,
    val size: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val tags: String = "", // Comma-separated or JSON list of tags
    val ocrText: String = "", // Extracted text if available
    val semanticEmbedding: String = "" // Placeholder for TFLite embeddings
)
