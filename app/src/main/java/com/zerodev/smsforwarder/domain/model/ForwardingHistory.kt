package com.zerodev.smsforwarder.domain.model

import kotlinx.datetime.Instant

/**
 * Enum for status colors in the UI.
 */
enum class StatusColor {
    SUCCESS, ERROR, WARNING, INFO
}

/**
 * Domain model representing a forwarding attempt history log for SMS or notifications.
 * This is the business logic representation, separate from database entities.
 * 
 * @property id Unique identifier for the history entry
 * @property ruleId Reference to the rule that triggered this forwarding (null if no rule matched)
 * @property matchedRule Whether this message/notification matched any forwarding rule
 * @property senderNumber The sender phone number (for SMS only, null for notifications)
 * @property messageBody The message content (SMS body or notification text)
 * @property sourceType Source type - "SMS" or "NOTIFICATION"
 * @property sourcePackage Package name (for notifications only, null for SMS)
 * @property sourceAppName App name (for notifications only, null for SMS)
 * @property notificationTitle Notification title (for notifications only, null for SMS)
 * @property notificationText Notification text (for notifications only, null for SMS)
 * @property endpoint The HTTP endpoint used for forwarding
 * @property method The HTTP method used for forwarding
 * @property requestHeaders Map of request headers
 * @property requestBody The request body sent to the API
 * @property responseCode HTTP response code (null if request failed or no rule matched)
 * @property responseBody HTTP response body (null if request failed or no rule matched)
 * @property status Success or failure status
 * @property errorMessage Error details if forwarding failed
 * @property timestamp Timestamp when the message/notification was received
 * @property forwardedAt Timestamp when the forwarding was attempted (null if no forwarding)
 */
data class ForwardingHistory(
    val id: Long = 0,
    val ruleId: Long? = null, // Nullable to support non-matching messages
    val matchedRule: Boolean = false, // Whether message matched any rule
    
    // SMS-specific fields
    val senderNumber: String? = null, // Phone number for SMS, null for notifications
    val messageBody: String, // SMS body or notification content
    
    // Source information
    val sourceType: String, // "SMS" or "NOTIFICATION"
    val sourcePackage: String? = null, // Package name for notifications
    val sourceAppName: String? = null, // App name for notifications
    
    // Notification-specific fields
    val notificationTitle: String? = null, // Title for notifications
    val notificationText: String? = null, // Text for notifications
    
    // Forwarding details
    val endpoint: String? = null,
    val method: String? = null,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val responseCode: Int? = null,
    val responseBody: String? = null,
    val status: ForwardingStatus,
    val errorMessage: String? = null,
    val timestamp: Instant, // When message/notification was received
    val forwardedAt: Instant? = null // When forwarding was attempted
) {
    /**
     * Check if this forwarding attempt was successful.
     * 
     * @return true if the forwarding was successful, false otherwise
     */
    fun isSuccessful(): Boolean {
        return status == ForwardingStatus.SUCCESS && responseCode in 200..299
    }
    
    /**
     * Check if this forwarding attempt failed.
     * 
     * @return true if the forwarding failed, false otherwise
     */
    fun isFailed(): Boolean {
        return status == ForwardingStatus.FAILED
    }
    
    /**
     * Check if this forwarding attempt is still pending or retrying.
     * 
     * @return true if the forwarding is in progress, false otherwise
     */
    fun isPending(): Boolean {
        return status == ForwardingStatus.RETRY
    }
    
    /**
     * Get a human-readable status description.
     * 
     * @return Status description string
     */
    fun getStatusDescription(): String {
        return when (status) {
            ForwardingStatus.SUCCESS -> "Success"
            ForwardingStatus.FAILED -> "Failed"
            ForwardingStatus.RETRY -> "Retrying"
            ForwardingStatus.NO_RULE_MATCHED -> "No Rule Matched"
            ForwardingStatus.RECEIVED -> "Received"
        }
    }
    
    /**
     * Get a color-coded status for UI display.
     * 
     * @return Status color identifier
     */
    fun getStatusColor(): StatusColor {
        return when (status) {
            ForwardingStatus.SUCCESS -> StatusColor.SUCCESS
            ForwardingStatus.FAILED -> StatusColor.ERROR
            ForwardingStatus.RETRY -> StatusColor.WARNING
            ForwardingStatus.NO_RULE_MATCHED -> StatusColor.WARNING
            ForwardingStatus.RECEIVED -> StatusColor.INFO
        }
    }
    
    /**
     * Get the HTTP status description if available.
     * 
     * @return HTTP status description or null if not available
     */
    fun getHttpStatusDescription(): String? {
        return responseCode?.let { code ->
            when (code) {
                in 200..299 -> "Success"
                in 300..399 -> "Redirect"
                in 400..499 -> "Client Error"
                in 500..599 -> "Server Error"
                else -> "Unknown"
            }
        }
    }
    
    /**
     * Get a summary of this forwarding attempt for display.
     * 
     * @return Summary string
     */
    fun getSummary(): String {
        val statusDesc = getStatusDescription()
        val httpDesc = getHttpStatusDescription()
        val codeDesc = responseCode?.let { " ($it)" } ?: ""
        
        return if (httpDesc != null) {
            "$statusDesc - $httpDesc$codeDesc"
        } else {
            statusDesc + (errorMessage?.let { " - $it" } ?: "")
        }
    }
    
    /**
     * Get the source display name for UI.
     * 
     * @return Source display name (app name for notifications, phone number for SMS)
     */
    fun getSourceDisplayName(): String {
        return when (sourceType) {
            "NOTIFICATION" -> sourceAppName ?: sourcePackage ?: "Unknown App"
            "SMS" -> senderNumber ?: "Unknown Number"
            else -> "Unknown Source"
        }
    }
    
    /**
     * Get a preview of the content for UI display.
     * 
     * @param maxLength Maximum length of the preview
     * @return Content preview string
     */
    fun getContentPreview(maxLength: Int = 100): String {
        val content = when (sourceType) {
            "NOTIFICATION" -> {
                val title = notificationTitle?.takeIf { it.isNotBlank() } ?: ""
                val text = notificationText?.takeIf { it.isNotBlank() } ?: messageBody
                if (title.isNotEmpty() && text.isNotEmpty()) {
                    "$title: $text"
                } else {
                    title.ifEmpty { text }
                }
            }
            "SMS" -> messageBody
            else -> messageBody
        }
        
        return if (content.length <= maxLength) {
            content
        } else {
            content.take(maxLength - 3) + "..."
        }
    }
    
    /**
     * Check if this is an SMS forwarding entry.
     * 
     * @return true if this is SMS, false otherwise
     */
    fun isSms(): Boolean = sourceType == "SMS"
    
    /**
     * Check if this is a notification forwarding entry.
     * 
     * @return true if this is notification, false otherwise
     */
    fun isNotification(): Boolean = sourceType == "NOTIFICATION"
}

 