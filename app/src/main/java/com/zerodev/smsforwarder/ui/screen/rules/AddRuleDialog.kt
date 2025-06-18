package com.zerodev.smsforwarder.ui.screen.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.zerodev.smsforwarder.domain.model.Rule
import com.zerodev.smsforwarder.domain.model.SourceType
import kotlinx.datetime.Clock

/**
 * Dialog for adding a new SMS or notification forwarding rule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onSave: (Rule) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    var endpoint by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("POST") }
    var headers by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(SourceType.SMS) }
    var packageFilter by remember { mutableStateOf("") }
    var usePackageFilter by remember { mutableStateOf(false) }
    
    val isValid = name.isNotBlank() && pattern.isNotBlank() && endpoint.isNotBlank() &&
            (sourceType == SourceType.SMS || !usePackageFilter || packageFilter.isNotBlank())
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add New Rule",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Source Type Selection
                Text(
                    text = "Source Type",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        onClick = { sourceType = SourceType.SMS },
                        label = { Text("SMS") },
                        selected = sourceType == SourceType.SMS,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        onClick = { sourceType = SourceType.NOTIFICATION },
                        label = { Text("Notifications") },
                        selected = sourceType == SourceType.NOTIFICATION
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Rule Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pattern
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { 
                        Text(
                            if (sourceType == SourceType.SMS) "SMS Pattern" 
                            else "Notification Pattern"
                        ) 
                    },
                    placeholder = { 
                        Text(
                            if (sourceType == SourceType.SMS) "e.g., 'OTP' or '[0-9]{6}' for regex"
                            else "e.g., 'new message' or 'WhatsApp.*' for regex"
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Regex Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isRegex,
                            onClick = { isRegex = !isRegex },
                            role = Role.Checkbox
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRegex,
                        onCheckedChange = { isRegex = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use as Regular Expression")
                }
                
                // Package Filter for Notifications
                if (sourceType == SourceType.NOTIFICATION) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = usePackageFilter,
                                onClick = { usePackageFilter = !usePackageFilter },
                                role = Role.Checkbox
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = usePackageFilter,
                            onCheckedChange = { usePackageFilter = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Filter by specific app")
                    }
                    
                    if (usePackageFilter) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // App Selection Button instead of text input
                        var showAppPicker by remember { mutableStateOf(false) }
                        var selectedAppName by remember { mutableStateOf("") }
                        
                        OutlinedButton(
                            onClick = { showAppPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedAppName.isNotEmpty()) {
                                        selectedAppName
                                    } else {
                                        "Select App"
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search apps"
                                )
                            }
                        }
                        
                        if (packageFilter.isNotEmpty()) {
                            Text(
                                text = "Package: $packageFilter",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        Text(
                            text = "Leave unchecked to match notifications from all apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        // App Picker Dialog
                        if (showAppPicker) {
                            AppPickerDialog(
                                onDismiss = { showAppPicker = false },
                                onAppSelected = { selectedApp ->
                                    packageFilter = selectedApp.packageName
                                    selectedAppName = selectedApp.appName
                                    showAppPicker = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Endpoint URL
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("API Endpoint") },
                    placeholder = { Text("https://api.example.com/webhook") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // HTTP Method
                Text(
                    text = "HTTP Method",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val methods = listOf("POST", "GET", "PUT", "PATCH")
                methods.forEach { httpMethod ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (method == httpMethod),
                                onClick = { method = httpMethod },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (method == httpMethod),
                            onClick = { method = httpMethod }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(httpMethod)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Custom Headers
                OutlinedTextField(
                    value = headers,
                    onValueChange = { headers = it },
                    label = { Text("Custom Headers (JSON)") },
                    placeholder = { Text("{\"Authorization\": \"Bearer token\"}") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val headersMap = if (headers.isNotBlank()) {
                                try {
                                    // Simple parsing - in production you'd use proper JSON parsing
                                    mapOf("Content-Type" to "application/json")
                                } catch (e: Exception) {
                                    emptyMap()
                                }
                            } else {
                                emptyMap()
                            }
                            
                            val now = Clock.System.now()
                            val rule = Rule(
                                name = name.trim(),
                                pattern = pattern.trim(),
                                isRegex = isRegex,
                                endpoint = endpoint.trim(),
                                method = method,
                                headers = headersMap,
                                isActive = true,
                                source = sourceType,
                                packageFilter = if (sourceType == SourceType.NOTIFICATION && usePackageFilter) {
                                    packageFilter.trim().takeIf { it.isNotBlank() }
                                } else null,
                                createdAt = now,
                                updatedAt = now
                            )
                            
                            onSave(rule)
                        },
                        enabled = isValid
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
} 