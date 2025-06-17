package com.zerodev.smsforwarder.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model representing an incoming SMS message.
 * 
 * @property body The SMS message content
 * @property sender The phone number or address that sent the SMS
 * @property timestamp When the SMS was received
 */
data class SmsMessage(
    val body: String,
    val sender: String,
    val timestamp: Instant
) {
    /**
     * Convert the SMS message to a JSON payload for API forwarding.
     * 
     * @return Map representing the SMS data as key-value pairs
     */
    fun toApiPayload(): Map<String, Any> {
        return mapOf(
            "sms_body" to body,
            "sms_sender" to sender,
            "sms_timestamp" to timestamp.epochSeconds,
            "sms_timestamp_iso" to timestamp.toString(),
            "received_at" to Instant.fromEpochSeconds(System.currentTimeMillis() / 1000).toString()
        )
    }
    
    /**
     * Check if this SMS message is valid for processing.
     * 
     * @return true if the SMS has valid content, false otherwise
     */
    fun isValid(): Boolean {
        return body.isNotBlank() && sender.isNotBlank()
    }
    
    /**
     * Get a safe representation of the sender for logging/display.
     * Masks sensitive information while keeping the format recognizable.
     * 
     * @return Masked sender string
     */
    fun getMaskedSender(): String {
        return if (sender.length > 4) {
            "${sender.take(2)}***${sender.takeLast(2)}"
        } else {
            "***"
        }
    }
    
    /**
     * Get a truncated version of the SMS body for display purposes.
     * 
     * @param maxLength Maximum length of the truncated body
     * @return Truncated SMS body with ellipsis if needed
     */
    fun getTruncatedBody(maxLength: Int): String {
        return if (body.length <= maxLength) {
            body
        } else {
            "${body.take(maxLength)}..."
        }
    }
} 