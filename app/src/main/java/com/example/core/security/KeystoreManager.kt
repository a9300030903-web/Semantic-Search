package com.example.core.security

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object KeystoreManager {

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
