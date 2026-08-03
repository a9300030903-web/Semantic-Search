# VVF Smart Manager - Phase 7: Search Engine (Core)

## Overview
This phase implements the core, blazing-fast search capabilities using Room's Full Text Search (FTS4). This enables instantaneous searches across filenames, metadata, tags, and extracted OCR text.

## Components Implemented

### 1. FTS Database Entities
- **`MediaFileFts`**: A Room `@Fts4` entity that mirrors the searchable columns of `MediaFile` (`name`, `tags`, `ocrText`).
- **`MediaFileDao`**: Added the `searchFilesFts` method utilizing the `MATCH` operator to perform highly optimized searches.

### 2. Search Domain Layer
- **`CoreSearchEngine`**: A domain-level class that wraps the DAO search logic. It prepares the search query (e.g., adding wildcards for prefix matching) and returns a reactive `Flow<List<MediaFile>>` to the UI layer.

## Search Strategy Note
The FTS-based search will always remain the primary and fastest way to retrieve files locally. Semantic Search (TFLite) will run on top of this or as an alternative intelligent filter (Phase 9), but the core keyword/FTS search ensures there is always a lightning-fast offline search available.

## Checklist Status
- [x] Code Implemented
- [x] Security Checked (Data remains fully encrypted via SQLCipher)
- [x] Tests Running
- [x] Documentation Updated
