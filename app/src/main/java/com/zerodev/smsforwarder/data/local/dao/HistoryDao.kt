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
     * Get total matched messages count with current filters.
     * @param searchQuery Optional search query (use empty string if not filtering)
     * @param appFilter Optional app filter (use empty string if not filtering)  
     * @param patternFilter Optional pattern filter (use empty string if not filtering)
     * @return Count of matched messages with filters applied
     */
    @Query("""
        SELECT COUNT(*) FROM forwarding_history 
        WHERE matched_rule = 1 AND
              (:searchQuery = '' OR 
               source_app_name LIKE '%' || :searchQuery || '%' OR
               source_package LIKE '%' || :searchQuery || '%' OR
               notification_title LIKE '%' || :searchQuery || '%' OR
               message_body LIKE '%' || :searchQuery || '%' OR
               sender_number LIKE '%' || :searchQuery || '%') AND
              (:appFilter = '' OR 
               source_package = :appFilter OR source_app_name = :appFilter) AND
              (:patternFilter = '' OR
               message_body LIKE '%' || :patternFilter || '%' OR
               notification_title LIKE '%' || :patternFilter || '%' OR
               notification_text LIKE '%' || :patternFilter || '%')
    """)
    suspend fun getFilteredMatchedCount(
        searchQuery: String,
        appFilter: String, 
        patternFilter: String
    ): Int
    
    /**
     * Get total count with current filters.
     * @param searchQuery Optional search query (use empty string if not filtering)
     * @param appFilter Optional app filter (use empty string if not filtering)  
     * @param patternFilter Optional pattern filter (use empty string if not filtering)
     * @return Total count of messages with filters applied
     */
    @Query("""
        SELECT COUNT(*) FROM forwarding_history 
        WHERE (:searchQuery = '' OR 
               source_app_name LIKE '%' || :searchQuery || '%' OR
               source_package LIKE '%' || :searchQuery || '%' OR
               notification_title LIKE '%' || :searchQuery || '%' OR
               message_body LIKE '%' || :searchQuery || '%' OR
               sender_number LIKE '%' || :searchQuery || '%') AND
              (:appFilter = '' OR 
               source_package = :appFilter OR source_app_name = :appFilter) AND
              (:patternFilter = '' OR
               message_body LIKE '%' || :patternFilter || '%' OR
               notification_title LIKE '%' || :patternFilter || '%' OR
               notification_text LIKE '%' || :patternFilter || '%')
    """)
    suspend fun getFilteredTotalCount(
        searchQuery: String,
        appFilter: String, 
        patternFilter: String
    ): Int
    
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
    
    /**
     * Search history entries with fuzzy search (combines app, title, content).
     * @param searchQuery The search query
     * @param limit Number of items per page
     * @param offset Offset for pagination
     * @return Flow of matching history entries
     */
    @Query("""
        SELECT * FROM forwarding_history 
        WHERE (source_app_name LIKE '%' || :searchQuery || '%' OR
               source_package LIKE '%' || :searchQuery || '%' OR
               notification_title LIKE '%' || :searchQuery || '%' OR
               message_body LIKE '%' || :searchQuery || '%' OR
               sender_number LIKE '%' || :searchQuery || '%')
        ORDER BY timestamp DESC 
        LIMIT :limit OFFSET :offset
    """)
    fun searchHistoryPaginated(searchQuery: String, limit: Int, offset: Int): Flow<List<HistoryEntity>>
    
    /**
     * Filter history entries by app (source package or app name).
     * @param appFilter The app package name or app name to filter by
     * @param limit Number of items per page
     * @param offset Offset for pagination
     * @return Flow of filtered history entries
     */
    @Query("""
        SELECT * FROM forwarding_history 
        WHERE (source_package = :appFilter OR source_app_name = :appFilter)
        ORDER BY timestamp DESC 
        LIMIT :limit OFFSET :offset
    """)
    fun filterByAppPaginated(appFilter: String, limit: Int, offset: Int): Flow<List<HistoryEntity>>
    
    /**
     * Filter history entries by pattern (message content or notification text).
     * @param pattern The pattern to search for in message content
     * @param limit Number of items per page
     * @param offset Offset for pagination
     * @return Flow of filtered history entries
     */
    @Query("""
        SELECT * FROM forwarding_history 
        WHERE (message_body LIKE '%' || :pattern || '%' OR
               notification_title LIKE '%' || :pattern || '%' OR
               notification_text LIKE '%' || :pattern || '%')
        ORDER BY timestamp DESC 
        LIMIT :limit OFFSET :offset
    """)
    fun filterByPatternPaginated(pattern: String, limit: Int, offset: Int): Flow<List<HistoryEntity>>
    
    /**
     * Get distinct apps from history for filter dropdown.
     * @return List of distinct app names and packages
     */
    @Query("""
        SELECT DISTINCT 
            COALESCE(source_app_name, source_package, 'Unknown') as app_name,
            source_package
        FROM forwarding_history 
        WHERE source_package IS NOT NULL OR source_app_name IS NOT NULL
        ORDER BY app_name
    """)
    suspend fun getDistinctApps(): List<AppInfo>
    
    /**
     * Combined filter with search, app filter, and pattern filter.
     * @param searchQuery Optional search query (use empty string if not filtering)
     * @param appFilter Optional app filter (use empty string if not filtering)  
     * @param patternFilter Optional pattern filter (use empty string if not filtering)
     * @param limit Number of items per page
     * @param offset Offset for pagination
     * @return Flow of filtered history entries
     */
    @Query("""
        SELECT * FROM forwarding_history 
        WHERE (:searchQuery = '' OR 
               source_app_name LIKE '%' || :searchQuery || '%' OR
               source_package LIKE '%' || :searchQuery || '%' OR
               notification_title LIKE '%' || :searchQuery || '%' OR
               message_body LIKE '%' || :searchQuery || '%' OR
               sender_number LIKE '%' || :searchQuery || '%') AND
              (:appFilter = '' OR 
               source_package = :appFilter OR source_app_name = :appFilter) AND
              (:patternFilter = '' OR
               message_body LIKE '%' || :patternFilter || '%' OR
               notification_title LIKE '%' || :patternFilter || '%' OR
               notification_text LIKE '%' || :patternFilter || '%')
        ORDER BY timestamp DESC 
        LIMIT :limit OFFSET :offset
    """)
    fun filterHistoryPaginated(
        searchQuery: String,
        appFilter: String, 
        patternFilter: String,
        limit: Int, 
        offset: Int
    ): Flow<List<HistoryEntity>>
}

/**
 * Data class for app information used in filters.
 */
data class AppInfo(
    val app_name: String,
    val source_package: String?
) 