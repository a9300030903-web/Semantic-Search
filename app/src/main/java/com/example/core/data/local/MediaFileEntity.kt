package com.example.core.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.MediaFile

@Entity(
    tableName = "media_files",
    indices = [
        Index(value = ["isEncrypted"]),
        Index(value = ["modifiedAt"])
    ]
)
data class MediaFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val path: String,
    val type: String,
    val mimeType: String,
    val size: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val tags: String = "",
    val ocrText: String = "",
    val semanticEmbedding: String = ""
) {
    fun toDomain(): MediaFile = MediaFile(
        id = id,
        name = name,
        path = path,
        type = type,
        mimeType = mimeType,
        size = size,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        isEncrypted = isEncrypted,
        tags = tags,
        ocrText = ocrText,
        semanticEmbedding = semanticEmbedding
    )

    companion object {
        fun fromDomain(domain: MediaFile): MediaFileEntity = MediaFileEntity(
            id = domain.id,
            name = domain.name,
            path = domain.path,
            type = domain.type,
            mimeType = domain.mimeType,
            size = domain.size,
            createdAt = domain.createdAt,
            modifiedAt = domain.modifiedAt,
            isEncrypted = domain.isEncrypted,
            tags = domain.tags,
            ocrText = domain.ocrText,
            semanticEmbedding = domain.semanticEmbedding
        )
    }
}
