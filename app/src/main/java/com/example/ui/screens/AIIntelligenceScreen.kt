package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.feature.SmartManagerViewModel
import com.example.ui.theme.*

@Composable
fun AIIntelligenceScreen(viewModel: SmartManagerViewModel) {
    val similarityThreshold by viewModel.similarityThreshold.collectAsStateWithLifecycle()
    val semanticDuplicates by viewModel.semanticDuplicates.collectAsStateWithLifecycle()
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text("AI Intelligence Panel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicBlue)
            Text("Phase 10: Deep Duplicate Cleaner (Level 3-4 Similarity)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        if (plugins["semantic"] != true) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("AI Semantic plugin is disabled. Enable it in the Plugins tab to use visual and text duplicate analysis.", color = Color.Red, textAlign = TextAlign.Center)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Similarity Threshold Sensitivity", fontWeight = FontWeight.Bold, color = BhagwaOrange)
                        Badge(containerColor = SoftGold, contentColor = CosmicBlue) {
                            Text("${(similarityThreshold * 100).toInt()}% Match", modifier = Modifier.padding(4.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("70% Loose Matching (scans, receipts) → 95% Exact Matches", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Slider(
                        value = similarityThreshold,
                        onValueChange = { viewModel.updateSimilarityThreshold(it) },
                        valueRange = 0.70f..0.95f,
                        colors = SliderDefaults.colors(
                            thumbColor = BhagwaOrange,
                            activeTrackColor = CosmicBlue
                        )
                    )
                }
            }

            Text("Identified Semantic Duplicate Documents", fontWeight = FontWeight.Bold, color = CosmicBlue)

            if (semanticDuplicates.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No duplicate documents found at this threshold", fontWeight = FontWeight.Bold)
                        Text("Try lowering the threshold slider.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(semanticDuplicates, key = { it.first.id.toString() + "_" + it.second.id.toString() }) { pair ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Possible Duplicate Documents", fontWeight = FontWeight.Bold, color = BhagwaOrange)
                                    Badge(containerColor = EmeraldGreen, contentColor = Color.White) {
                                        Text("AI Cluster Found", modifier = Modifier.padding(4.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pair.first.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Original File • Size: ${pair.first.size / 1024} KB", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Badge(containerColor = CosmicBlue, contentColor = Color.White) {
                                        Text("Main", modifier = Modifier.padding(4.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pair.second.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Duplicate Scan • Size: ${pair.second.size / 1024} KB", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteFile(pair.second.id, pair.second.name) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete duplicate", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
