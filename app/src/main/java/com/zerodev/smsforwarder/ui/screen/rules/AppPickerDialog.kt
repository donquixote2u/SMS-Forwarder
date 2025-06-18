package com.zerodev.smsforwarder.ui.screen.rules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.zerodev.smsforwarder.data.repository.AppInfo
import com.zerodev.smsforwarder.data.repository.AppRepository
import kotlinx.coroutines.delay

/**
 * Data class representing the selected app information.
 */
data class SelectedApp(
    val packageName: String,
    val appName: String
)

/**
 * Dialog for picking an app from installed applications with search functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (SelectedApp) -> Unit,
    appRepository: AppRepository = hiltViewModel<AppPickerViewModel>().appRepository
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchSuggestions by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Debounced search effect
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchSuggestions = emptyList()
            return@LaunchedEffect
        }
        
        isLoading = true
        delay(300) // Debounce delay
        
        try {
            // Use AppRepository's direct search method
            val suggestions = appRepository.getSearchSuggestions(searchQuery, 15)
            searchSuggestions = suggestions
        } catch (e: Exception) {
            searchSuggestions = emptyList()
        } finally {
            isLoading = false
        }
    }
    
    // Collect MRU apps for initial display
    val mruApps by appRepository.mruApps.collectAsState()
    
    // Initialize repository when dialog opens
    LaunchedEffect(Unit) {
        appRepository.initialize()
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // Allow custom width
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select App",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content
                if (searchQuery.isEmpty()) {
                    // Show MRU apps when not searching
                    if (mruApps.isNotEmpty()) {
                        Text(
                            text = "Recently Used Apps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        LazyColumn {
                            items(mruApps) { app ->
                                AppItem(
                                    appInfo = app,
                                    onClick = {
                                        appRepository.addToMru(app) // Update MRU
                                        onAppSelected(
                                            SelectedApp(
                                                packageName = app.packageName,
                                                appName = app.appName
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        // Empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No recent apps",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Start typing to search for apps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Show search results
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (searchSuggestions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No apps found",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Try a different search term",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Search Results (${searchSuggestions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        LazyColumn {
                            items(searchSuggestions) { app ->
                                AppItem(
                                    appInfo = app,
                                    onClick = {
                                        appRepository.addToMru(app) // Update MRU
                                        onAppSelected(
                                            SelectedApp(
                                                packageName = app.packageName,
                                                appName = app.appName
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual app item in the picker list.
 */
@Composable
private fun AppItem(
    appInfo: AppInfo,
    onClick: () -> Unit
) {
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon - Use remember to handle icon loading safely
            val iconBitmap = remember(appInfo.packageName) {
                try {
                    appInfo.icon.toBitmap(48, 48).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            
            if (iconBitmap != null) {
                androidx.compose.foundation.Image(
                    painter = BitmapPainter(iconBitmap),
                    contentDescription = "${appInfo.appName} icon",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                // Fallback if icon fails to load
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = appInfo.appName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // App Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = appInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (appInfo.isSystemApp) {
                    Text(
                        text = "System App",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Simple ViewModel to inject AppRepository.
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class AppPickerViewModel @javax.inject.Inject constructor(
    val appRepository: AppRepository
) : androidx.lifecycle.ViewModel() 