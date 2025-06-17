package com.zerodev.smsforwarder.data.local.dao

import androidx.room.*
import com.zerodev.smsforwarder.data.local.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SMS forwarding rules.
 * Provides database operations for managing forwarding rules.
 */
@Dao
interface RuleDao {
    
    /**
     * Get all rules as a Flow for reactive updates.
     * @return Flow of all rules ordered by creation time (newest first)
     */
    @Query("SELECT * FROM rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<RuleEntity>>
    
    /**
     * Get all active rules as a Flow.
     * @return Flow of active rules only
     */
    @Query("SELECT * FROM rules WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveRules(): Flow<List<RuleEntity>>
    
    /**
     * Get a specific rule by ID.
     * @param id The rule ID
     * @return The rule if found, null otherwise
     */
    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: Long): RuleEntity?
    
    /**
     * Insert a new rule.
     * @param rule The rule to insert
     * @return The ID of the inserted rule
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long
    
    /**
     * Update an existing rule.
     * @param rule The rule with updated values
     */
    @Update
    suspend fun updateRule(rule: RuleEntity)
    
    /**
     * Delete a rule by ID.
     * @param id The rule ID to delete
     */
    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
    
    /**
     * Delete all rules.
     */
    @Query("DELETE FROM rules")
    suspend fun deleteAllRules()
    
    /**
     * Toggle the active status of a rule.
     * @param id The rule ID
     * @param isActive The new active status
     */
    @Query("UPDATE rules SET isActive = :isActive WHERE id = :id")
    suspend fun toggleRuleStatus(id: Long, isActive: Boolean)
    
    /**
     * Get count of active rules.
     * @return Number of active rules
     */
    @Query("SELECT COUNT(*) FROM rules WHERE isActive = 1")
    suspend fun getActiveRuleCount(): Int
} 