package com.example.core.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class VaultEncryptionManager {
    private val masterKey: SecretKey = KeystoreManager.getOrGenerateMasterKey()

    suspend fun encryptFile(inputFile: File, outputFile: File, fileId: String) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        cipher.updateAAD(fileId.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv

        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                fos.write(iv) // Store IV at the beginning of the file
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val output = cipher.update(buffer, 0, bytesRead)
                    if (output != null) fos.write(output)
                }
                val outputBytes = cipher.doFinal()
                if (outputBytes != null) fos.write(outputBytes)
            }
        }
    }

    suspend fun decryptFile(inputFile: File, outputFile: File, fileId: String) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        FileInputStream(inputFile).use { fis ->
            val iv = ByteArray(12) // GCM standard IV length
            var totalRead = 0
            while (totalRead < 12) {
                val read = fis.read(iv, totalRead, 12 - totalRead)
                if (read == -1) break
                totalRead += read
            }
            if (totalRead < 12) {
                throw java.io.IOException("Invalid encrypted file header: truncated IV")
            }

            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
            cipher.updateAAD(fileId.toByteArray(Charsets.UTF_8))

            FileOutputStream(outputFile).use { fos ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    val output = cipher.update(buffer, 0, bytesRead)
                    if (output != null) fos.write(output)
                }
                val outputBytes = cipher.doFinal()
                if (outputBytes != null) fos.write(outputBytes)
            }
        }
    }
}

