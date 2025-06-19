package com.zerodev.smsforwarder.data.local.dao

import androidx.room.*
import com.zerodev.smsforwarder.data.local.entity.HistoryEntity
import com.zerodev.smsforwarder.domain.model.ForwardingStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data class for status count results.
 */
data class StatusCount(
    val status: String,
    val count: Int
)

/**
 * Data Access Object for forwarding history (SMS and notifications).
 * Provides database operations for managing forwarding attempt logs.
 */
@Dao
interface HistoryDao {
    
    /**
     * Get all history entries for display in the history screen.
     * @return Flow of all history entries ordered by timestamp (newest first)
     */
    @Query("SELECT * FROM forwarding_history ORDER BY timestamp DESC LIMIT 1000")
    fun getAllHistory(): Flow<List<HistoryEntity>>
    
    /**
     * Get history for a specific rule.
     * @param ruleId The rule ID
     * @return Flow of history entries for the rule
     */
    @Query("SELECT * FROM forwarding_history WHERE rule_id = :ruleId ORDER BY timestamp DESC")
    fun getHistoryByRule(ruleId: Long): Flow<List<HistoryEntity>>
    
    /**
     * Get history entries by source type.
     * @param sourceType The source type ("SMS" or "NOTIFICATION")
     * @return Flow of history entries for the specified source type
     */
    @Query("SELECT * FROM forwarding_history WHERE source_type = :sourceType ORDER BY timestamp DESC LIMIT 1000")
    fun getHistoryBySourceType(sourceType: String): Flow<List<HistoryEntity>>
    
    /**
     * Get history entries for a specific package (notifications only).
     * @param packageName The package name
     * @return Flow of history entries for the specified package
     */
    @Query("SELECT * FROM forwarding_history WHERE source_package = :packageName ORDER BY timestamp DESC LIMIT 1000")
    fun getHistoryByPackage(packageName: String): Flow<List<HistoryEntity>>
    
    /**
     * Get a specific history entry by ID.
     * @param id The history ID
     * @return The history entry if found, null otherwise
     */
    @Query("SELECT * FROM forwarding_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): HistoryEntity?
    
    /**
     * Insert a new history entry.
     * @param history The history entry to insert
     * @return The ID of the inserted history entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long
    
    /**
     * Update an existing history entry.
     * @param history The history entry with updated values
     */
    @Update
    suspend fun updateHistory(history: HistoryEntity)
    
    /**
     * Get history entries by status.
     * @param status The forwarding status to filter by
     * @return Flow of history entries with the specified status
     */
    @Query("SELECT * FROM forwarding_history WHERE status = :status ORDER BY timestamp DESC")
    fun getHistoryByStatus(status: String): Flow<List<HistoryEntity>>
    
    /**
     * Delete old history entries to keep database size manageable.
     * Keeps only the most recent 1000 entries.
     */
    @Query("DELETE FROM forwarding_history WHERE id NOT IN (SELECT id FROM forwarding_history ORDER BY timestamp DESC LIMIT 1000)")
    suspend fun cleanupOldHistory()
    
    /**
     * Delete all history entries for a specific rule.
     * @param ruleId The rule ID
     */
    @Query("DELETE FROM forwarding_history WHERE rule_id = :ruleId")
    suspend fun deleteHistoryByRule(ruleId: Long)
    
    /**
     * Delete all history entries.
     */
    @Query("DELETE FROM forwarding_history")
    suspend fun deleteAllHistory()
    
    /**
     * Get count of history entries by status.
     * @param status The forwarding status
     * @return Count of entries with the specified status
     */
    @Query("SELECT COUNT(*) FROM forwarding_history WHERE status = :status")
    suspend fun getHistoryCountByStatus(status: String): Int
    
    /**
     * Get statistics about forwarding history.
     * @return List of status counts
     */
    @Query("SELECT status, COUNT(*) as count FROM forwarding_history GROUP BY status")
    suspend fun getHistoryStatistics(): List<StatusCount>
    
    /**
     * Get recent history entries (last 24 hours).
     * @param since Timestamp to filter from (in milliseconds)
     * @return Flow of recent history entries
     */
    @Query("SELECT * FROM forwarding_history WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getRecentHistory(since: Long): Flow<List<HistoryEntity>>
    
    /**
     * Get successful forwarding attempts count.
     * @return Count of successful attempts
     */
    @Query("SELECT COUNT(*) FROM forwarding_history WHERE status = 'SUCCESS'")
    suspend fun getSuccessfulCount(): Int
    
    /**
     * Get failed forwarding attempts count.
     * @return Count of failed attempts
     */
    @Query("SELECT COUNT(*) FROM forwarding_history WHERE status = 'FAILED'")
    suspend fun getFailedCount(): Int
    
    /**
     * Get matched messages count (messages that matched at least one rule).
     * @return Count of matched messages
     */
    @Query("SELECT COUNT(*) FROM forwarding_history WHERE matched_rule = 1")
    suspend fun getMatchedCount(): Int
    
    /**
     * Get total message count.
     * @return Total count of all messages/notifications processed
     */
    @Query("SELECT COUNT(*) FROM forwarding_history")
    suspend fun getTotalCount(): Int
    
    /**
     * Get history entries with pagination.
     * @param limit Number of items per page
     * @param offset Offset for pagination
     * @return Flow of paginated history entries
     */
    @Query("SELECT * FROM forwarding_history ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    fun getHistoryPaginated(limit: Int, offset: Int): Flow<List<HistoryEntity>>
    
    /**
     * Delete a specific history entry by ID.
     * @param id The history entry ID to delete
     */
    @Query("DELETE FROM forwarding_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)
} 