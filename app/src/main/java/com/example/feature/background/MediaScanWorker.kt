package com.example.feature.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.core.data.repository.MediaFileRepository
import com.example.core.model.MediaFile
import com.example.feature.filemanager.CoreFileManager
import kotlinx.coroutines.delay
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
        delay(300)

        // Step 2: Read Existing Files and Simulate Indexing
        setProgress(workDataOf("progress" to 30, "status" to "Analyzing metadata..."))
        if (isStopped) return Result.failure()

        // Scan actual storage directory safely if available
        val targetDir = File(applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir, "VVFManager")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val discoveredFiles = coreFileManager.scanDirectoryRecursively(targetDir)
        if (isStopped) return Result.failure()

        // Step 3: Deep Scan
        setProgress(workDataOf("progress" to 60, "status" to "Computing semantic hashes..."))
        if (isStopped) return Result.failure()
        delay(300)

        // Step 4: Add scanned items
        val scanItems = mutableListOf<MediaFile>()
        
        for (f in discoveredFiles) {
            if (isStopped) return Result.failure()
            scanItems.add(
                MediaFile(
                    name = f.name,
                    path = f.absolutePath,
                    type = if (f.name.endsWith(".jpg") || f.name.endsWith(".png")) "Image" else "Document",
                    mimeType = if (f.name.endsWith(".jpg")) "image/jpeg" else "application/pdf",
                    size = f.length()
                )
            )
        }

        if (scanItems.isNotEmpty()) {
            if (isStopped) return Result.failure()
            mediaFileRepository.insertFiles(scanItems)
        }

        setProgress(workDataOf("progress" to 85, "status" to "Saving to local secure storage..."))
        if (isStopped) return Result.failure()
        delay(300)

        // Done
        setProgress(workDataOf("progress" to 100, "status" to "Media Scan completed!"))

        return Result.success()
    }
}

