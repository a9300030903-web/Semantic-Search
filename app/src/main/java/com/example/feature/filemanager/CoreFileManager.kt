package com.example.feature.filemanager

import com.example.core.model.MediaFile
import com.example.core.util.HashUtil
import java.io.File

/**
 * Core File Manager Domain Model
 * Handles primary file system interactions: Browse, Copy, Move, Rename, Delete, Duplicate check
 */
class CoreFileManager {

    fun browseDirectory(path: String): List<File> {
        val dir = File(path)
        return if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun copyFile(source: File, destination: File): Boolean {
        return try {
            source.copyTo(destination, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun moveFile(source: File, destination: File): Boolean {
        return try {
            source.renameTo(destination)
        } catch (e: Exception) {
            false
        }
    }

    fun renameFile(target: File, newName: String): Boolean {
        val parent = target.parentFile ?: return false
        val newFile = File(parent, newName)
        return target.renameTo(newFile)
    }

    fun deleteFile(target: File): Boolean {
        // In a complete implementation, this might move to a "Recycle Bin" folder first
        return target.delete()
    }
    
    fun moveToRecycleBin(target: File, recycleBinDir: File): Boolean {
        if (!recycleBinDir.exists()) recycleBinDir.mkdirs()
        val destination = File(recycleBinDir, target.name)
        return moveFile(target, destination)
    }

    /**
     * Level 1 & 2 Duplicate Cleaner
     * Level 1: Exact Hash Match (SHA-256)
     * Level 2: Metadata Match (Name + Size + Extension)
     */
    fun findDuplicates(files: List<File>): Map<String, List<File>> {
        val duplicates = mutableMapOf<String, MutableList<File>>()

        for (file in files) {
            if (!file.isFile) continue

            // Level 1: Hash Match
            val hash = HashUtil.generateFileHash(file) ?: continue
            
            // Note: Level 2 metadata fallback can be appended or handled in a secondary pass.
            // Here we group by hash for Level 1 exact matches.
            duplicates.getOrPut(hash) { mutableListOf() }.add(file)
        }

        // Return only groups that have more than 1 file (i.e., duplicates exist)
        return duplicates.filterValues { it.size > 1 }
    }
}
