package com.zerodev.smsforwarder.ui.screen.rules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.zerodev.smsforwarder.domain.model.Rule
import kotlinx.datetime.Clock

/**
 * Dialog for adding a new SMS forwarding rule.
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
    
    val isValid = name.isNotBlank() && pattern.isNotBlank() && endpoint.isNotBlank()
    
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
                    label = { Text("SMS Pattern") },
                    placeholder = { Text("e.g., 'OTP' or '[0-9]{6}' for regex") },
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