package com.example.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloud_sync_tasks")
data class CloudSyncTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileId: Int, // The local MediaFile ID
    val action: String, // "UPLOAD" or "DELETE"
    val providerId: String, // e.g. "google_drive"
    val cloudFileId: String?, // Drive file ID if resuming/updating
    val status: String, // "PENDING", "IN_PROGRESS", "FAILED"
    val bytesTransferred: Long = 0,
    val totalBytes: Long = 0,
    val uploadSessionUri: String? = null, // For resumable uploads
    val retryCount: Int = 0,
    val lastAttempt: Long = 0,
    val errorMessage: String? = null
)
