package com.example.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaFileDao {
    @Query("SELECT * FROM media_files ORDER BY modifiedAt DESC")
    fun getAllFiles(): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE isEncrypted = 1 ORDER BY modifiedAt DESC")
    fun getVaultFiles(): Flow<List<MediaFileEntity>>

    @Query("""
        SELECT media_files.* FROM media_files 
        JOIN media_files_fts ON media_files.id = media_files_fts.docid 
        WHERE media_files.isEncrypted = 0 AND media_files_fts MATCH :query 
        ORDER BY media_files.modifiedAt DESC
    """)
    fun searchFilesFts(query: String): Flow<List<MediaFileEntity>>

    @Query("""
        SELECT * FROM media_files 
        WHERE isEncrypted = 0 AND (name LIKE :queryPattern OR tags LIKE :queryPattern OR ocrText LIKE :queryPattern)
        ORDER BY modifiedAt DESC
    """)
    fun searchFilesFallback(queryPattern: String): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE id = :id")
    suspend fun getFileById(id: Int): MediaFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: MediaFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<MediaFileEntity>)

    @Update
    suspend fun updateFile(file: MediaFileEntity)

    @Query("DELETE FROM media_files WHERE id = :id")
    suspend fun deleteFileById(id: Int)
}
