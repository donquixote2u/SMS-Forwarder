package com.zerodev.smsforwarder.data.local.dao

import androidx.room.*
import com.zerodev.smsforwarder.data.local.entity.HistoryEntity
import com.zerodev.smsforwarder.data.local.entity.ForwardingStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SMS forwarding history.
 * Provides database operations for managing forwarding attempt logs.
 */
@Dao
interface HistoryDao {
    
    /**
     * Get all history entries for display in the history screen.
     * @return Flow of all history entries ordered by creation time (newest first)
     */
    @Query("SELECT * FROM history ORDER BY createdAt DESC LIMIT 1000")
    fun getAllHistory(): Flow<List<HistoryEntity>>
    
    /**
     * Get history for a specific rule.
     * @param ruleId The rule ID
     * @return Flow of history entries for the rule
     */
    @Query("SELECT * FROM history WHERE ruleId = :ruleId ORDER BY createdAt DESC")
    fun getHistoryByRule(ruleId: Long): Flow<List<HistoryEntity>>
    
    /**
     * Get a specific history entry by ID.
     * @param id The history ID
     * @return The history entry if found, null otherwise
     */
    @Query("SELECT * FROM history WHERE id = :id")
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
    @Query("SELECT * FROM history WHERE status = :status ORDER BY createdAt DESC")
    fun getHistoryByStatus(status: ForwardingStatus): Flow<List<HistoryEntity>>
    
    /**
     * Delete old history entries to keep database size manageable.
     * Keeps only the most recent 1000 entries.
     */
    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY createdAt DESC LIMIT 1000)")
    suspend fun cleanupOldHistory()
    
    /**
     * Delete all history entries for a specific rule.
     * @param ruleId The rule ID
     */
    @Query("DELETE FROM history WHERE ruleId = :ruleId")
    suspend fun deleteHistoryByRule(ruleId: Long)
    
    /**
     * Delete all history entries.
     */
    @Query("DELETE FROM history")
    suspend fun deleteAllHistory()
    
    /**
     * Get count of history entries by status.
     * @param status The forwarding status
     * @return Count of entries with the specified status
     */
    @Query("SELECT COUNT(*) FROM history WHERE status = :status")
    suspend fun getHistoryCountByStatus(status: ForwardingStatus): Int
    
    /**
     * Get recent history entries (last 24 hours).
     * @param since Timestamp to filter from
     * @return Flow of recent history entries
     */
    @Query("SELECT * FROM history WHERE createdAt >= :since ORDER BY createdAt DESC")
    fun getRecentHistory(since: Long): Flow<List<HistoryEntity>>
} 