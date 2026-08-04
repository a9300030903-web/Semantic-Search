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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.feature.SmartManagerViewModel
import com.example.ui.theme.*

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
