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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.feature.SmartManagerViewModel
import com.example.ui.theme.*

@Composable
fun AISearchScreen(viewModel: SmartManagerViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()

    val selectedOcrFile by viewModel.selectedOcrFile.collectAsStateWithLifecycle()
    val extractedOcrText by viewModel.extractedOcrText.collectAsStateWithLifecycle()
    val suggestedTags by viewModel.suggestedTags.collectAsStateWithLifecycle()
    val suggestedCategory by viewModel.suggestedCategory.collectAsStateWithLifecycle()
    val isOcrLoading by viewModel.isOcrLoading.collectAsStateWithLifecycle()

    var activeSearchTab by remember { mutableStateOf(0) } // 0=AI Hybrid Search, 1=ML Kit OCR

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = activeSearchTab) {
            Tab(selected = activeSearchTab == 0, onClick = { activeSearchTab = 0 }) {
                Text("AI Hybrid Search", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeSearchTab == 1, onClick = { activeSearchTab = 1 }) {
                Text("ML Kit OCR", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeSearchTab == 0) {
            // Search UI
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Hybrid Query (e.g., 'Starbucks coffee receipt')") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (plugins["semantic"] == true) {
                        Badge(containerColor = SoftGold, contentColor = CosmicBlue) {
                            Text("Semantic ON", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    } else {
                        Badge(containerColor = Color.LightGray, contentColor = Color.DarkGray) {
                            Text("Classic Only", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (searchQuery.isBlank()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Type query to begin search. Combines fast FTS + TFLite rank re-ordering.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else if (searchResults.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No results found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults, key = { it.id }) { file ->
                        val matchedScore = remember(searchQuery, file) {
                            val qLower = searchQuery.lowercase().trim()
                            val qTokens = qLower.split("\\s+".toRegex()).filter { it.isNotBlank() }
                            if (qTokens.isEmpty()) 0.1f
                            else {
                                var score = 0.2f
                                val nameL = file.name.lowercase()
                                val tagsL = file.tags.lowercase()
                                val ocrL = file.ocrText.lowercase()
                                if (nameL.contains(qLower)) score += 0.5f
                                if (tagsL.contains(qLower)) score += 0.3f
                                if (ocrL.contains(qLower)) score += 0.2f
                                for (tok in qTokens) {
                                    if (nameL.contains(tok)) score += 0.2f
                                    if (tagsL.contains(tok)) score += 0.15f
                                    if (ocrL.contains(tok)) score += 0.1f
                                }
                                score.coerceIn(0.1f, 0.99f)
                            }
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectDetailFile(file) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(file.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Badge(
                                        containerColor = if (matchedScore > 0.4f) EmeraldGreen else SoftGold,
                                        contentColor = Color.White
                                    ) {
                                        Text("${(matchedScore * 100).toInt()}% Semantic Match", modifier = Modifier.padding(4.dp))
                                    }
                                }
                                Text("Type: ${file.type} • Path: ${file.path}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                if (file.ocrText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(6.dp)
                                    ) {
                                        Text(file.ocrText, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // OCR UI
            if (plugins["ocr"] != true) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("OCR Plugin is disabled. Please enable it in the Plugins tab.", color = Color.Red, textAlign = TextAlign.Center)
                }
            } else {
                if (selectedOcrFile == null) {
                    // List of files to run OCR on
                    Text("Select a file to run ML Kit OCR analysis", fontWeight = FontWeight.Bold, color = CosmicBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files, key = { it.id }) { file ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectOcrFile(file) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (file.type == "Image") Icons.Default.Image else Icons.Default.List,
                                            contentDescription = "file icon",
                                            tint = BhagwaOrange
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(file.name, fontWeight = FontWeight.Bold)
                                            Text("Status: ${if (file.ocrText.isNotBlank()) "Processed" else "Awaiting OCR"}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = CosmicBlue)
                                }
                            }
                        }
                    }
                } else {
                    // OCR Active File View
                    val file = selectedOcrFile!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.selectOcrFile(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back to list")
                        }
                        Text("ML Kit Parser", fontWeight = FontWeight.Bold, color = CosmicBlue)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Analyzing: ${file.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Type: ${file.type} • Size: ${file.size / 1024} KB", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { viewModel.runOcrOnSelectedFile() },
                                colors = ButtonDefaults.buttonColors(containerColor = BhagwaOrange),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isOcrLoading
                            ) {
                                if (isOcrLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Text("Extract Text & Categories via ML Kit")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (extractedOcrText.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            colors = CardDefaults.cardColors(containerColor = CosmicBlue.copy(alpha = 0.05f))
                        ) {
                            LazyColumn(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                                item {
                                    Text("Extracted Metadata Results:", fontWeight = FontWeight.Bold, color = CosmicBlue)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(extractedOcrText, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("AI Intelligence Auto-Tags:", fontWeight = FontWeight.Bold, color = BhagwaOrange)
                                    Text(suggestedTags, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("AI Suggested Category:", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                    Box(
                                        modifier = Modifier
                                            .background(EmeraldGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(suggestedCategory, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                    }
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Click the button to extract text and analyze headers dynamically.", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
