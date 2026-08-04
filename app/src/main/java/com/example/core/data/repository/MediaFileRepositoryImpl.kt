package com.example.core.data.repository

import com.example.core.data.local.MediaFileDao
import com.example.core.data.local.MediaFileEntity
import com.example.core.model.MediaFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MediaFileRepositoryImpl(
    private val mediaFileDao: MediaFileDao
) : MediaFileRepository {

    override fun getAllFiles(): Flow<List<MediaFile>> {
        return mediaFileDao.getAllFiles().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getVaultFiles(): Flow<List<MediaFile>> {
        return mediaFileDao.getVaultFiles().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchFilesFts(query: String): Flow<List<MediaFile>> {
        return mediaFileDao.searchFilesFts(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchFilesFallback(queryPattern: String): Flow<List<MediaFile>> {
        return mediaFileDao.searchFilesFallback(queryPattern).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getFileById(id: Int): MediaFile? {
        return mediaFileDao.getFileById(id)?.toDomain()
    }

    override suspend fun insertFile(file: MediaFile): Long {
        return mediaFileDao.insertFile(MediaFileEntity.fromDomain(file))
    }

    override suspend fun insertFiles(files: List<MediaFile>) {
        mediaFileDao.insertFiles(files.map { MediaFileEntity.fromDomain(it) })
    }

    override suspend fun updateFile(file: MediaFile) {
        mediaFileDao.updateFile(MediaFileEntity.fromDomain(file))
    }

    override suspend fun deleteFileById(id: Int) {
        mediaFileDao.deleteFileById(id)
    }
}
