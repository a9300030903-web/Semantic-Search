package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.MediaFile
import com.example.feature.SmartManagerViewModel
import com.example.ui.theme.*

@Composable
fun MediaDetailDialog(file: MediaFile, viewModel: SmartManagerViewModel, onDismiss: () -> Unit) {
    val isAutoTagging by viewModel.isAutoTagging.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CosmicBlue, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (file.type) {
                        "Video" -> Icons.Default.Movie
                        "Image" -> Icons.Default.Image
                        else -> Icons.Default.List
                    },
                    contentDescription = file.type,
                    tint = BhagwaOrange,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Expanded Metadata",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CosmicBlue
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailItem("File Name", file.name)
                DetailItem("File Path", file.path)
                DetailItem("Storage Location", if (file.isEncrypted) "Secure Vault (Encrypted)" else "Local Storage Workspace")
                DetailItem("File Type", file.type)
                DetailItem("Mime-Type", file.mimeType)
                DetailItem("Size", "${file.size / 1024} KB (${file.size} Bytes)")
                DetailItem("Date Created", java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(file.createdAt)))
                DetailItem("Last Modified", java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(file.modifiedAt)))
                
                DetailItem("Stored AI Tags", if (file.tags.isNotBlank()) file.tags else "No tags stored in Room DB")

                Button(
                    onClick = { viewModel.autoTagFile(file) },
                    enabled = !isAutoTagging,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BhagwaOrange)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Auto Tag", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isAutoTagging) "Tagging with Gemini..." else "✨ Generate Tags with Gemini AI",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (file.ocrText.isNotBlank()) {
                    Text("ML Kit OCR Extracted Text:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CosmicBlue)
                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 100.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                            Text(file.ocrText, fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, color = CosmicBlue, fontWeight = FontWeight.Medium)
    }
}

data class SpaceCategory(
    val name: String,
    val sizeBytes: Long,
    val fileCount: Int,
    val color: Color
)

@Composable
fun DiskSpaceUsageWidget(files: List<MediaFile>, vaultFiles: List<MediaFile>) {
    val images = remember(files) { files.filter { it.type == "Image" } }
    val videos = remember(files) { files.filter { it.type == "Video" } }
    val docs = remember(files) { files.filter { it.type == "Document" } }
    val audio = remember(files) { files.filter { it.type == "Audio" } }
    
    val imagesSize = remember(images) { images.sumOf { it.size } }
    val videosSize = remember(videos) { videos.sumOf { it.size } }
    val docsSize = remember(docs) { docs.sumOf { it.size } }
    val audioSize = remember(audio) { audio.sumOf { it.size } }
    val vaultSize = remember(vaultFiles) { vaultFiles.sumOf { it.size } }
    
    val totalSizeBytes = remember(imagesSize, videosSize, docsSize, audioSize, vaultSize) { 
        imagesSize + videosSize + docsSize + audioSize + vaultSize 
    }
    val totalSizeMb = remember(totalSizeBytes) { if (totalSizeBytes > 0) totalSizeBytes / (1024f * 1024f) else 0f }
    val totalSizeKb = remember(totalSizeBytes) { totalSizeBytes / 1024 }
    
    val categories = remember(imagesSize, videosSize, docsSize, audioSize, vaultSize, images.size, videos.size, docs.size, audio.size, vaultFiles.size) {
        listOf(
            SpaceCategory("Images", imagesSize, images.size, BhagwaOrange),
            SpaceCategory("Videos", videosSize, videos.size, CosmicBlue),
            SpaceCategory("Documents", docsSize, docs.size, EmeraldGreen),
            SpaceCategory("Audio", audioSize, audio.size, SkyCyan),
            SpaceCategory("Vault", vaultSize, vaultFiles.size, SoftGold)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Disk Space Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicBlue
                    )
                    Text(
                        "Storage usage by media type stored in database",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CosmicBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (totalSizeMb >= 1.0f) String.format("%.2f MB Total", totalSizeMb) else "$totalSizeKb KB Total",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicBlue
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Donut Chart Canvas
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(130.dp)) {
                        var startAngle = -90f
                        if (totalSizeBytes == 0L) {
                            drawArc(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 22.dp.toPx())
                            )
                        } else {
                            categories.forEach { cat ->
                                val sweep = (cat.sizeBytes.toFloat() / totalSizeBytes.toFloat()) * 360f
                                if (sweep > 0) {
                                    drawArc(
                                        color = cat.color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = 22.dp.toPx())
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (totalSizeMb >= 1.0f) String.format("%.1f", totalSizeMb) else "$totalSizeKb",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = CosmicBlue
                        )
                        Text(
                            text = if (totalSizeMb >= 1.0f) "MB" else "KB",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Legend List
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val percent = if (totalSizeBytes > 0) (cat.sizeBytes.toFloat() / totalSizeBytes.toFloat() * 100) else 0f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cat.color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    cat.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                String.format("%.1f%% (%d)", percent, cat.fileCount),
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Multi-segment horizontal bar indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.LightGray.copy(alpha = 0.2f))
            ) {
                if (totalSizeBytes > 0) {
                    categories.forEach { cat ->
                        val weight = cat.sizeBytes.toFloat() / totalSizeBytes.toFloat()
                        if (weight > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(weight)
                                    .background(cat.color)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeminiAutoTaggingCard(viewModel: SmartManagerViewModel) {
    val isAutoTagging by viewModel.isAutoTagging.collectAsStateWithLifecycle()
    val autoTagStatus by viewModel.autoTagStatus.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("✨", fontSize = 22.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Gemini AI Auto-Tagging Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CosmicBlue
                    )
                    Text(
                        "Analyze media file metadata & auto-store tags in Room DB",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            if (autoTagStatus.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BhagwaOrange.copy(alpha = 0.1f))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isAutoTagging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = BhagwaOrange
                            )
                        }
                        Text(
                            autoTagStatus,
                            fontSize = 12.sp,
                            color = CosmicBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.autoTagAllFiles() },
                enabled = !isAutoTagging,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BhagwaOrange)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Auto Tag All",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isAutoTagging) "Tagging Files with Gemini AI..." else "Batch Auto-Tag All Files (Gemini AI)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
