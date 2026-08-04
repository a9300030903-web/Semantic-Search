#!/bin/bash
sed -i '/import com.example.plugin.cloud.GitHubSyncProvider/d' app/src/main/java/com/example/core/di/DatabaseModule.kt
sed -i '/single { GitHubSyncProvider(androidContext(), get()) }/d' app/src/main/java/com/example/core/di/DatabaseModule.kt
sed -i '/gitHubSyncProvider = get(),/d' app/src/main/java/com/example/core/di/DatabaseModule.kt
