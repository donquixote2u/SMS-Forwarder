package com.zerodev.smsforwarder.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model representing a forwarding rule for SMS or notifications.
 * This is the business logic representation, separate from database entities.
 * 
 * @property id Unique identifier for the rule
 * @property name Human-readable name for the rule
 * @property pattern Regex or substring pattern to match against content
 * @property source Source type - SMS or NOTIFICATION
 * @property packageFilter Package name filter for notifications (required if source == NOTIFICATION, null means all packages)
 * @property isRegex Whether the pattern should be treated as regex (true) or substring (false)
 * @property endpoint HTTP API endpoint URL
 * @property method HTTP method (default: POST)
 * @property headers Map of custom headers as key-value pairs
 * @property isActive Whether the rule is currently active
 * @property createdAt Timestamp when the rule was created
 * @property updatedAt Timestamp when the rule was last updated
 */
data class Rule(
    val id: Long = 0,
    val name: String,
    val pattern: String,
    val source: SourceType,
    val packageFilter: String? = null,
    val isRegex: Boolean = false,
    val endpoint: String,
    val method: String = "POST",
    val headers: Map<String, String> = emptyMap(),
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /**
     * Check if the given content matches this rule's pattern.
     * For SMS, content is the SMS body.
     * For notifications, content is typically title + text combined.
     * 
     * @param content The content to match against
     * @return true if the content matches the pattern, false otherwise
     */
    fun matches(content: String): Boolean {
        return try {
            if (isRegex) {
                pattern.toRegex(RegexOption.IGNORE_CASE).containsMatchIn(content)
            } else {
                content.contains(pattern, ignoreCase = true)
            }
        } catch (e: Exception) {
            // If regex is invalid, fall back to substring matching
            content.contains(pattern, ignoreCase = true)
        }
    }
    
    /**
     * Check if this rule applies to the given package name.
     * For SMS rules, this always returns true.
     * For notification rules, checks against packageFilter.
     * 
     * @param packageName The package name to check
     * @return true if the rule applies to this package, false otherwise
     */
    fun appliesToPackage(packageName: String): Boolean {
        return when (source) {
            SourceType.SMS -> true
            SourceType.NOTIFICATION -> packageFilter == null || packageFilter == packageName
        }
    }
    
    /**
     * Validate the rule configuration.
     * 
     * @return ValidationResult indicating if the rule is valid
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (name.isBlank()) {
            errors.add("Rule name cannot be empty")
        }
        
        if (pattern.isBlank()) {
            errors.add("Pattern cannot be empty")
        }
        
        if (endpoint.isBlank()) {
            errors.add("Endpoint URL cannot be empty")
        } else if (!isValidUrl(endpoint)) {
            errors.add("Endpoint must be a valid HTTP/HTTPS URL")
        }
        
        if (method.isBlank()) {
            errors.add("HTTP method cannot be empty")
        }
        
        // Package filter validation for notification rules
        if (source == SourceType.NOTIFICATION && packageFilter != null && packageFilter.isBlank()) {
            errors.add("Package filter cannot be empty (use null to match all packages)")
        }
        
        // Test regex pattern if it's marked as regex
        if (isRegex && pattern.isNotBlank()) {
            try {
                pattern.toRegex()
            } catch (e: Exception) {
                errors.add("Invalid regex pattern: ${e.message}")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }
    
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://", ignoreCase = true) || 
               url.startsWith("https://", ignoreCase = true)
    }
}

/**
 * Result of rule validation.
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val errors: List<String>) : ValidationResult()
} 