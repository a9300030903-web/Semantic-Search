#!/bin/bash
# Remove the last closing brace so we can append
sed -i '$d' app/src/main/java/com/example/feature/SmartManagerViewModel.kt

cat << 'INNER_EOF' >> app/src/main/java/com/example/feature/SmartManagerViewModel.kt

    // ----------------------------------------------------
    // AI Duplicates Operations
    // ----------------------------------------------------
    fun updateSimilarityThreshold(threshold: Float) {
        _similarityThreshold.value = threshold
    }

    // ----------------------------------------------------
    // Cloud Operations
    // ----------------------------------------------------
    fun addCloudLog(log: String) {
        _cloudLogs.value = listOf(log) + _cloudLogs.value.take(49)
    }

    fun connectToGoogleDrive() {
        viewModelScope.launch {
            if (!_isCloudConnected.value) {
                addCloudLog("Connecting to Google Drive...")
                driveProvider.authenticate()
                if (driveProvider.isAuthenticated()) {
                    _isCloudConnected.value = true
                    addCloudLog("Connected to Google Drive successfully.")
                } else {
                    addCloudLog("Failed to connect to Google Drive.")
                }
            } else {
                _isCloudConnected.value = false
                addCloudLog("Disconnected from Google Drive.")
            }
        }
    }

    fun toggleCloudProvider(provider: String, isEnabled: Boolean) {
        val current = _plugins.value.toMutableMap()
        current[provider] = isEnabled
        _plugins.value = current
    }

    fun addToCloudQueue(file: MediaFile) {
        val current = _cloudQueue.value.toMutableList()
        if (current.none { it.id == file.id }) {
            current.add(file)
            _cloudQueue.value = current
            addCloudLog("Added ${file.name} to upload queue.")
        }
    }

    fun syncCloudQueue() {
        viewModelScope.launch {
            if (_cloudQueue.value.isEmpty() || !_isCloudConnected.value) return@launch
            _cloudSyncing.value = true
            addCloudLog("Starting sync for ${_cloudQueue.value.size} files...")
            
            val result = driveProvider.syncChanges()
            if (result.isSuccess) {
                _cloudQueue.value = emptyList()
                addCloudLog("Sync completed successfully.")
            } else {
                addCloudLog("Sync failed: ${result.exceptionOrNull()?.message}")
            }
            _cloudSyncing.value = false
        }
    }

    fun togglePlugin(pluginId: String, isEnabled: Boolean) {
        val current = _plugins.value.toMutableMap()
        current[pluginId] = isEnabled
        _plugins.value = current
    }
}
INNER_EOF
