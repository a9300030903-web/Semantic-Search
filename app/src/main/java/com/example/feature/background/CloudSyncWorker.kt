package com.example.feature.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.core.data.repository.MediaFileRepository
import com.example.plugin.cloud.GoogleDriveProvider

/**
 * Phase 12: Background System - Cloud Sync
 * Automatically processes offline queued uploads and syncs state via Google Drive.
 */
class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val mediaFileRepository: MediaFileRepository,
    private val driveProvider: GoogleDriveProvider
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (isStopped) return Result.failure()

        try {
            // Step 1: Set initial progress
            setProgress(workDataOf("progress" to 10, "status" to "Preparing Drive Sync..."))
            
            if (!driveProvider.isAuthenticated()) {
                 setProgress(workDataOf("progress" to 0, "status" to "Not authenticated to Google Drive"))
                 return Result.failure(workDataOf("error" to "Not authenticated"))
            }

            setProgress(workDataOf(
                "progress" to 40,
                "status" to "Processing offline queue..."
            ))

            // Step 2: Push queued updates
            val syncResult = driveProvider.syncChanges()

            if (isStopped) return Result.failure()

            return if (syncResult.isSuccess) {
                setProgress(workDataOf("progress" to 100, "status" to "Sync Complete"))
                Result.success(workDataOf("syncResult" to "Sync Complete"))
            } else {
                val error = syncResult.exceptionOrNull()?.message ?: "Unknown sync error"
                setProgress(workDataOf("progress" to 0, "status" to "Sync failed: $error"))
                Result.retry()
            }

        } catch (e: Exception) {
            return Result.failure(workDataOf("error" to (e.localizedMessage ?: e.message ?: "Sync error")))
        }
    }
}
