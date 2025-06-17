package com.zerodev.smsforwarder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.datetime.Instant

/**
 * Database entity representing an SMS forwarding attempt history log.
 * 
 * @property id Unique identifier for the history entry
 * @property ruleId Foreign key reference to the rule that triggered this forwarding (null if no rule matched)
 * @property smsBody The SMS body content that was received
 * @property smsFrom The sender of the SMS
 * @property smsTimestamp When the SMS was received
 * @property matchedRule Whether this SMS matched any forwarding rule
 * @property requestPayload The JSON payload sent to the API (null if no rule matched)
 * @property responseCode HTTP response code (null if request failed or no rule matched)
 * @property responseBody HTTP response body (null if request failed or no rule matched)
 * @property status Success or failure status
 * @property errorMessage Error details if forwarding failed
 * @property createdAt Timestamp when this history entry was created
 */
@Entity(
    tableName = "history",
    foreignKeys = [
        ForeignKey(
            entity = RuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ruleId"]),
        Index(value = ["createdAt"]),
        Index(value = ["matchedRule"])
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleId: Long? = null, // Nullable to support non-matching SMS
    val smsBody: String,
    val smsFrom: String,
    val smsTimestamp: Instant,
    val matchedRule: Boolean = false, // Whether SMS matched any rule
    val requestPayload: String? = null, // Nullable for non-matching SMS
    val responseCode: Int? = null,
    val responseBody: String? = null,
    val status: ForwardingStatus,
    val errorMessage: String? = null,
    val createdAt: Instant
)

/**
 * Enum representing the status of an SMS forwarding attempt.
 */
enum class ForwardingStatus {
    SUCCESS,
    FAILED,
    PENDING,
    RETRYING,
    NO_RULE_MATCHED, // New status for SMS that don't match any rule
    RECEIVED // Status for SMS that were received but not processed yet
} 