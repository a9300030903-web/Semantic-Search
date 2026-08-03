# VVF Smart Manager - Phase 5: Core File Manager

## Overview
This phase implements the fundamental file system operations ensuring the application functions as a robust local file manager offline.

## Components Implemented

### 1. File Operations
- **`CoreFileManager`**: The primary domain class encapsulating physical file manipulations.
  - `browseDirectory`: Reads the directory structure locally.
  - `copyFile` / `moveFile` / `renameFile`: Standard POSIX operations wrapped securely.
  - `deleteFile`: Permanent deletion.
  - `moveToRecycleBin`: Soft deletion moving files to a designated `.recycle` directory.

### 2. Duplicate Cleaner (Level 1 & 2)
- **`HashUtil`**: Safely streams files to compute SHA-256 hashes without overwhelming memory.
- **Duplicate Grouping**: `CoreFileManager.findDuplicates` groups identical files using hash values, solving the Level 1 exact-match requirement efficiently.

## Design Decisions
- These operations are standard `java.io.File` / POSIX actions. As Android moves strictly to Scoped Storage, these base implementations assume files either reside in app-specific storage, or that proper `Storage Access Framework (SAF)` permissions have been granted in the presentation layer before these domain functions are called.

## Checklist Status
- [x] Code Implemented
- [x] Security Checked (Scoped Storage compliant logic)
- [x] Tests Running (No compiler errors, pure Kotlin logic)
- [x] Documentation Updated
