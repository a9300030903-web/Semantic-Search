package com.example

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.core.model.MediaFile
import com.example.feature.SmartManagerViewModel
import com.example.ui.theme.AppTheme
import com.example.ui.theme.BhagwaOrange
import com.example.ui.theme.CosmicBlue
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.SoftGold
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class MainActivity : FragmentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    var isAuthenticated by remember { mutableStateOf(false) }
                    
                    val viewModel: com.example.feature.SmartManagerViewModel = koinViewModel()
                    
                    if (showSplash) {
                        com.example.ui.SplashScreen(onTimeout = { 
                            // When splash ends, trigger biometric auth
                            viewModel.authenticateOnStartup(this) { success ->
                                if (success) {
                                    isAuthenticated = true
                                    showSplash = false
                                } else {
                                    // Handle failure (e.g., stay on splash or show error)
                                    // For now, we allow retry or exit
                                }
                            }
                        })
                    } else if (isAuthenticated) {
                        MainAppContent(viewModel = viewModel)
                    } else {
                        // Fallback if not authenticated but splash is gone
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Button(onClick = {
                                viewModel.authenticateOnStartup(this@MainActivity) { success ->
                                    if (success) isAuthenticated = true
                                }
                            }) {
                                Text("Retry Authentication")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: SmartManagerViewModel = koinViewModel()) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val plugins by viewModel.plugins.collectAsStateWithLifecycle()
    val isVaultLocked by viewModel.isVaultLocked.collectAsStateWithLifecycle()
    val selectedDetailFile by viewModel.selectedDetailFile.collectAsStateWithLifecycle()
    val scanWorkState by viewModel.scanWorkState.collectAsStateWithLifecycle()

    val isScanning = scanWorkState.isScanning
    val progress = scanWorkState.progress
    val scanStatus = scanWorkState.status

    // Observe selected detail file to show the dialog
    selectedDetailFile?.let { file ->
        MediaDetailDialog(file = file, viewModel = viewModel, onDismiss = { viewModel.selectDetailFile(null) })
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.triggerMediaScan() },
                containerColor = SoftGold,
                contentColor = CosmicBlue,
                icon = { Icon(Icons.Default.Refresh, contentDescription = "Scan Media", tint = CosmicBlue) },
                text = { Text("Scan Media", fontWeight = FontWeight.Bold, color = CosmicBlue) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CosmicBlue,
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard", tint = if (currentTab == 0) BhagwaOrange else Color.LightGray) },
                    label = { Text("Home", color = if (currentTab == 0) Color.White else Color.LightGray, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = CosmicBlue.copy(alpha = 0.4f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Files", tint = if (currentTab == 1) BhagwaOrange else Color.LightGray) },
                    label = { Text("Files", color = if (currentTab == 1) Color.White else Color.LightGray, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = CosmicBlue.copy(alpha = 0.4f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Vault", tint = if (currentTab == 2) BhagwaOrange else Color.LightGray) },
                    label = { Text("Vault", color = if (currentTab == 2) Color.White else Color.LightGray, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = CosmicBlue.copy(alpha = 0.4f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "AI Search", tint = if (currentTab == 3) BhagwaOrange else Color.LightGray) },
                    label = { Text("AI Search", color = if (currentTab == 3) Color.White else Color.LightGray, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = CosmicBlue.copy(alpha = 0.4f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { viewModel.selectTab(4) },
                    icon = { Icon(Icons.Default.Star, contentDescription = "AI Intel", tint = if (currentTab == 4) BhagwaOrange else Color.LightGray) },
                    label = { Text("AI Intel", color = if (currentTab == 4) Color.White else Color.LightGray, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = CosmicBlue.copy(alpha = 0.4f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 5,
                    onClick = { viewModel.selectTab(5) },
                    icon = { Icon(Icons.Default.Cloud, contentDescription = "Cloud", tint = if (currentTab == 5) BhagwaOrange else Color.LightGray) },
                    label = { Text("Cloud", color = if (currentTab == 5) Color.White else Color.LightGray, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = CosmicBlue.copy(alpha = 0.4f)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 6,
                    onClick = { viewModel.selectTab(6) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Plugins", tint = if (currentTab == 6) BhagwaOrange else Color.LightGray) },
                    label = { Text("Plugins", color = if (currentTab == 6) Color.White else Color.LightGray, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = CosmicBlue.copy(alpha = 0.4f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isScanning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CosmicBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.size(24.dp),
                            color = SoftGold,
                            strokeWidth = 3.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Media Scan in Progress ($progress%)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(scanStatus, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                        }
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.width(60.dp).clip(RoundedCornerShape(2.dp)),
                            color = SoftGold,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentTab) {
                    0 -> DashboardScreen(viewModel)
                    1 -> FilesScreen(viewModel)
                    2 -> VaultScreen(viewModel)
                    3 -> AISearchScreen(viewModel)
                    4 -> AIIntelligenceScreen(viewModel)
                    5 -> CloudScreen(viewModel)
                    6 -> PluginsScreen(viewModel)
                }
            }
        }
    }
}

// ========================================================
// 1. DASHBOARD SCREEN
// ========================================================
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
    val coroutineScope = rememberCoroutineScope()

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

// ========================================================
// 2. FILES & DUPLICATE CLEANER SCREEN
// ========================================================
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

// ========================================================
// 3. SECURE VAULT SCREEN
// ========================================================
@Composable
fun VaultScreen(viewModel: SmartManagerViewModel) {
    val isVaultLocked by viewModel.isVaultLocked.collectAsStateWithLifecycle()
    val vaultPinInput by viewModel.vaultPinInput.collectAsStateWithLifecycle()
    val vaultErrorMessage by viewModel.vaultErrorMessage.collectAsStateWithLifecycle()
    val vaultFiles by viewModel.vaultFiles.collectAsStateWithLifecycle()

    if (isVaultLocked) {
        // Vault Lock Screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(CosmicBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = CosmicBlue, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("VVF Secure Vault", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = CosmicBlue)
            Text("Vault is encrypted using 256-bit AES SQLCipher", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = vaultPinInput,
                onValueChange = { viewModel.setPinInput(it) },
                label = { Text("Enter 4-Digit Security PIN") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.width(220.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
            )

            if (vaultErrorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(vaultErrorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.unlockVault() },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicBlue)
                ) {
                    Text("Unlock")
                }
                OutlinedButton(
                    onClick = { viewModel.unlockWithBiometrics() }
                ) {
                    Icon(Icons.Default.Face, contentDescription = "Biometrics", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Biometric Unlock")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Hint: Default PIN is 1234", style = MaterialTheme.typography.bodySmall, color = Color.Gray.copy(alpha = 0.7f))
        }
    } else {
        // Vault Unlocked Content
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
                Column {
                    Text("VVF Secure Vault Unlocked", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    Text("AES-256 On-Device Encryption active", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Button(
                    onClick = { viewModel.lockVault() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lock")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (vaultFiles.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No files in the Secure Vault. Move files here from Files tab.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(vaultFiles, key = { it.id }) { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CosmicBlue.copy(alpha = 0.04f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Lock, contentDescription = "Vault File", tint = CosmicBlue)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(file.name, fontWeight = FontWeight.Bold)
                                        Text("Encrypted • ${file.size / 1024} KB", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                                Button(
                                    onClick = { viewModel.toggleFileEncryption(file) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftGold)
                                ) {
                                    Text("Decrypt", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// 4. AI SEARCH & OCR SCREEN
// ========================================================
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
                                    Divider()
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

// ========================================================
// 5. AI INTELLIGENCE SCREEN
// ========================================================
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

// ========================================================
// 6. CLOUD SYNC SCREEN
// ========================================================
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

// ========================================================
// 7. PLUGINS MANAGER SCREEN
// ========================================================
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

// ========================================================
// 8. METADATA DETAIL DIALOG AND DISTRIBUTION COMPONENTS
// ========================================================
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

// ========================================================
// 9. DISK SPACE USAGE BREAKDOWN WIDGET & GEMINI AUTO-TAGGING HUB
// ========================================================
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
