package com.example.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CloudSyncTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: CloudSyncTaskEntity)

    @Update
    suspend fun updateTask(task: CloudSyncTaskEntity)

    @Query("SELECT * FROM cloud_sync_tasks WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY lastAttempt ASC")
    suspend fun getPendingTasks(): List<CloudSyncTaskEntity>

    @Query("SELECT * FROM cloud_sync_tasks WHERE fileId = :fileId AND action = :action LIMIT 1")
    suspend fun getTaskForFile(fileId: Int, action: String): CloudSyncTaskEntity?

    @Query("DELETE FROM cloud_sync_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Int)
}
