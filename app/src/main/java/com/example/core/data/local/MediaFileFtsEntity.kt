package com.example.core.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = MediaFileEntity::class)
@Entity(tableName = "media_files_fts")
data class MediaFileFtsEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "tags") val tags: String,
    @ColumnInfo(name = "ocrText") val ocrText: String
)
