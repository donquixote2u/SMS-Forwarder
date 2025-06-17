package com.zerodev.smsforwarder.ui.screen.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.zerodev.smsforwarder.domain.model.ForwardingHistory
import com.zerodev.smsforwarder.domain.model.StatusColor
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Screen for viewing SMS forwarding history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedHistoryEntry by remember { mutableStateOf<ForwardingHistory?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forwarding History") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
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
                        StatisticItem("Total SMS", uiState.totalCount.toString())
                        StatisticItem("Matched", uiState.matchedCount.toString())
                        StatisticItem("Success", uiState.successCount.toString())
                        StatisticItem("Failed", uiState.failedCount.toString())
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // History List
            if (uiState.isLoading) {
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
                                text = "No SMS Messages Yet",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Received SMS messages will appear here for debugging.\nSend an SMS to your phone to test.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.history) { historyEntry ->
                        HistoryItem(
                            historyEntry = historyEntry,
                            onClick = { selectedHistoryEntry = historyEntry }
                        )
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
private fun HistoryItem(
    historyEntry: ForwardingHistory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (historyEntry.matchedRule) "Rule: ${historyEntry.ruleName}" else "No Rule Matched",
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
            
            Text(
                text = "SMS: \"${historyEntry.smsMessage.getTruncatedBody(60)}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "From: ${historyEntry.smsMessage.getMaskedSender()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
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
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = historyEntry.createdAt.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
                            text = "SMS Details",
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
                
                // SMS Information
                DetailSection(title = "SMS Information") {
                    DetailRow("From", historyEntry.smsMessage.sender)
                    DetailRow("Content", historyEntry.smsMessage.body)
                    DetailRow(
                        "Received", 
                        historyEntry.smsMessage.timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
                    )
                }
                
                // Rule Information
                if (historyEntry.matchedRule) {
                    DetailSection(title = "Rule Information") {
                        DetailRow("Rule Name", historyEntry.ruleName)
                        if (historyEntry.ruleId != null) {
                            DetailRow("Rule ID", historyEntry.ruleId.toString())
                        }
                    }
                    
                    // Request Information
                    if (historyEntry.requestPayload != null) {
                        DetailSection(title = "Request Information") {
                            DetailRow("Payload", historyEntry.requestPayload, isCode = true)
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
                        historyEntry.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).toString()
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
    val error: String? = null
) 