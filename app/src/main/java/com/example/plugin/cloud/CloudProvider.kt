package com.example.plugin.cloud

import com.example.core.model.MediaFile
import kotlinx.coroutines.flow.Flow

/**
 * Phase 11: Multi-Cloud Plugin Architecture
 * Base interface that all cloud providers (Drive, OneDrive, Dropbox, NAS) must implement.
 */
interface CloudProvider {
    val providerId: String
    val providerName: String
    
    fun isAuthenticated(): Boolean
    suspend fun authenticate()
    
    suspend fun uploadFile(file: MediaFile): Result<String> // Returns cloud file ID
    suspend fun downloadFile(cloudFileId: String, destinationPath: String): Result<Unit>
    
    // Offline Queue / Sync Support
    suspend fun syncChanges(): Result<Unit>
    fun getSyncProgress(): Flow<Float>
}
