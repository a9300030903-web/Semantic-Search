package com.example.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MediaFileEntity::class, MediaFileFtsEntity::class, CloudSyncTaskEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaFileDao(): MediaFileDao
    abstract fun cloudSyncTaskDao(): CloudSyncTaskDao
}
