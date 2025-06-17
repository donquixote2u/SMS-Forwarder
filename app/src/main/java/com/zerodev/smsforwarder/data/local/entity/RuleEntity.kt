package com.zerodev.smsforwarder.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Database entity representing an SMS forwarding rule.
 * 
 * @property id Unique identifier for the rule
 * @property name Human-readable name for the rule
 * @property pattern Regex or substring pattern to match against SMS body
 * @property isRegex Whether the pattern should be treated as regex (true) or substring (false)
 * @property endpoint HTTP API endpoint URL
 * @property method HTTP method (default: POST)
 * @property headers JSON string of custom headers as key-value pairs
 * @property isActive Whether the rule is currently active
 * @property createdAt Timestamp when the rule was created
 * @property updatedAt Timestamp when the rule was last updated
 */
@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val pattern: String,
    val isRegex: Boolean = false,
    val endpoint: String,
    val method: String = "POST",
    val headers: String = "{}",
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
) 