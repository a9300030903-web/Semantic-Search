# VVF Smart Manager - Phase 2: Architecture Freeze

## 1. Modular Package Structure
```
com.aistudio.mediamanager.a1b2c3 (com.example)
├── core/
│   ├── di/             # Hilt modules
│   ├── data/           # Repositories, DAO, Network/Local Data Sources
│   ├── model/          # Domain models
│   ├── security/       # Encryption, Keystore, Vault logic
│   └── util/           # Extensions, Constants
├── feature/
│   ├── filemanager/    # Browse, Copy, Move, Duplicate Cleaner Level 1-2
│   ├── vault/          # Secure Vault UI and logic
│   ├── search/         # FTS, OCR, Semantic Search logic
│   ├── ai/             # AI auto tags, categories, similarity slider
│   └── settings/       # App configuration
├── plugin/
│   ├── cloud/          # Google Drive, OneDrive, etc.
│   └── ocr/            # ML Kit dynamic module integration
└── MainActivity.kt
```

## 2. Data Flow (Clean Architecture + MVVM)
1. **UI Layer**: Jetpack Compose screens observe `StateFlow` from `ViewModel`.
2. **Presentation Layer**: `ViewModel` handles user intents, interacts with UseCases.
3. **Domain Layer**: `UseCases` encapsulate single business rules (e.g., `EncryptFileUseCase`).
4. **Data Layer**: `Repository` interfaces implemented by `RepositoryImpl` connecting to `Room/SQLCipher` or `File System`.

## 3. Security Flow
- **Key Generation**: On first launch, a master key is generated in the Android Keystore.
- **Database Security**: SQLCipher encrypts the Room database using the master key.
- **File Encryption**: AES-256 GCM encrypts files moving into the Vault.
- **Authentication**: BiometricPrompt intercepts Vault access.

## 4. Plugin Architecture
Plugins will implement strict interfaces, e.g., `CloudStorageProvider`. The Core will interact with plugins through these abstractions, keeping the Core decoupled from specific cloud or ML SDKs.

## Architecture Lock
This architecture is frozen. No breaking changes will be made to this structure without prior approval.
