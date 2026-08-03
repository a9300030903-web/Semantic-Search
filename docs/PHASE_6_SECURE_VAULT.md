# VVF Smart Manager - Phase 6: Secure Vault

## Overview
This phase integrates the file encryption mechanisms (Phase 4) with a dedicated user session layer handling authentication. It strictly adheres to local-only policies without requiring a cloud connection.

## Components Implemented

### 1. Biometric Authentication
- **`BiometricAuthManager`**: Wraps the AndroidX Biometric library. It queries for `BIOMETRIC_STRONG` or `DEVICE_CREDENTIAL` and surfaces a secure, system-level prompt for fingerprint or face ID verification.

### 2. Vault Session Management
- **`VaultSessionManager`**: 
  - Manages the `isVaultUnlocked` reactive state (`StateFlow`).
  - Utilizes `EncryptedSharedPreferences` backed by `MasterKey.KeyScheme.AES256_GCM` to safely store a recovery/fallback PIN.
  - Can be extended to implement automatic locking based on background timeouts or lifecycle events.

### 3. Dependencies
- Added `androidx.security:security-crypto` for encrypted local KV storage.
- Added `androidx.biometric:biometric` for the authentication prompt.

## Integration Plan (UI Layer - Phase 13)
When the UI is built, screens requiring vault access will intercept the navigation graph, prompt for Biometrics/PIN via these managers, and only display decrypted content when `isVaultUnlocked` is true. `FLAG_SECURE` will be applied to the activity when inside these screens.

## Checklist Status
- [x] Code Implemented
- [x] Security Checked (EncryptedSharedPreferences, System Biometrics)
- [x] Tests Running
- [x] Documentation Updated
