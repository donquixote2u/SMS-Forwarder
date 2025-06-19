package com.zerodev.smsforwarder.ui.screen.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.zerodev.smsforwarder.domain.model.ForwardingHistory
import com.zerodev.smsforwarder.domain.model.StatusColor
import com.zerodev.smsforwarder.data.local.dao.AppInfo
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

/**
 * Check if any filters are currently active.
 */
private fun hasActiveFilters(uiState: HistoryUiState): Boolean {
    return uiState.searchQuery.isNotEmpty() || 
           uiState.selectedApp.isNotEmpty() || 
           uiState.patternFilter.isNotEmpty()
}

/**
 * Screen for viewing SMS forwarding history with pagination and delete functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedHistoryEntry by remember { mutableStateOf<ForwardingHistory?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forwarding History") },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Filter",
                            tint = if (hasActiveFilters(uiState)) MaterialTheme.colorScheme.primary 
                                  else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.refreshHistory() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    if (uiState.history.isNotEmpty()) {
                        IconButton(onClick = { showClearAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear All"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                onClearSearch = { viewModel.updateSearchQuery("") }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Active Filters Display
            if (hasActiveFilters(uiState)) {
                ActiveFiltersRow(
                    uiState = uiState,
                    onClearFilters = viewModel::clearFilters,
                    onRemoveAppFilter = { viewModel.updateAppFilter("") },
                    onRemovePatternFilter = { viewModel.updatePatternFilter("") }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Statistics Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatisticItem("Total", uiState.totalCount.toString())
                        StatisticItem("Matched", uiState.matchedCount.toString())
                        StatisticItem("Success", uiState.successCount.toString())
                        StatisticItem("Failed", uiState.failedCount.toString())
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // History List
            if (uiState.isLoading && uiState.history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.history.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Messages Yet",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "SMS messages and notifications will appear here.\nSend an SMS or trigger a notification to test.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.history,
                        key = { it.id }
                    ) { historyEntry ->
                        SwipeToDeleteHistoryItem(
                            historyEntry = historyEntry,
                            onDelete = { viewModel.deleteHistoryEntry(historyEntry.id) },
                            onClick = { selectedHistoryEntry = historyEntry }
                        )
                    }
                    
                    // Load more button
                    if (uiState.hasMorePages) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isLoadingMore) {
                                    CircularProgressIndicator()
                                } else {
                                    Button(
                                        onClick = { viewModel.loadNextPage() }
                                    ) {
                                        Text("Load More")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Show detailed dialog when history entry is selected
    selectedHistoryEntry?.let { historyEntry ->
        HistoryDetailDialog(
            historyEntry = historyEntry,
            onDismiss = { selectedHistoryEntry = null }
        )
    }
    
    // Clear all confirmation dialog
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = { Text("Clear All History") },
            text = { Text("Are you sure you want to delete all history entries? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            uiState = uiState,
            onDismiss = { showFilterDialog = false },
            onApplyAppFilter = viewModel::updateAppFilter,
            onApplyPatternFilter = viewModel::updatePatternFilter,
            onClearFilters = viewModel::clearFilters
        )
    }
    
    // Show error snackbar if needed
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Error handling could be improved with SnackbarHost
            viewModel.clearError()
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SwipeToDeleteHistoryItem(
    historyEntry: ForwardingHistory,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var itemWidth by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val deleteThreshold = with(density) { 120.dp.toPx() }
    
    val backgroundColor by animateFloatAsState(
        targetValue = if (offsetX < -deleteThreshold * 0.3f) 1f else 0f,
        animationSpec = tween(200),
        label = "background_color"
    )
    
    val iconScale by animateFloatAsState(
        targetValue = if (offsetX < -deleteThreshold * 0.5f) 1.2f else 0.8f,
        animationSpec = tween(200),
        label = "icon_scale"
    )

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Delete background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = backgroundColor)
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (backgroundColor > 0.1f) {
                Row(
                    modifier = Modifier.padding(end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.scale(iconScale)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Main item
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { itemWidth = it.width }
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -deleteThreshold) {
                                onDelete()
                            }
                            offsetX = 0f
                        }
                    ) { _, dragAmount ->
                        val newOffset = offsetX + dragAmount
                        offsetX = newOffset.coerceAtMost(0f).coerceAtLeast(-itemWidth * 0.4f)
                    }
                }
                .clickable { onClick() }
        ) {
            HistoryItemContent(historyEntry = historyEntry)
        }
    }
}

@Composable
private fun HistoryItemContent(
    historyEntry: ForwardingHistory
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (historyEntry.matchedRule) "Rule Matched" else "No Rule Matched",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (historyEntry.matchedRule) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (historyEntry.matchedRule) {
                        Text(
                            text = "✓ Pattern matched",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "✗ Pattern not matched",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                StatusChip(historyEntry = historyEntry)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Display content based on source type
            if (historyEntry.isSms()) {
                Text(
                    text = "SMS: \"${historyEntry.getContentPreview(60)}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "From: ${historyEntry.senderNumber ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Notification: \"${historyEntry.getContentPreview(60)}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "From: ${historyEntry.sourceAppName ?: historyEntry.sourcePackage ?: "Unknown App"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (historyEntry.responseCode != null) {
                Text(
                    text = "Response: ${historyEntry.responseCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (historyEntry.errorMessage != null) {
                Text(
                    text = "Error: ${historyEntry.errorMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Bottom row with timestamp
            Text(
                text = historyEntry.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryItem(
    historyEntry: ForwardingHistory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        HistoryItemContent(historyEntry = historyEntry)
    }
}

@Composable
private fun StatusChip(
    historyEntry: ForwardingHistory
) {
    val colors = when (historyEntry.getStatusColor()) {
        StatusColor.SUCCESS -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        StatusColor.ERROR -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        StatusColor.WARNING -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        StatusColor.INFO -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
    
    Surface(
        color = colors.first,
        contentColor = colors.second,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = historyEntry.getStatusDescription(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun HistoryDetailDialog(
    historyEntry: ForwardingHistory,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (historyEntry.isSms()) "SMS Details" else "Notification Details",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status
                DetailSection(title = "Status") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(historyEntry = historyEntry)
                        Spacer(modifier = Modifier.width(8.dp))
                        if (historyEntry.matchedRule) {
                            Text(
                                text = "✓ Rule matched",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "✗ No rule matched",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // SMS/Notification Information
                if (historyEntry.isSms()) {
                    DetailSection(title = "SMS Information") {
                        DetailRow("From", historyEntry.senderNumber ?: "Unknown")
                        DetailRow("Content", historyEntry.messageBody)
                        DetailRow(
                            "Received", 
                            historyEntry.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
                        )
                    }
                } else {
                    DetailSection(title = "Notification Information") {
                        DetailRow("App", historyEntry.sourceAppName ?: historyEntry.sourcePackage ?: "Unknown")
                        historyEntry.notificationTitle?.let { title ->
                            DetailRow("Title", title)
                        }
                        DetailRow("Content", historyEntry.notificationText ?: historyEntry.messageBody)
                        DetailRow(
                            "Received", 
                            historyEntry.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
                        )
                    }
                }
                
                // Rule Information
                if (historyEntry.matchedRule) {
                    DetailSection(title = "Rule Information") {
                        if (historyEntry.ruleId != null) {
                            DetailRow("Rule ID", historyEntry.ruleId.toString())
                        }
                        historyEntry.endpoint?.let { endpoint ->
                            DetailRow("Endpoint", endpoint)
                        }
                        historyEntry.method?.let { method ->
                            DetailRow("HTTP Method", method)
                        }
                    }
                    
                    // Request Information
                    if (historyEntry.requestBody != null) {
                        DetailSection(title = "Request Information") {
                            DetailRow("Payload", historyEntry.requestBody, isCode = true)
                        }
                    }
                    
                    // Response Information
                    if (historyEntry.responseCode != null || historyEntry.responseBody != null) {
                        DetailSection(title = "Response Information") {
                            historyEntry.responseCode?.let { code ->
                                DetailRow("Response Code", code.toString())
                                DetailRow("HTTP Status", historyEntry.getHttpStatusDescription() ?: "Unknown")
                            }
                            historyEntry.responseBody?.let { body ->
                                DetailRow("Response Body", body, isCode = true)
                            }
                        }
                    }
                } else {
                    // No Rule Matched Information
                    DetailSection(title = "Processing Information") {
                        DetailRow("Reason", historyEntry.errorMessage ?: "No matching rules found")
                    }
                }
                
                // Error Information
                if (historyEntry.errorMessage != null && historyEntry.matchedRule) {
                    DetailSection(title = "Error Information") {
                        DetailRow("Error Message", historyEntry.errorMessage)
                    }
                }
                
                // Timestamps
                DetailSection(title = "Timestamps") {
                    DetailRow(
                        "Processed At", 
                        historyEntry.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isCode: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isCode) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * UI state for the history screen.
 */
