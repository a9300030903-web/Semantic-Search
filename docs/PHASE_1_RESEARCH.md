# VVF Smart Manager - Phase 1: Technical Research

## 1. Core Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Dependency Injection**: Dagger Hilt
- **Asynchronous Programming**: Kotlin Coroutines & Flow
- **Navigation**: Jetpack Navigation Component

## 2. File Management & Storage
- **Local Database**: Room
- **Secure Database**: SQLCipher (for encrypted database fields/metadata)
- **File Access**: `java.io.File` / `Storage Access Framework (SAF)` as required by scoped storage.
- **Vault Encryption**: AES-256 GCM using Android Keystore for key management.
- **Background Processing**: WorkManager (for indexing, sync, cloud upload)

## 3. Search Pipeline (Core feature)
- **Keyword Search**: Room FTS4 (Full Text Search) for blazing-fast metadata and filename lookups.
- **Semantic Search**: TensorFlow Lite (TFLite) lightweight embedding model running locally on-device for generating and comparing text embeddings.
- **OCR Search**: ML Kit Text Recognition (Plugin module, downloaded on-demand).

## 4. Security & Authentication
- **Local Auth**: Android BiometricPrompt API (PIN / Fingerprint / Face).
- **Secure Preferences**: EncryptedSharedPreferences.
- **Security Flags**: `WindowManager.LayoutParams.FLAG_SECURE` for Vault screens.

## 5. Cloud Sync (Plugin Architecture)
- **Google Drive Integration**: Google Drive REST API + Credential Manager.
- **Other Providers**: Will follow a similar REST / SDK integration path wrapped behind a standard `CloudProvider` interface in the future.

## Conclusion
The stack is fully verified against Android's modern development standards. The use of SQLCipher, Room, and WorkManager ensures a robust, secure, and offline-first foundation. The semantic search using TFLite guarantees privacy by processing everything locally without requiring cloud access.
