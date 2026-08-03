package com.example.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MediaFileEntity::class, MediaFileFtsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaFileDao(): MediaFileDao
}
