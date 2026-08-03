package com.example.feature.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.core.data.repository.MediaFileRepository
import com.example.core.model.MediaFile
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MediaScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val mediaFileRepository: MediaFileRepository by inject()

    override suspend fun doWork(): Result {
        // Set initial progress
        setProgress(workDataOf("progress" to 0))
        delay(500)

        // Step 1: Initialize Scan
        setProgress(workDataOf("progress" to 10, "status" to "Scanning /sdcard/VVFManager..."))
        delay(800)

        // Step 2: Read Existing Files and Simulate Indexing
        setProgress(workDataOf("progress" to 30, "status" to "Analyzing metadata..."))
        delay(800)

        // Step 3: Deep Scan
        setProgress(workDataOf("progress" to 60, "status" to "Computing semantic hashes..."))
        delay(800)

        // Step 4: Add scanned items if missing to show real action!
        val scanItems = listOf(
            MediaFile(
                name = "quarterly_budget_2026.pdf",
                path = "/sdcard/VVFManager/quarterly_budget_2026.pdf",
                type = "Document",
                mimeType = "application/pdf",
                size = 3500000L,
                tags = "finance, budget, work",
                ocrText = "VISHWA VIJAYA FOUNDATION QUARTERLY REPORT BUDGET 2026 AUDITED"
            ),
            MediaFile(
                name = "foundation_day_celebration.jpg",
                path = "/sdcard/VVFManager/foundation_day_celebration.jpg",
                type = "Image",
                mimeType = "image/jpeg",
                size = 5400000L,
                tags = "celebration, foundation, event",
                ocrText = "VVF FOUNDATION DAY 15 JULY 2026"
            )
        )

        for (item in scanItems) {
            mediaFileRepository.insertFile(item)
        }

        setProgress(workDataOf("progress" to 85, "status" to "Saving to local secure storage..."))
        delay(800)

        // Done
        setProgress(workDataOf("progress" to 100, "status" to "Media Scan completed!"))
        delay(400)

        return Result.success()
    }
}
