package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.feature.SmartManagerViewModel
import com.example.ui.theme.*
import com.example.ui.components.DiskSpaceUsageWidget
import com.example.ui.components.GeminiAutoTaggingCard

@Composable
fun DashboardScreen(viewModel: SmartManagerViewModel) {
    val files by viewModel.files.collectAsStateWithLifecycle()
    val vaultFiles by viewModel.vaultFiles.collectAsStateWithLifecycle()
    val isVaultLocked by viewModel.isVaultLocked.collectAsStateWithLifecycle()
    val isCloudConnected by viewModel.isCloudConnected.collectAsStateWithLifecycle()
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()

    val assistantResponse by viewModel.assistantResponse.collectAsStateWithLifecycle()
    val isAssistantLoading by viewModel.isAssistantLoading.collectAsStateWithLifecycle()
    var assistantQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Banner
        item(key = "header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CosmicBlue, BhagwaOrange)
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // VVF Golden Leaf Brand Logo
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon_1785747912012),
                        contentDescription = "VVF Logo",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "VVF Smart Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Next-Gen AI Media Workspace",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // VVF AI Assistant
        item(key = "assistant") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤖", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("VVF AI Smart Co-Pilot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        if (isAssistantLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.Center))
                        } else {
                            Text(assistantResponse, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = assistantQuery,
                        onValueChange = { assistantQuery = it },
                        placeholder = { Text("Ask Gemini to locate, explain, or tag...") },
                        modifier = Modifier.fillMaxWidth().testTag("search_input"),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (assistantQuery.isNotBlank()) {
                                        viewModel.askCopilot(assistantQuery)
                                        assistantQuery = ""
                                    }
                                },
                                modifier = Modifier.testTag("search_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Ask AI", tint = BhagwaOrange)
                            }
                        }
                    )
                }
            }
        }

        // Summary Statistics Grid
        item(key = "statistics") {
            Text("System Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CosmicBlue.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Default.Folder, contentDescription = "Files", tint = CosmicBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Total Storage", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("${files.size} Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = BhagwaOrange.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = "Vault", tint = BhagwaOrange)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Secure Vault", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                            if (isVaultLocked) "Locked (${vaultFiles.size})" else "Unlocked (${vaultFiles.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Default.Cloud, contentDescription = "Cloud", tint = EmeraldGreen)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Google Drive", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(
                            if (isCloudConnected) "Connected" else "Offline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCloudConnected) EmeraldGreen else Color.Gray
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SkyCyan.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Plugins", tint = SkyCyan)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Active Plugins", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        val activeCount = plugins.values.count { it }
                        Text("$activeCount Enabled", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Data Safety & Privacy Info
        item(key = "privacy_info") {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Privacy Info", tint = EmeraldGreen)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Data Safety & Privacy", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CosmicBlue)
                        Text(
                            "VVF Smart Manager processes your media locally. Sync to Cloud (Drive/GitHub) is optional and encrypted.",
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // 4. Disk Space Breakdown Donut Widget
        item(key = "disk_usage") {
            Spacer(modifier = Modifier.height(8.dp))
            DiskSpaceUsageWidget(files = files, vaultFiles = vaultFiles)
        }

        // 5. Gemini AI Auto-Tagging Hub Widget
        item(key = "auto_tagging") {
            Spacer(modifier = Modifier.height(8.dp))
            GeminiAutoTaggingCard(viewModel = viewModel)
        }

        // 5. Recently Indexed Media
        item(key = "recent_media") {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Recently Indexed Media", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicBlue)
            Spacer(modifier = Modifier.height(4.dp))
            
            if (files.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No indexed files yet. Trigger a Media Scan below!", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        files.take(4).forEachIndexed { index, file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectDetailFile(file) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = when (file.type) {
                                            "Video" -> Icons.Default.Movie
                                            "Image" -> Icons.Default.Image
                                            else -> Icons.Default.List
                                        },
                                        contentDescription = file.type,
                                        tint = BhagwaOrange,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${file.type} • ${file.size / 1024} KB", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "View Details", tint = Color.LightGray)
                            }
                            if (index < files.take(4).size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
}
