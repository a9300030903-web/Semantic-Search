package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.feature.SmartManagerViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(viewModel: SmartManagerViewModel) {
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()

    val pluginList = listOf(
        Triple("ocr", "ML Kit OCR Engine", "Extracts metadata and text dynamically from documents & scanned images on-demand to construct searchable files."),
        Triple("semantic", "AI Semantic Search & Matching", "Powers natural language search query ranking and identifies structural/meaning similarity clusters."),
        Triple("onedrive", "Microsoft OneDrive Sync", "Dynamically extends the core cloud backup with companion OneDrive syncing API hooks."),
        Triple("dropbox", "Dropbox Cloud Storage Plugin", "Plugin to map backup profiles to secure Dropbox REST endpoints."),
        Triple("nextcloud", "NextCloud Personal NAS Backup", "Dynamically sync your offline folders to your personal self-hosted cloud repository.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("Master Plugin Architecture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicBlue)
            Text("Phase 14: Dynamically configure modular add-ons and plugins", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pluginList) { (key, name, desc) ->
                val isEnabled = plugins[key] ?: false
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEnabled) CosmicBlue.copy(alpha = 0.03f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold, color = CosmicBlue)
                                Badge(
                                    containerColor = if (isEnabled) EmeraldGreen else Color.LightGray,
                                    contentColor = Color.White
                                ) {
                                    Text(if (isEnabled) "Active Plugin" else "Inactive", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { viewModel.togglePlugin(key) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BhagwaOrange,
                                    checkedTrackColor = CosmicBlue
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}
