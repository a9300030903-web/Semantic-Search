package com.example.core.util

import java.security.MessageDigest

object HashUtil {
    /**
     * Generates a SHA-256 hash for a given file.
     * Essential for Level 1 Duplicate Cleaner (Exact Match).
     */
    fun generateFileHash(file: java.io.File): String? {
        if (!file.exists() || !file.isFile) return null

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
