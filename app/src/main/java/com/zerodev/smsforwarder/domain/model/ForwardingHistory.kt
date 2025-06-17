package com.zerodev.smsforwarder.domain.model

import com.zerodev.smsforwarder.data.local.entity.ForwardingStatus
import kotlinx.datetime.Instant

/**
 * Enum for status colors in the UI.
 */
enum class StatusColor {
    SUCCESS, ERROR, WARNING, INFO
}

/**
 * Domain model representing an SMS forwarding attempt history log.
 * This is the business logic representation, separate from database entities.
 * 
 * @property id Unique identifier for the history entry
 * @property ruleId Reference to the rule that triggered this forwarding (null if no rule matched)
 * @property ruleName Name of the rule for display purposes
 * @property smsMessage The SMS message that was received/forwarded
 * @property matchedRule Whether this SMS matched any forwarding rule
 * @property requestPayload The JSON payload sent to the API (null if no rule matched)
 * @property responseCode HTTP response code (null if request failed or no rule matched)
 * @property responseBody HTTP response body (null if request failed or no rule matched)
 * @property status Success or failure status
 * @property errorMessage Error details if forwarding failed
 * @property createdAt Timestamp when this history entry was created
 */
data class ForwardingHistory(
    val id: Long = 0,
    val ruleId: Long? = null, // Nullable to support non-matching SMS
    val ruleName: String,
    val smsMessage: SmsMessage,
    val matchedRule: Boolean = false, // Whether SMS matched any rule
    val requestPayload: String? = null, // Nullable for non-matching SMS
    val responseCode: Int? = null,
    val responseBody: String? = null,
    val status: ForwardingStatus,
    val errorMessage: String? = null,
    val createdAt: Instant
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
        return status == ForwardingStatus.PENDING || status == ForwardingStatus.RETRYING
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
            ForwardingStatus.PENDING -> "Pending"
            ForwardingStatus.RETRYING -> "Retrying"
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
            ForwardingStatus.PENDING -> StatusColor.INFO
            ForwardingStatus.RETRYING -> StatusColor.WARNING
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
}

 