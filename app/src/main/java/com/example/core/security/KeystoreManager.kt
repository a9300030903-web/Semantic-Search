package com.example.core.security

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreManager {
    private const val PREFS_NAME = "vvf_security_prefs"
    private const val ENCRYPTED_DB_KEY = "encrypted_db_key"
    private const val DB_KEY_IV = "db_key_iv"

    fun getDatabasePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedKeyBase64 = prefs.getString(ENCRYPTED_DB_KEY, null)
        val ivBase64 = prefs.getString(DB_KEY_IV, null)

        val masterKey = getOrGenerateMasterKey()

        if (encryptedKeyBase64 != null && ivBase64 != null) {
            try {
                val encryptedKey = Base64.decode(encryptedKeyBase64, Base64.DEFAULT)
                val iv = Base64.decode(ivBase64, Base64.DEFAULT)
                
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
                
                return cipher.doFinal(encryptedKey)
            } catch (e: Exception) {
                // If decryption fails (e.g. key invalidated), we must regenerate
            }
        }

        // Generate a new random key for the database
        val dbKey = ByteArray(32)
        SecureRandom().nextBytes(dbKey)

        // Encrypt the new key with the master key
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val encryptedKey = cipher.doFinal(dbKey)

        // Store encrypted key and IV
        prefs.edit()
            .putString(ENCRYPTED_DB_KEY, Base64.encodeToString(encryptedKey, Base64.DEFAULT))
            .putString(DB_KEY_IV, Base64.encodeToString(iv, Base64.DEFAULT))
            .apply()

        return dbKey
    }

    fun getOrGenerateMasterKey(): SecretKey {
        // Safe check for Robolectric / local JVM environment to prevent loading Android Keystore classes
        val isRobolectric = System.getProperty("robolectric.class") != null || 
                System.getProperty("java.runtime.name")?.contains("Android") == false
        
        if (isRobolectric) {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            return keyGenerator.generateKey()
        }

        return try {
            // AndroidKeystoreGenerator is only loaded dynamically at runtime on Android
            AndroidKeystoreGenerator.generateAndroidKey()
        } catch (e: Throwable) {
            // Fallback for JVM/Robolectric testing environment where AndroidKeyStore is not available
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            keyGenerator.generateKey()
        }
    }
}
