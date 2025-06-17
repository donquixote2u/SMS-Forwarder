package com.zerodev.smsforwarder.ui.screen.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zerodev.smsforwarder.domain.model.Rule

/**
 * Screen for managing SMS forwarding rules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    viewModel: RulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Forwarding Rules") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Active Rules: ${uiState.rules.count { it.isActive }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Total Rules: ${uiState.rules.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Rules List
            if (uiState.rules.isEmpty()) {
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
                                text = "No Rules Yet",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap the + button to create your first SMS forwarding rule",
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
                    items(uiState.rules) { rule ->
                        RuleItem(
                            rule = rule,
                            onToggleActive = { viewModel.toggleRuleStatus(rule.id, !rule.isActive) },
                            onDelete = { viewModel.deleteRule(rule.id) }
                        )
                    }
                }
            }
        }
    }
    
    // Add Rule Dialog
    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { rule ->
                viewModel.createRule(rule)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun RuleItem(
    rule: Rule,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = rule.isActive,
                    onCheckedChange = { onToggleActive() }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Pattern: ${rule.pattern}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Endpoint: ${rule.endpoint}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Method: ${rule.method}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (rule.headers.isNotEmpty()) {
                Text(
                    text = "Headers: ${rule.headers.size} configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

/**
 * UI state for the rules screen.
 */
data class RulesUiState(
    val rules: List<Rule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) 