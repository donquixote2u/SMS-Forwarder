package com.zerodev.smsforwarder.data.local.dao

import androidx.room.*
import com.zerodev.smsforwarder.data.local.entity.RuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for forwarding rules (SMS and notifications).
 * Provides database operations for managing forwarding rules.
 */
@Dao
interface RuleDao {
    
    /**
     * Get all rules as a Flow for reactive updates.
     * @return Flow of all rules ordered by creation time (newest first)
     */
    @Query("SELECT * FROM rules ORDER BY created_at DESC")
    fun getAllRules(): Flow<List<RuleEntity>>
    
    /**
     * Get all active rules as a Flow.
     * @return Flow of active rules only
     */
    @Query("SELECT * FROM rules WHERE is_active = 1 ORDER BY created_at DESC")
    fun getActiveRules(): Flow<List<RuleEntity>>
    
    /**
     * Get active rules for a specific source type.
     * @param sourceType The source type ("SMS" or "NOTIFICATION")
     * @return Flow of active rules for the specified source type
     */
    @Query("SELECT * FROM rules WHERE is_active = 1 AND source = :sourceType ORDER BY created_at DESC")
    fun getActiveRulesBySource(sourceType: String): Flow<List<RuleEntity>>
    
    /**
     * Get active notification rules for a specific package.
     * @param packageName The package name to filter by
     * @return Flow of active notification rules for the specified package
     */
    @Query("SELECT * FROM rules WHERE is_active = 1 AND source = 'NOTIFICATION' AND (package_filter IS NULL OR package_filter = :packageName) ORDER BY created_at DESC")
    fun getActiveNotificationRulesForPackage(packageName: String): Flow<List<RuleEntity>>
    
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
    @Query("UPDATE rules SET is_active = :isActive WHERE id = :id")
    suspend fun toggleRuleStatus(id: Long, isActive: Boolean)
    
    /**
     * Get count of active rules.
     * @return Number of active rules
     */
    @Query("SELECT COUNT(*) FROM rules WHERE is_active = 1")
    suspend fun getActiveRuleCount(): Int
    
    /**
     * Get count of active rules by source type.
     * @param sourceType The source type ("SMS" or "NOTIFICATION")
     * @return Number of active rules for the specified source type
     */
    @Query("SELECT COUNT(*) FROM rules WHERE is_active = 1 AND source = :sourceType")
    suspend fun getActiveRuleCountBySource(sourceType: String): Int
} 