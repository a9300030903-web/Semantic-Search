#!/bin/bash
sed -i '/val gitHubSyncProvider: GitHubSyncProvider,/d' app/src/main/java/com/example/feature/SmartManagerViewModel.kt
sed -i '/import com.example.plugin.cloud.GitHubSyncProvider/d' app/src/main/java/com/example/feature/SmartManagerViewModel.kt
