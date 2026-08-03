package com.example.core.model

/**
 * Pure Domain Model representing the state of background media scanning tasks.
 * Independent of WorkManager or any framework APIs.
 */
data class MediaScanState(
    val isScanning: Boolean = false,
    val progress: Int = 0,
    val status: String = "Idle"
)