data class HistoryUiState(
    val history: List<ForwardingHistory> = emptyList(),
    val totalCount: Int = 0,
    val matchedCount: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val error: String? = null,
    // Filter-related states
    val searchQuery: String = "",
    val selectedApp: String = "",
    val patternFilter: String = "",
    val availableApps: List<AppInfo> = emptyList(),
    val showFilterDialog: Boolean = false
)

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search apps, content, numbers...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearSearch) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        singleLine = true
    )
}

@Composable
private fun ActiveFiltersRow(
    uiState: HistoryUiState,
    onClearFilters: () -> Unit,
    onRemoveAppFilter: () -> Unit,
    onRemovePatternFilter: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Filters:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (uiState.selectedApp.isNotEmpty()) {
            FilterChip(
                selected = true,
                onClick = onRemoveAppFilter,
                label = { 
                    Text(
                        text = "App: ${uiState.selectedApp}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove app filter",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        
        if (uiState.patternFilter.isNotEmpty()) {
            FilterChip(
                selected = true,
                onClick = onRemovePatternFilter,
                label = { 
                    Text(
                        text = "Pattern: ${uiState.patternFilter}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove pattern filter",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        TextButton(onClick = onClearFilters) {
            Text("Clear All")
        }
    }
}

@Composable
private fun FilterDialog(
    uiState: HistoryUiState,
    onDismiss: () -> Unit,
    onApplyAppFilter: (String) -> Unit,
    onApplyPatternFilter: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    var selectedApp by remember { mutableStateOf(uiState.selectedApp) }
    var patternText by remember { mutableStateOf(uiState.patternFilter) }
    var expandedApp by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter History",
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
                
                // App Filter
                Text(
                    text = "Filter by App",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Box {
                    OutlinedTextField(
                        value = selectedApp.ifEmpty { "All Apps" },
                        onValueChange = { },
                        readOnly = true,
                        placeholder = { Text("Select an app") },
                        trailingIcon = {
                            IconButton(onClick = { expandedApp = !expandedApp }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Dropdown"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    DropdownMenu(
                        expanded = expandedApp,
                        onDismissRequest = { expandedApp = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Apps") },
                            onClick = {
                                selectedApp = ""
                                expandedApp = false
                            }
                        )
                        
                        uiState.availableApps.forEach { app ->
                            DropdownMenuItem(
                                text = { Text(app.app_name) },
                                onClick = {
                                    selectedApp = app.app_name
                                    expandedApp = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pattern Filter
                Text(
                    text = "Filter by Pattern",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = patternText,
                    onValueChange = { patternText = it },
                    placeholder = { Text("Enter pattern to search in content") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onClearFilters()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                    
                    Button(
                        onClick = {
                            onApplyAppFilter(selectedApp)
                            onApplyPatternFilter(patternText)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
} 