#!/bin/bash
sed -i -e '/fun triggerGitHubSync/,+3d' app/src/main/java/com/example/feature/SmartManagerViewModel.kt
sed -i '$d' app/src/main/java/com/example/feature/SmartManagerViewModel.kt
cat << 'INNER_EOF' >> app/src/main/java/com/example/feature/SmartManagerViewModel.kt
    fun triggerGitHubSync() {
        viewModelScope.launch {
            addCloudLog("Triggering WorkManager Drive Sync...")
            backgroundManager.triggerImmediateCloudSync()
        }
    }
}
INNER_EOF
