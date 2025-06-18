package com.zerodev.smsforwarder.ui.screen.settings

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.zerodev.smsforwarder.data.service.NotifRouterService
import com.zerodev.smsforwarder.data.repository.AppRepository

/**
 * Settings screen for permissions and app configuration.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    appRepository: AppRepository = hiltViewModel<SettingsViewModel>().appRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val permissionsToRequest = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    ).apply {
        // Only request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = permissionsToRequest
    )
    
    // Check if notification listener is enabled
    val isNotificationListenerEnabled = remember(context) {
        NotifRouterService.isServiceEnabled(context)
    }
    
    val openNotificationListenerSettings = {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        context.startActivity(intent)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Permissions Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    PermissionItem(
                        title = "SMS Permissions",
                        description = "Required to receive and read SMS messages",
                        icon = Icons.Default.Phone,
                        isGranted = permissionsState.permissions.filter { 
                            it.permission == Manifest.permission.RECEIVE_SMS || 
                            it.permission == Manifest.permission.READ_SMS 
                        }.all { it.status.isGranted },
                        onRequest = { permissionsState.launchMultiplePermissionRequest() }
                    )
                    
                    // Only show notification permission on Android 13+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        PermissionItem(
                            title = "Notification Permission",
                            description = "Required for foreground service notifications (Android 13+)",
                            icon = Icons.Default.Star,
                            isGranted = permissionsState.permissions.find { 
                                it.permission == Manifest.permission.POST_NOTIFICATIONS 
                            }?.status?.isGranted ?: false,
                            onRequest = { permissionsState.launchMultiplePermissionRequest() }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    PermissionItem(
                        title = "Notification Access",
                        description = "Required to capture and forward notifications from other apps",
                        icon = Icons.Default.Notifications,
                        isGranted = isNotificationListenerEnabled,
                        onRequest = openNotificationListenerSettings
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    PermissionItem(
                        title = "Internet Access",
                        description = "Required to forward SMS to HTTP endpoints",
                        icon = Icons.Default.Star,
                        isGranted = true, // INTERNET permission is granted at install time
                        onRequest = { /* No action needed */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Debugging Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Debug Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "To test SMS and notification forwarding:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "SMS Testing:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1. Ensure SMS permissions are granted above",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2. Create an SMS forwarding rule in the Rules tab",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "3. Send an SMS to your phone (from another phone)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Notification Testing:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "1. Ensure Notification Access is granted above",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2. Create a notification forwarding rule in the Rules tab",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "3. Receive a notification from any app (WhatsApp, email, etc.)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "General:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "4. Check the History tab - ALL messages/notifications will appear there",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "5. Check logcat for detailed logs (tag: SmsReceiver, NotifRouterService)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "💡 Tip: The History tab now shows ALL received SMS messages and notifications, even ones that don't match any rules. This helps debug if messages are being received at all.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // App Info Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "App Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InfoRow("Version", "1.0.0")
                    InfoRow("Package", "com.zerodev.smsforwarder")
                    InfoRow("Target SDK", "34 (Android 14)")
                    InfoRow("Min SDK", "29 (Android 10)")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Debug Section (always available for troubleshooting)
            run {
                var debugInfo by remember { mutableStateOf("") }
                var showDebugDialog by remember { mutableStateOf(false) }
                
                Text(
                    text = "Debug Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                debugInfo = appRepository.debugAppVisibility()
                                showDebugDialog = true
                            } catch (e: Exception) {
                                debugInfo = "Debug failed: ${e.message}"
                                showDebugDialog = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test App Visibility")
                }
                
                if (showDebugDialog) {
                    AlertDialog(
                        onDismissRequest = { showDebugDialog = false },
                        title = { Text("App Visibility Debug") },
                        text = {
                            Text(
                                text = debugInfo,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showDebugDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "If app search isn't working:\n" +
                            "1. Check notification access is enabled\n" +
                            "2. Try restarting the app\n" +
                            "3. On Android 11+, some apps may not be visible due to privacy restrictions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isGranted) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Granted",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        } else {
            Button(
                onClick = onRequest,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Grant")
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}