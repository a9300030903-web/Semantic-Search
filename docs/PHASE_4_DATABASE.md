# VVF Smart Manager - Phase 4: Database & Security

## Overview
This phase establishes the foundational secure data storage mechanisms for the application, strictly adhering to the offline-first and privacy-first mandates.

## Components Implemented

### 1. Database (Room + SQLCipher)
- **Entity**: `MediaFile` represents a unified file metadata structure including flags for encryption, OCR text, tags, and TFLite embeddings.
- **DAO**: `MediaFileDao` provides reactive `Flow`-based queries and standard CRUD operations.
- **Security**: The database is fully encrypted at rest using `SQLCipher` via the `SupportFactory`.

### 2. Security (Android Keystore + AES-256 GCM)
- **KeystoreManager**: Generates and securely stores the application's Master Key within the hardware-backed Android Keystore (`AndroidKeyStore`). Key material is non-exportable.
- **VaultEncryptionManager**: Handles file-level encryption for the Secure Vault. Uses `AES/GCM/NoPadding` with securely generated IVs stored alongside the encrypted payload.

## Dependency Injection (Koin)
- `DatabaseModule` provisions the `AppDatabase` and `MediaFileDao` as singletons, injecting the SQLCipher passphrase seamlessly.

## Checklist Status
- [x] Code Implemented
- [x] Security Checked (AES-256 GCM, Android Keystore, SQLCipher)
- [x] Tests Running
- [x] Documentation Updated
