# Phase 15: Optimization - Proguard Rules

# Keep Room entities and DAOs intact
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# WorkManager
-keep class androidx.work.** { *; }

# ML Kit OCR (Keep essential models)
-keep class com.google.mlkit.** { *; }

# Prevent Compose classes from being overly obfuscated causing runtime crashes
-keep class androidx.compose.** { *; }

# Data model classes
-keep class com.example.core.model.** { *; }
