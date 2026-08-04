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
import com.example.ui.theme.*
import com.example.ui.screens.*
import com.example.ui.components.MediaDetailDialog
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
    val isVaultLocked by viewModel.isVaultLocked.collectAsStateWithLifecycle()
    val selectedDetailFile by viewModel.selectedDetailFile.collectAsStateWithLifecycle()
    val scanWorkState by viewModel.scanWorkState.collectAsStateWithLifecycle()

    val progress = scanWorkState.progress
    val scanStatus = scanWorkState.status
    val isScanning = scanWorkState.isScanning

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
