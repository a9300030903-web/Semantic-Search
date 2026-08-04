package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.feature.SmartManagerViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudScreen(viewModel: SmartManagerViewModel) {
    val cloudSyncing by viewModel.cloudSyncing.collectAsStateWithLifecycle()
    val cloudQueue by viewModel.cloudQueue.collectAsStateWithLifecycle()
    val cloudLogs by viewModel.cloudLogs.collectAsStateWithLifecycle()
    val isCloudConnected by viewModel.isCloudConnected.collectAsStateWithLifecycle()
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()
    val cloudSyncWorkState by viewModel.cloudSyncWorkState.collectAsStateWithLifecycle()

    var gitHubOwner by remember { mutableStateOf(viewModel.gitHubSyncProvider.repoOwner) }
    var gitHubRepo by remember { mutableStateOf(viewModel.gitHubSyncProvider.repoName) }
    var gitHubToken by remember { mutableStateOf(viewModel.gitHubSyncProvider.personalAccessToken) }
    var showTokenConfig by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Cloud & Repository Sync Manager", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicBlue)
                Text("Phase 11 & 12: WorkManager Background Sync & Cloud Plugins", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        // GitHub Linked Repository Sync Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🐙", fontSize = 24.sp)
                            Column {
                                Text("GitHub Repo Metadata Sync", fontWeight = FontWeight.Bold, color = CosmicBlue)
                                Text("WorkManager background push: $gitHubOwner/$gitHubRepo", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { showTokenConfig = !showTokenConfig }) {
                            Icon(Icons.Default.Settings, contentDescription = "Config Repo", tint = CosmicBlue)
                        }
                    }

                    if (showTokenConfig) {
                        OutlinedTextField(
                            value = gitHubOwner,
                            onValueChange = { gitHubOwner = it; viewModel.gitHubSyncProvider.repoOwner = it },
                            label = { Text("Repository Owner/Org") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = gitHubRepo,
                            onValueChange = { gitHubRepo = it; viewModel.gitHubSyncProvider.repoName = it },
                            label = { Text("Repository Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = gitHubToken,
                            onValueChange = { gitHubToken = it; viewModel.gitHubSyncProvider.personalAccessToken = it },
                            label = { Text("GitHub Personal Access Token (Optional)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    if (cloudSyncWorkState.isScanning) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("WorkManager Sync Active...", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BhagwaOrange)
                                Text("${cloudSyncWorkState.progress}%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BhagwaOrange)
                            }
                            LinearProgressIndicator(
                                progress = { cloudSyncWorkState.progress / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = BhagwaOrange
                            )
                            Text(cloudSyncWorkState.status, fontSize = 11.sp, color = Color.Gray)
                        }
                    } else if (cloudSyncWorkState.status.isNotBlank()) {
                        Text("Status: ${cloudSyncWorkState.status}", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = { viewModel.triggerGitHubSync() },
                        enabled = !cloudSyncWorkState.isScanning,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BhagwaOrange)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (cloudSyncWorkState.isScanning) "Syncing via WorkManager..." else "Trigger Background Sync (WorkManager)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Google Drive Core Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🍁", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Google Drive Sync (Core)", fontWeight = FontWeight.Bold)
                                Text("Official Google REST API integration", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Button(
                            onClick = { viewModel.connectToGoogleDrive() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCloudConnected) Color.Red else EmeraldGreen
                            )
                        ) {
                            Text(if (isCloudConnected) "Disconnect" else "Authenticate")
                        }
                    }
                }
            }
        }

        // Cloud Companions Plugins Toggles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Active Cloud Companions (Plugins)", fontWeight = FontWeight.Bold, color = CosmicBlue)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("onedrive" to "OneDrive", "dropbox" to "Dropbox", "nextcloud" to "NextCloud").forEach { (key, name) ->
                        val isEnabled = plugins[key] ?: false
                        FilterChip(
                            selected = isEnabled,
                            onClick = { viewModel.toggleCloudProvider(key, !isEnabled) },
                            label = { Text(name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Upload Queue
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Offline Upload Queue (${cloudQueue.size} files)", fontWeight = FontWeight.Bold, color = CosmicBlue)
                Button(
                    onClick = { viewModel.syncCloudQueue() },
                    colors = ButtonDefaults.buttonColors(containerColor = BhagwaOrange),
                    enabled = !cloudSyncing && isCloudConnected && cloudQueue.isNotEmpty()
                ) {
                    if (cloudSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Sync Now")
                    }
                }
            }
        }

        if (cloudQueue.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color.LightGray.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("All local changes synced. Storage up-to-date.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            items(cloudQueue, key = { it.id }) { file ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Badge(containerColor = SoftGold) {
                            Text("Awaiting Sync", modifier = Modifier.padding(2.dp))
                        }
                    }
                }
            }
        }

        // Cloud Console Sync Logs
        item {
            Text("Cloud Connection Logs (Real-time Console)", fontWeight = FontWeight.Bold, color = BhagwaOrange)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(cloudLogs, key = { it.hashCode() + it.length }) { log ->
                        Text(
                            text = log,
                            color = Color.Green,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
