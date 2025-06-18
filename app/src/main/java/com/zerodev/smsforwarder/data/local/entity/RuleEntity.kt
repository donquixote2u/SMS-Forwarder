package com.zerodev.smsforwarder.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

/**
 * Database entity representing a forwarding rule for SMS or notifications.
 * 
 * @property id Unique identifier for the rule
 * @property name Human-readable name for the rule
 * @property pattern Regex or substring pattern to match against content
 * @property source Source type - "SMS" or "NOTIFICATION"
 * @property packageFilter Package name filter for notifications (null means all packages)
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
    
    @ColumnInfo(name = "source")
    val source: String, // "SMS" or "NOTIFICATION"
    
    @ColumnInfo(name = "package_filter")
    val packageFilter: String? = null,
    
    @ColumnInfo(name = "is_regex")
    val isRegex: Boolean = false,
    
    val endpoint: String,
    val method: String = "POST",
    val headers: String = "{}",
    
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
) 