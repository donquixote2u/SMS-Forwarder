package com.zerodev.smsforwarder.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model representing an SMS forwarding rule.
 * This is the business logic representation, separate from database entities.
 * 
 * @property id Unique identifier for the rule
 * @property name Human-readable name for the rule
 * @property pattern Regex or substring pattern to match against SMS body
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
    val isRegex: Boolean = false,
    val endpoint: String,
    val method: String = "POST",
    val headers: Map<String, String> = emptyMap(),
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    /**
     * Check if the given SMS body matches this rule's pattern.
     * 
     * @param smsBody The SMS body content to match against
     * @return true if the SMS body matches the pattern, false otherwise
     */
    fun matches(smsBody: String): Boolean {
        return try {
            if (isRegex) {
                pattern.toRegex(RegexOption.IGNORE_CASE).containsMatchIn(smsBody)
            } else {
                smsBody.contains(pattern, ignoreCase = true)
            }
        } catch (e: Exception) {
            // If regex is invalid, fall back to substring matching
            smsBody.contains(pattern, ignoreCase = true)
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