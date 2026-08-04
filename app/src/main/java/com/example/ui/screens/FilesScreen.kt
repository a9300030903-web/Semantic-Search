package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.MediaFile
import com.example.feature.SmartManagerViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(viewModel: SmartManagerViewModel) {
    val files by viewModel.files.collectAsStateWithLifecycle()
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileType by remember { mutableStateOf("Image") } // Image, Video, Document
    var newFileSize by remember { mutableStateOf("1024") }
    var newFileOcrText by remember { mutableStateOf("") }
    var newFileTags by remember { mutableStateOf("") }

    var duplicateFilterActive by remember { mutableStateOf(false) } // show Duplicates sub-pane

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Local Storage Workspace",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CosmicBlue
            )
            Button(
                onClick = { duplicateFilterActive = !duplicateFilterActive },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (duplicateFilterActive) SoftGold else CosmicBlue
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Duplicates", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (duplicateFilterActive) "All Files" else "Duplicate Cleaner", fontSize = 11.sp)
            }
        }
        Text("Directory: Storage > VVFManager", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        Spacer(modifier = Modifier.height(12.dp))

        if (duplicateFilterActive) {
            // Level 1-2 Duplicate Cleaner interface
            Text("Duplicate Cleaner (Level 1 Hash & Level 2 Metadata)", fontWeight = FontWeight.Bold, color = BhagwaOrange)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calculate duplicates locally
            val duplicates = remember(files) {
                val groups = mutableMapOf<String, MutableList<MediaFile>>()
                for (f in files) {
                    // Level 1: Hash simulation or level 2 metadata fallback (name + size)
                    val key = f.tags.substringBefore(",") + "_" + f.size
                    groups.getOrPut(key) { mutableListOf() }.add(f)
                }
                groups.filterValues { it.size > 1 }
            }

            if (duplicates.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Level 1 & 2 duplicates found!", fontWeight = FontWeight.Bold)
                        Text("All media files are unique.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            } else {
                val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()) }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    duplicates.forEach { (key, list) ->
                        item(key = key) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Duplicate Group (${list.first().type})", fontWeight = FontWeight.Bold, color = CosmicBlue, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    list.forEachIndexed { index, file ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(file.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text("Size: ${file.size / 1024} KB • Modified: ${dateFormat.format(java.util.Date(file.modifiedAt))}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Row {
                                                if (index > 0) {
                                                    IconButton(
                                                        onClick = { viewModel.deleteFile(file.id, file.name) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete duplicate", tint = Color.Red)
                                                    }
                                                } else {
                                                    Badge(containerColor = EmeraldGreen, contentColor = Color.White) {
                                                        Text("Keep", modifier = Modifier.padding(4.dp))
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
            }
        } else {
            // General Files List
            if (files.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No files in local storage. Click + to add.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files, key = { it.id }) { file ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectDetailFile(file) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
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
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(file.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${file.type} • ${file.size / 1024} KB", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                    Row {
                                        // Encrypt / Move to Vault
                                        IconButton(onClick = { viewModel.toggleFileEncryption(file) }) {
                                            Icon(Icons.Default.Lock, contentDescription = "Encrypt", tint = CosmicBlue)
                                        }
                                        // Queue to Cloud
                                        IconButton(onClick = { viewModel.addToCloudQueue(file) }) {
                                            Icon(Icons.Default.Cloud, contentDescription = "Queue to Cloud", tint = EmeraldGreen)
                                        }
                                        // Delete
                                        IconButton(onClick = { viewModel.deleteFile(file.id, file.name) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                        }
                                    }
                                }
                                if (file.tags.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        file.tags.split(",").forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .background(SkyCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(tag.trim(), fontSize = 10.sp, color = CosmicBlue, fontWeight = FontWeight.SemiBold)
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

        Spacer(modifier = Modifier.height(16.dp))

        // Add File Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = BhagwaOrange,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add File")
        }
    }

    // Add File Dialog
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add Media File", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CosmicBlue)
                    
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("File Name (without extension)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("File Type", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Image", "Video", "Document").forEach { type ->
                            ElevatedFilterChip(
                                selected = newFileType == type,
                                onClick = { newFileType = type },
                                label = { Text(type) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newFileSize,
                        onValueChange = { newFileSize = it },
                        label = { Text("File Size (KB)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newFileTags,
                        onValueChange = { newFileTags = it },
                        label = { Text("Tags (comma separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newFileOcrText,
                        onValueChange = { newFileOcrText = it },
                        label = { Text("Simulated OCR Text (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newFileName.isNotBlank()) {
                                    val size = newFileSize.toLongOrNull() ?: 1024L
                                    viewModel.addFile(newFileName, newFileType, size, newFileOcrText, newFileTags)
                                    showAddDialog = false
                                    newFileName = ""
                                    newFileOcrText = ""
                                    newFileTags = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BhagwaOrange)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}
