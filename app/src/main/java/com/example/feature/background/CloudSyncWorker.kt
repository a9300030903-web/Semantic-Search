package com.example.feature.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.core.data.repository.MediaFileRepository
import com.example.plugin.cloud.GitHubSyncProvider
import kotlinx.coroutines.flow.first

/**
 * Phase 12: Background System - GitHub Repository Metadata Cloud Sync
 * Automatically monitors local media changes and pushes metadata updates to a linked GitHub repository.
 */
class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val mediaFileRepository: MediaFileRepository,
    private val gitHubSyncProvider: GitHubSyncProvider
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (isStopped) return Result.failure()

        try {
            // Step 1: Set initial progress
            setProgress(workDataOf("progress" to 10, "status" to "Reading local media database..."))

            // Fetch current media files from database
            val mediaFiles = mediaFileRepository.getAllFiles().first()
            if (isStopped) return Result.failure()

            setProgress(workDataOf(
                "progress" to 40,
                "status" to "Preparing metadata manifest (${mediaFiles.size} files)..."
            ))

            // Step 2: Push metadata update payload to linked GitHub Repository
            val syncResult = gitHubSyncProvider.syncMetadataManifest(mediaFiles)

            if (isStopped) return Result.failure()

            return if (syncResult.isSuccess) {
                val message = syncResult.getOrNull() ?: "Metadata successfully synced to GitHub!"
                setProgress(workDataOf("progress" to 100, "status" to message))
                Result.success(workDataOf("syncResult" to message))
            } else {
                val error = syncResult.exceptionOrNull()?.message ?: "Unknown sync error"
                setProgress(workDataOf("progress" to 0, "status" to "Sync failed: $error"))
                
                // Permanent errors should not be retried
                if (error.contains("HTTP 401") || error.contains("HTTP 403") || error.contains("HTTP 404") || error.contains("Token required")) {
                    Result.failure(workDataOf("error" to error))
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            return Result.failure(workDataOf("error" to (e.localizedMessage ?: e.message ?: "Sync error")))
        }
    }
}

