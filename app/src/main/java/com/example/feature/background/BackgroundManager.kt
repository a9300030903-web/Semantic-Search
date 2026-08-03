package com.example.feature.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.core.model.MediaScanState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * Phase 12: Background System Manager
 * Handles scheduling of WorkManager tasks and abstraction of background execution state.
 */
class BackgroundManager(private val context: Context) {
    
    companion object {
        const val MEDIA_SCAN_WORK_NAME = "vvf_media_scan_work"
    }

    fun getScanWorkStateFlow(): Flow<MediaScanState> {
        return try {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkFlow(MEDIA_SCAN_WORK_NAME)
                .map { list ->
                    val info = list.firstOrNull()
                    if (info == null) {
                        MediaScanState(isScanning = false, progress = 0, status = "Idle")
                    } else {
                        val isScanning = info.state == WorkInfo.State.RUNNING
                        val progress = info.progress.getInt("progress", 0)
                        val status = info.progress.getString("status") ?: if (isScanning) "Scanning..." else "Idle"
                        MediaScanState(isScanning = isScanning, progress = progress, status = status)
                    }
                }
        } catch (e: Throwable) {
            flowOf(MediaScanState())
        }
    }
    
    fun scheduleCloudSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<CloudSyncWorker>(
                15, TimeUnit.MINUTES // Minimum interval allowed by WorkManager
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncWorkRequest)
        } catch (t: Throwable) {
            // Fallback for tests/non-WorkManager environments
        }
    }

    fun triggerManualMediaScan() {
        try {
            val mediaScanRequest = OneTimeWorkRequestBuilder<MediaScanWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                MEDIA_SCAN_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                mediaScanRequest
            )
        } catch (t: Throwable) {
            // Fallback for tests/non-WorkManager environments
        }
    }
}
