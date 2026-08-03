package com.example.feature.vault

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    fun setPin(pin: String) {
        sharedPreferences.edit().putString(PREF_PIN, pin).apply()
    }

    fun hasPin(): Boolean {
        return sharedPreferences.contains(PREF_PIN)
    }

    fun verifyPin(pin: String): Boolean {
        val storedPin = sharedPreferences.getString(PREF_PIN, null)
        val isValid = storedPin == pin
        if (isValid) {
            unlockVault()
        }
        return isValid
    }

    fun unlockVault() {
        _isVaultUnlocked.value = true
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }
}
