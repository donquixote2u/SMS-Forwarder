package com.zerodev.smsforwarder.data.repository

import com.zerodev.smsforwarder.data.local.dao.HistoryDao
import com.zerodev.smsforwarder.data.local.dao.RuleDao
import com.zerodev.smsforwarder.data.mapper.EntityMapper.toDomain
import com.zerodev.smsforwarder.data.mapper.EntityMapper.toEntity
import com.zerodev.smsforwarder.domain.model.ForwardingHistory
import com.zerodev.smsforwarder.domain.model.ForwardingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing forwarding history (SMS and notifications).
 * Provides a clean API for history operations.
 */
@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val ruleDao: RuleDao
) {
    
    /**
     * Get all history entries for display in the history screen.
     * @return Flow of all history entries (limited to 1000 most recent)
     */
    fun getAllHistory(): Flow<List<ForwardingHistory>> {
        return historyDao.getAllHistory().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    /**
     * Get history for a specific rule.
     * @param ruleId The rule ID
     * @return Flow of history entries for the rule
     */
    fun getHistoryByRule(ruleId: Long): Flow<List<ForwardingHistory>> {
        return historyDao.getHistoryByRule(ruleId).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    /**
     * Get history entries by source type.
     * @param sourceType The source type ("SMS" or "NOTIFICATION")
     * @return Flow of history entries for the specified source type
     */
    fun getHistoryBySourceType(sourceType: String): Flow<List<ForwardingHistory>> {
        return historyDao.getHistoryBySourceType(sourceType).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    /**
     * Get history entries for a specific package (notifications only).
     * @param packageName The package name
     * @return Flow of history entries for the specified package
     */
    fun getHistoryByPackage(packageName: String): Flow<List<ForwardingHistory>> {
        return historyDao.getHistoryByPackage(packageName).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    /**
     * Get a specific history entry by ID.
     * @param id The history ID
     * @return The history entry if found, null otherwise
     */
    suspend fun getHistoryById(id: Long): ForwardingHistory? {
        val entity = historyDao.getHistoryById(id) ?: return null
        return entity.toDomain()
    }
    
    /**
     * Create a new history entry.
     * @param history The history entry to create (ID will be ignored)
     * @return The ID of the created history entry
     */
    suspend fun createHistory(history: ForwardingHistory): Long {
        val historyToInsert = history.copy(
            id = 0 // Let database generate ID
        )
        
        return historyDao.insertHistory(historyToInsert.toEntity())
    }
    
    /**
     * Update an existing history entry.
     * @param history The history entry with updated values
     */
    suspend fun updateHistory(history: ForwardingHistory) {
        historyDao.updateHistory(history.toEntity())
    }
    
    /**
     * Get history entries by status.
     * @param status The forwarding status to filter by
     * @return Flow of history entries with the specified status
     */
    fun getHistoryByStatus(status: ForwardingStatus): Flow<List<ForwardingHistory>> {
        return historyDao.getHistoryByStatus(status.name).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    /**
     * Clean up old history entries to keep database size manageable.
     * Keeps only the most recent 1000 entries.
     */
    suspend fun cleanupOldHistory() {
        historyDao.cleanupOldHistory()
    }
    
    /**
     * Delete all history entries for a specific rule.
     * @param ruleId The rule ID
     */
    suspend fun deleteHistoryByRule(ruleId: Long) {
        historyDao.deleteHistoryByRule(ruleId)
    }
    
    /**
     * Delete all history entries.
     */
    suspend fun deleteAllHistory() {
        historyDao.deleteAllHistory()
    }
    
    /**
     * Get count of history entries by status.
     * @param status The forwarding status
     * @return Count of entries with the specified status
     */
    suspend fun getHistoryCountByStatus(status: ForwardingStatus): Int {
        return historyDao.getHistoryCountByStatus(status.name)
    }
    
    /**
     * Get recent history entries (last 24 hours).
     * @return Flow of recent history entries
     */
    fun getRecentHistory(): Flow<List<ForwardingHistory>> {
        val last24Hours = Clock.System.now().toEpochMilliseconds() - (24 * 60 * 60 * 1000L)
        return historyDao.getRecentHistory(last24Hours).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    /**
     * Get statistics for the history.
     * @return Map of status to count
     */
    suspend fun getHistoryStatistics(): Map<ForwardingStatus, Int> {
        val statusCounts = historyDao.getHistoryStatistics()
        val statsMap = statusCounts.associate { it.status to it.count }
        
        return ForwardingStatus.values().associateWith { status ->
            statsMap[status.name] ?: 0
        }
    }
    
    /**
     * Get comprehensive statistics.
     * @return HistoryStatistics object with all counts
     */
    suspend fun getComprehensiveStatistics(): HistoryStatistics {
        return HistoryStatistics(
            totalCount = historyDao.getTotalCount(),
            successfulCount = historyDao.getSuccessfulCount(),
            failedCount = historyDao.getFailedCount(),
            matchedCount = historyDao.getMatchedCount()
        )
    }
    
    /**
     * Get history entries with pagination.
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return Flow of paginated history entries
     */
    fun getHistoryPaginated(page: Int, pageSize: Int): Flow<List<ForwardingHistory>> {
        val offset = page * pageSize
        return historyDao.getHistoryPaginated(pageSize, offset).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    /**
     * Delete a specific history entry by ID.
     * @param id The history entry ID to delete
     */
    suspend fun deleteHistoryById(id: Long) {
        historyDao.deleteHistoryById(id)
    }
    
    /**
     * Search history entries with fuzzy search.
     * @param searchQuery The search query
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return Flow of matching history entries
     */
    fun searchHistoryPaginated(searchQuery: String, page: Int, pageSize: Int): Flow<List<ForwardingHistory>> {
        val offset = page * pageSize
        return historyDao.searchHistoryPaginated(searchQuery, pageSize, offset).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    /**
     * Filter history entries by app.
     * @param appFilter The app package name or app name to filter by
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return Flow of filtered history entries
     */
    fun filterByAppPaginated(appFilter: String, page: Int, pageSize: Int): Flow<List<ForwardingHistory>> {
        val offset = page * pageSize
        return historyDao.filterByAppPaginated(appFilter, pageSize, offset).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    /**
     * Filter history entries by pattern.
     * @param pattern The pattern to search for in message content
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return Flow of filtered history entries
     */
    fun filterByPatternPaginated(pattern: String, page: Int, pageSize: Int): Flow<List<ForwardingHistory>> {
        val offset = page * pageSize
        return historyDao.filterByPatternPaginated(pattern, pageSize, offset).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    /**
     * Get distinct apps from history for filter dropdown.
     * @return List of distinct app information
     */
    suspend fun getDistinctApps(): List<com.zerodev.smsforwarder.data.local.dao.AppInfo> {
        return historyDao.getDistinctApps()
    }
    
    /**
     * Filter history with combined filters (search, app, pattern).
     * @param searchQuery Optional search query (empty string if not filtering)
     * @param appFilter Optional app filter (empty string if not filtering)
     * @param patternFilter Optional pattern filter (empty string if not filtering)
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return Flow of filtered history entries
     */
    fun filterHistoryPaginated(
        searchQuery: String,
        appFilter: String,
        patternFilter: String,
        page: Int,
        pageSize: Int
    ): Flow<List<ForwardingHistory>> {
        val offset = page * pageSize
        return historyDao.filterHistoryPaginated(
            searchQuery, appFilter, patternFilter, pageSize, offset
        ).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    /**
     * Get statistics with current filters applied.
     * @param searchQuery Optional search query (empty string if not filtering)
     * @param appFilter Optional app filter (empty string if not filtering)
     * @param patternFilter Optional pattern filter (empty string if not filtering)
     * @return HistoryStatistics object with filtered counts
     */
    suspend fun getFilteredStatistics(
        searchQuery: String,
        appFilter: String,
        patternFilter: String
    ): HistoryStatistics {
        return if (searchQuery.isEmpty() && appFilter.isEmpty() && patternFilter.isEmpty()) {
            // No filters, use regular statistics
            getComprehensiveStatistics()
        } else {
            // Filters applied, get filtered counts
            HistoryStatistics(
                totalCount = historyDao.getFilteredTotalCount(searchQuery, appFilter, patternFilter),
                successfulCount = historyDao.getSuccessfulCount(), // These don't change with filters
                failedCount = historyDao.getFailedCount(),         // as they're global stats
                matchedCount = historyDao.getFilteredMatchedCount(searchQuery, appFilter, patternFilter)
            )
        }
    }
}

/**
 * Data class for comprehensive history statistics.
 */
data class HistoryStatistics(
    val totalCount: Int,
    val successfulCount: Int,
    val failedCount: Int,
    val matchedCount: Int
) {
    val successRate: Double
        get() = if (totalCount > 0) (successfulCount.toDouble() / totalCount) * 100 else 0.0
    
    val matchRate: Double
        get() = if (totalCount > 0) (matchedCount.toDouble() / totalCount) * 100 else 0.0
} 