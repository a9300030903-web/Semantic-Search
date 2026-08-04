#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/feature/background/MediaScanWorker.kt
package com.example.feature.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.core.data.repository.MediaFileRepository
import com.example.core.model.MediaFile
import com.example.feature.filemanager.CoreFileManager
import java.io.File

class MediaScanWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val mediaFileRepository: MediaFileRepository,
    private val coreFileManager: CoreFileManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Set initial progress
        setProgress(workDataOf("progress" to 0))
        if (isStopped) return Result.failure()

        // Step 1: Initialize Scan
        setProgress(workDataOf("progress" to 10, "status" to "Scanning /sdcard/VVFManager..."))
        if (isStopped) return Result.failure()

        // Scan actual storage directory safely if available
        val targetDir = File(applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir, "VVFManager")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val discoveredFiles = coreFileManager.scanDirectoryRecursively(targetDir)
        if (isStopped) return Result.failure()

        if (discoveredFiles.isEmpty()) {
             setProgress(workDataOf("progress" to 100, "status" to "No files found. Scan completed!"))
             return Result.success()
        }

        // Step 2: Add scanned items & Compute progress dynamically
        val totalFiles = discoveredFiles.size
        var processed = 0

        // We'll process them in batches or one-by-one to report progress
        val scanItems = mutableListOf<MediaFile>()
        
        for (f in discoveredFiles) {
            if (isStopped) return Result.failure()
            
            scanItems.add(
                MediaFile(
                    name = f.name,
                    path = f.absolutePath,
                    type = if (f.name.endsWith(".jpg") || f.name.endsWith(".png") || f.name.endsWith(".jpeg")) "Image" else "Document",
                    mimeType = if (f.name.endsWith(".jpg") || f.name.endsWith(".jpeg")) "image/jpeg" else if (f.name.endsWith(".png")) "image/png" else "application/pdf",
                    size = f.length(),
                    modifiedAt = f.lastModified()
                )
            )
            
            processed++
            // Calculate progress between 10% and 50%
            val percent = 10 + ((processed.toFloat() / totalFiles) * 40).toInt()
            if (processed % 10 == 0 || processed == totalFiles) {
                 setProgress(workDataOf("progress" to percent, "status" to "Analyzing metadata ($processed/$totalFiles)..."))
            }
        }

        if (scanItems.isNotEmpty()) {
            if (isStopped) return Result.failure()
            
            // Step 3: Insert in batches for smooth progress reporting during DB save
            val batchSize = 50
            val chunks = scanItems.chunked(batchSize)
            var inserted = 0
            
            for (chunk in chunks) {
                if (isStopped) return Result.failure()
                
                mediaFileRepository.insertFiles(chunk)
                inserted += chunk.size
                
                // Calculate progress between 50% and 100%
                val percent = 50 + ((inserted.toFloat() / totalFiles) * 50).toInt()
                setProgress(workDataOf("progress" to percent, "status" to "Saving to local secure storage ($inserted/$totalFiles)..."))
            }
        }

        // Done
        setProgress(workDataOf("progress" to 100, "status" to "Media Scan completed!"))
        return Result.success()
    }
}
INNER_EOF
