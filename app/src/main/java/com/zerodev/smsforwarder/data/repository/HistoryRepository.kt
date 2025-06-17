package com.zerodev.smsforwarder.data.repository

import com.zerodev.smsforwarder.data.local.dao.HistoryDao
import com.zerodev.smsforwarder.data.local.dao.RuleDao
import com.zerodev.smsforwarder.data.local.entity.ForwardingStatus
import com.zerodev.smsforwarder.data.mapper.EntityMapper.toDomain
import com.zerodev.smsforwarder.data.mapper.EntityMapper.toEntity
import com.zerodev.smsforwarder.domain.model.ForwardingHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing SMS forwarding history.
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
            entities.map { entity ->
                val ruleName = if (entity.ruleId != null) {
                    ruleDao.getRuleById(entity.ruleId)?.name ?: "Unknown Rule"
                } else {
                    "N/A"
                }
                entity.toDomain(ruleName)
            }
        }
    }
    
    /**
     * Get history for a specific rule.
     * @param ruleId The rule ID
     * @return Flow of history entries for the rule
     */
    fun getHistoryByRule(ruleId: Long): Flow<List<ForwardingHistory>> {
        return historyDao.getHistoryByRule(ruleId).map { entities ->
            entities.map { entity ->
                val ruleName = if (entity.ruleId != null) {
                    ruleDao.getRuleById(entity.ruleId)?.name ?: "Unknown Rule"
                } else {
                    "N/A"
                }
                entity.toDomain(ruleName)
            }
        }
    }
    
    /**
     * Get a specific history entry by ID.
     * @param id The history ID
     * @return The history entry if found, null otherwise
     */
    suspend fun getHistoryById(id: Long): ForwardingHistory? {
        val entity = historyDao.getHistoryById(id) ?: return null
        val ruleName = if (entity.ruleId != null) {
            ruleDao.getRuleById(entity.ruleId)?.name ?: "Unknown Rule"
        } else {
            "N/A"
        }
        return entity.toDomain(ruleName)
    }
    
    /**
     * Create a new history entry.
     * @param history The history entry to create (ID will be ignored)
     * @return The ID of the created history entry
     */
    suspend fun createHistory(history: ForwardingHistory): Long {
        val historyToInsert = history.copy(
            id = 0, // Let database generate ID
            createdAt = Clock.System.now()
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
        return historyDao.getHistoryByStatus(status).map { entities ->
            entities.map { entity ->
                val ruleName = if (entity.ruleId != null) {
                    ruleDao.getRuleById(entity.ruleId)?.name ?: "Unknown Rule"
                } else {
                    "N/A"
                }
                entity.toDomain(ruleName)
            }
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
        return historyDao.getHistoryCountByStatus(status)
    }
    
    /**
     * Get recent history entries (last 24 hours).
     * @return Flow of recent history entries
     */
    fun getRecentHistory(): Flow<List<ForwardingHistory>> {
        val last24Hours = Clock.System.now().epochSeconds - (24 * 60 * 60)
        return historyDao.getRecentHistory(last24Hours).map { entities ->
            entities.map { entity ->
                val ruleName = if (entity.ruleId != null) {
                    ruleDao.getRuleById(entity.ruleId)?.name ?: "Unknown Rule"
                } else {
                    "N/A"
                }
                entity.toDomain(ruleName)
            }
        }
    }
    
    /**
     * Get statistics for the history.
     * @return Map of status to count
     */
    suspend fun getHistoryStatistics(): Map<ForwardingStatus, Int> {
        return ForwardingStatus.values().associateWith { status ->
            getHistoryCountByStatus(status)
        }
    }
} 