#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/feature/vault/VaultSessionManager.kt
package com.example.feature.vault

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

class VaultSessionManager(context: Context) {
    private val sharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "vault_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        context.getSharedPreferences("vault_secure_prefs", Context.MODE_PRIVATE)
    }

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val PREF_PIN = "vault_pin"
    private val PREF_SALT = "vault_salt"
    private val PREF_FAILED_ATTEMPTS = "vault_failed_attempts"
    private val PREF_LOCKOUT_UNTIL = "vault_lockout_until"

    private val _lockoutRemainingSeconds = MutableStateFlow(0L)
    val lockoutRemainingSeconds: StateFlow<Long> = _lockoutRemainingSeconds.asStateFlow()

    private fun hashPin(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun setPin(pin: String) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hashedPin = hashPin(pin, salt)
        sharedPreferences.edit()
            .putString(PREF_PIN, hashedPin)
            .putString(PREF_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putInt(PREF_FAILED_ATTEMPTS, 0)
            .putLong(PREF_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun hasPin(): Boolean {
        return sharedPreferences.contains(PREF_PIN)
    }

    fun updateLockoutState() {
        val lockoutUntil = sharedPreferences.getLong(PREF_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        if (lockoutUntil > now) {
            _lockoutRemainingSeconds.value = (lockoutUntil - now) / 1000
        } else {
            _lockoutRemainingSeconds.value = 0L
        }
    }

    fun verifyPin(pin: String): Boolean {
        updateLockoutState()
        if (_lockoutRemainingSeconds.value > 0) return false

        val storedHash = sharedPreferences.getString(PREF_PIN, null)
        val storedSaltStr = sharedPreferences.getString(PREF_SALT, null)
        
        if (storedHash == null || storedSaltStr == null) return false
        
        val salt = Base64.decode(storedSaltStr, Base64.NO_WRAP)
        val inputHash = hashPin(pin, salt)
        
        // Constant time comparison
        var isValid = true
        if (storedHash.length != inputHash.length) {
            isValid = false
        }
        var result = 0
        val len = minOf(storedHash.length, inputHash.length)
        for (i in 0 until len) {
            result = result or (storedHash[i].code xor inputHash[i].code)
        }
        if (result != 0) isValid = false

        if (isValid) {
            sharedPreferences.edit()
                .putInt(PREF_FAILED_ATTEMPTS, 0)
                .putLong(PREF_LOCKOUT_UNTIL, 0L)
                .apply()
            _lockoutRemainingSeconds.value = 0L
            unlockVault()
        } else {
            var attempts = sharedPreferences.getInt(PREF_FAILED_ATTEMPTS, 0) + 1
            var lockoutUntil = 0L
            if (attempts >= 5) {
                val penaltyFactor = attempts - 4
                val lockoutDuration = when (penaltyFactor) {
                    1 -> 30_000L // 30s
                    2 -> 60_000L // 60s
                    else -> 300_000L // 5 mins
                }
                lockoutUntil = System.currentTimeMillis() + lockoutDuration
            }
            sharedPreferences.edit()
                .putInt(PREF_FAILED_ATTEMPTS, attempts)
                .putLong(PREF_LOCKOUT_UNTIL, lockoutUntil)
                .apply()
            updateLockoutState()
        }
        return isValid
    }

    fun resetFailedAttempts() {
        sharedPreferences.edit()
            .putInt(PREF_FAILED_ATTEMPTS, 0)
            .putLong(PREF_LOCKOUT_UNTIL, 0L)
            .apply()
        _lockoutRemainingSeconds.value = 0L
    }

    fun unlockVault() {
        _isVaultUnlocked.value = true
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }
}
INNER_EOF
