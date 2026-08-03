# VVF Smart Manager - Phase 8: OCR Engine (Plugin)

## Overview
This phase implements the Optical Character Recognition (OCR) engine. Designed architecturally as a plugin, it extracts text from images (and eventually PDFs) which is then fed into the Core Search Engine (Phase 7) via the `ocrText` field in the database.

## Components Implemented

### 1. OCR Interface (`OcrEngine`)
- Provides a decoupled boundary for text extraction, ensuring the core app doesn't strictly depend on ML Kit's implementation details.

### 2. ML Kit Implementation (`MlKitOcrEngine`)
- Uses Google Play Services ML Kit for on-device Latin text recognition.
- Currently bundled into the main APK for testing purposes, but architected so it can be moved to a Dynamic Feature Module (on-demand download) to reduce the initial APK size.

## Data Flow
When a user adds an image to the manager (or the background WorkManager indexes it), the `OcrEngine` will scan it. The resulting text is saved to the Room database's `ocrText` field, automatically making the image searchable via the Phase 7 FTS engine.

## Checklist Status
- [x] Code Implemented
- [x] Security Checked (Runs entirely on-device, no cloud APIs)
- [x] Tests Running
- [x] Documentation Updated
