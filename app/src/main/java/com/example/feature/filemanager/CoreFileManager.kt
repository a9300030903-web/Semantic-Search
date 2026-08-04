package com.example.feature.filemanager

import com.example.core.model.MediaFile
import com.example.core.util.HashUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

/**
 * Core File Manager Domain Model
 * Handles primary file system interactions: Browse, Copy, Move, Rename, Delete, Duplicate check
 * 
 * Audited for ANRs, Infinite Recursion, Memory Leaks, Symbolic Link Loops, Excessive I/O, and Thread Starvation.
 */
class CoreFileManager {

    /**
     * Safely browses a directory without blocking the main thread or crashing on unreadable paths.
     */
    suspend fun browseDirectory(path: String): List<File> = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) {
            return@withContext emptyList()
        }

        try {
            if (Files.isSymbolicLink(dir.toPath())) {
                return@withContext emptyList()
            }
            dir.listFiles()?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Safely scans directory tree recursively with max depth limit and symlink cycle detection.
     */
    suspend fun scanDirectoryRecursively(
        rootDir: File,
        maxDepth: Int = 10,
        visitedPaths: MutableSet<String> = mutableSetOf()
    ): List<File> = withContext(Dispatchers.IO) {
        val results = mutableListOf<File>()
        if (maxDepth <= 0 || !rootDir.exists() || !rootDir.canRead()) return@withContext results

        try {
            coroutineContext.ensureActive()

            val canonicalPath = try { rootDir.canonicalPath } catch (e: Exception) { rootDir.absolutePath }
            if (visitedPaths.contains(canonicalPath)) {
                return@withContext results // Symlink cycle detected
            }
            visitedPaths.add(canonicalPath)

            if (Files.isSymbolicLink(rootDir.toPath())) {
                return@withContext results
            }

            val files = rootDir.listFiles() ?: return@withContext results

            for (file in files) {
                coroutineContext.ensureActive()
                try {
                    val fileCanonical = try { file.canonicalPath } catch (e: Exception) { file.absolutePath }
                    if (visitedPaths.contains(fileCanonical)) continue

                    if (file.isDirectory) {
                        if (!Files.isSymbolicLink(file.toPath())) {
                            val subFiles = scanDirectoryRecursively(file, maxDepth - 1, visitedPaths)
                            results.addAll(subFiles)
                        }
                    } else if (file.isFile && file.canRead()) {
                        results.add(file)
                    }
                } catch (e: SecurityException) {
                    // Ignore inaccessible subpaths
                }
            }
        } catch (e: Exception) {
            // Log/ignore unhandled I/O failures
        }

        results
    }

    suspend fun copyFile(source: File, destination: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!source.exists() || !source.canRead()) return@withContext false
            source.copyTo(destination, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun moveFile(source: File, destination: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!source.exists()) return@withContext false
            source.renameTo(destination)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameFile(target: File, newName: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!target.exists()) return@withContext false
            val parent = target.parentFile ?: return@withContext false
            val newFile = File(parent, newName)
            target.renameTo(newFile)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteFile(target: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!target.exists()) return@withContext false
            target.delete()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun moveToRecycleBin(target: File, recycleBinDir: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!recycleBinDir.exists()) recycleBinDir.mkdirs()
            val destination = File(recycleBinDir, target.name)
            moveFile(target, destination)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Level 1 & 2 Duplicate Cleaner (Optimized)
     * Level 2 (Pre-filter): Group by file size to eliminate >90% unnecessary disk reads.
     * Level 1 (Exact Hash): Only compute SHA-256 for size-matched candidates.
     */
    suspend fun findDuplicates(files: List<File>): Map<String, List<File>> = withContext(Dispatchers.IO) {
        val duplicates = mutableMapOf<String, MutableList<File>>()

        // Pass 1: Group by file size (metadata check - fast)
        val sizeGrouped = mutableMapOf<Long, MutableList<File>>()
        for (file in files) {
            coroutineContext.ensureActive()
            if (!file.exists() || !file.isFile || !file.canRead() || file.length() == 0L) continue
            sizeGrouped.getOrPut(file.length()) { mutableListOf() }.add(file)
        }

        // Filter out unique file sizes
        val candidateGroups = sizeGrouped.filterValues { it.size > 1 }

        // Pass 2: SHA-256 Hash on candidate groups only
        for ((_, candidateFiles) in candidateGroups) {
            coroutineContext.ensureActive()
            for (file in candidateFiles) {
                coroutineContext.ensureActive()
                val hash = HashUtil.generateFileHash(file) ?: continue
                duplicates.getOrPut(hash) { mutableListOf() }.add(file)
            }
        }

        return@withContext duplicates.filterValues { it.size > 1 }
    }

    /**
     * Duplicate Detection Level 1 (exact file size match) and Level 2 (SHA-256 hash match)
     * Grouping by size first, then hashing only same-size groups.
     */
    suspend fun findExactDuplicates(files: List<MediaFile>): Map<String, List<MediaFile>> = withContext(Dispatchers.IO) {
        val duplicates = mutableMapOf<String, MutableList<MediaFile>>()

        // Pass 1: Group by file size (Level 1)
        val sizeGrouped = mutableMapOf<Long, MutableList<MediaFile>>()
        for (file in files) {
            coroutineContext.ensureActive()
            if (file.size == 0L) continue
            sizeGrouped.getOrPut(file.size) { mutableListOf() }.add(file)
        }

        // Filter out unique file sizes
        val candidateGroups = sizeGrouped.filterValues { it.size > 1 }

        // Pass 2: SHA-256 Hash on candidate groups only (Level 2)
        for ((_, candidateFiles) in candidateGroups) {
            coroutineContext.ensureActive()
            for (mediaFile in candidateFiles) {
                coroutineContext.ensureActive()
                val ioFile = File(mediaFile.path)
                if (!ioFile.exists() || !ioFile.isFile || !ioFile.canRead()) continue
                
                val hash = HashUtil.generateFileHash(ioFile) ?: continue
                duplicates.getOrPut(hash) { mutableListOf() }.add(mediaFile)
            }
        }

        return@withContext duplicates.filterValues { it.size > 1 }
    }
}

