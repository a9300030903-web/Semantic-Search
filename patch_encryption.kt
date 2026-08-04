    fun toggleFileEncryption(file: MediaFile) {
        viewModelScope.launch {
            try {
                val originalFile = java.io.File(file.path)
                
                if (!file.isEncrypted) {
                    val vaultDir = java.io.File(applicationContext.filesDir, "vault")
                    if (!vaultDir.exists()) vaultDir.mkdirs()
                    
                    val encryptedFile = java.io.File(vaultDir, "enc_${file.id}_${originalFile.name}")
                    
                    vaultEncryptionManager.encryptFile(originalFile, encryptedFile, file.id.toString())
                    
                    if (originalFile.exists()) {
                        originalFile.delete() // Simple delete, shredding could be implemented later
                    }
                    
                    val updated = file.copy(
                        isEncrypted = true,
                        path = encryptedFile.absolutePath,
                        modifiedAt = System.currentTimeMillis()
                    )
                    mediaFileRepository.updateFile(updated)
                    addCloudLog("File ${file.name} encrypted & moved to Vault.")
                } else {
                    val decryptedDir = java.io.File(applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir, "VVFManager")
                    if (!decryptedDir.exists()) decryptedDir.mkdirs()
                    
                    val decryptedFile = java.io.File(decryptedDir, "dec_${file.id}_${originalFile.name.removePrefix("enc_${file.id}_")}")
                    
                    vaultEncryptionManager.decryptFile(originalFile, decryptedFile, file.id.toString())
                    
                    if (originalFile.exists()) {
                        originalFile.delete()
                    }
                    
                    val updated = file.copy(
                        isEncrypted = false,
                        path = decryptedFile.absolutePath,
                        modifiedAt = System.currentTimeMillis()
                    )
                    mediaFileRepository.updateFile(updated)
                    addCloudLog("File ${file.name} decrypted & moved to Storage.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                addCloudLog("Encryption/Decryption failed for ${file.name}: ${e.message}")
            }
        }
    }
