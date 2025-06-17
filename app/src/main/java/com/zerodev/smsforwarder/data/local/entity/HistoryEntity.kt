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
 * @property ruleId Foreign key reference to the rule that triggered this forwarding
 * @property smsBody The SMS body content that was forwarded
 * @property smsFrom The sender of the SMS
 * @property smsTimestamp When the SMS was received
 * @property requestPayload The JSON payload sent to the API
 * @property responseCode HTTP response code (null if request failed)
 * @property responseBody HTTP response body (null if request failed)
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
        Index(value = ["createdAt"])
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleId: Long,
    val smsBody: String,
    val smsFrom: String,
    val smsTimestamp: Instant,
    val requestPayload: String,
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
    RETRYING
} 