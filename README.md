# VVF Smart Manager

VVF Smart Manager is a next-generation AI-powered media management application for Android. It combines traditional file management with advanced AI capabilities like semantic search, OCR, and automated tagging to provide a seamless experience for organizing and protecting your digital assets.

## 🚀 Features

- **Core File Manager**: Browse, copy, move, and delete files with ease. Includes a built-in Duplicate Cleaner (Level 1 & 2).
- **Secure Vault**: Protect sensitive files with AES-256 encryption. Supports PIN and Biometric authentication.
- **AI Hybrid Search**: Combine fast Full-Text Search (FTS) with TFLite-powered semantic ranking to find files based on meaning, not just filenames.
- **ML Kit OCR**: Extract text and metadata from documents and images using on-device Machine Learning.
- **AI Intelligence**: Advanced duplicate detection using visual and semantic similarity (Phase 10).
- **Cloud & Repo Sync**: Synchronize your media metadata with GitHub repositories and backup files to Google Drive or other cloud providers.
- **Modular Plugin System**: Enable or disable advanced features like OCR, Semantic Search, and various Cloud providers dynamically.
- **Gemini AI Integration**: Batch auto-tagging and a smart co-pilot to help you manage your workspace.

## 🛠️ Project Structure

- `app/src/main/java/com/example/`: Core application logic.
  - `MainActivity.kt`: Main entry point, navigation, and authentication.
  - `feature/`: ViewModels and business logic.
  - `core/`: Data models and repositories.
  - `ui/`:
    - `screens/`: Modular UI screens (Dashboard, Files, Vault, etc.).
    - `components/`: Reusable UI components.
    - `theme/`: Material 3 theme definitions.

## ⚙️ Setup Steps

1. **Clone the Project**: Import the codebase into your Android development environment.
2. **Secrets Configuration**: Add your `GEMINI_API_KEY` and other credentials via the AI Studio Secrets panel.
3. **Build**: Run `./gradlew assembleDebug` or use the AI Studio `compile_applet` tool.
4. **Permissions**: The app requires Storage and Notification permissions (and Biometric if enabled).

## 📊 Current Status

- **Architecture**: Refactored to a modular screen-based architecture.
- **Security**: Hardened Vault and Biometric implementation.
- **AI Features**: OCR and Semantic Search functional via plugins.
- **Cloud**: Google Drive (Core) and GitHub Sync (WorkManager) integrated.

---
*Built with ❤️ using Kotlin, Jetpack Compose, and Google AI.*
