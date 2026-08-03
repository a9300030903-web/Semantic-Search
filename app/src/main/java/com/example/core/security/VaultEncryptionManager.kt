package com.example.core.security

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class VaultEncryptionManager {
    private val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    private val masterKey: SecretKey = KeystoreManager.getOrGenerateMasterKey()

    fun encryptFile(inputFile: File, outputFile: File) {
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv

        FileInputStream(inputFile).use { fis ->
            FileOutputStream(outputFile).use { fos ->
                fos.write(iv) // Store IV at the beginning of the file
                val buffer = ByteArray(1024)
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

    fun decryptFile(inputFile: File, outputFile: File) {
        FileInputStream(inputFile).use { fis ->
            val iv = ByteArray(12) // GCM standard IV length
            fis.read(iv)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)

            FileOutputStream(outputFile).use { fos ->
                val buffer = ByteArray(1024)
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
