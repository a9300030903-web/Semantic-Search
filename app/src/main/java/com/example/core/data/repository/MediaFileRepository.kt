package com.example.core.data.repository

import com.example.core.model.MediaFile
import kotlinx.coroutines.flow.Flow

interface MediaFileRepository {
    fun getAllFiles(): Flow<List<MediaFile>>
    fun getVaultFiles(): Flow<List<MediaFile>>
    fun searchFilesFts(query: String): Flow<List<MediaFile>>
    fun searchFilesFallback(queryPattern: String): Flow<List<MediaFile>>
    suspend fun getFileById(id: Int): MediaFile?
    suspend fun insertFile(file: MediaFile): Long
    suspend fun insertFiles(files: List<MediaFile>)
    suspend fun updateFile(file: MediaFile)
    suspend fun deleteFileById(id: Int)
}
