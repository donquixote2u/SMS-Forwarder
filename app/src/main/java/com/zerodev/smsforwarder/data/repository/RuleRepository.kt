package com.zerodev.smsforwarder.data.repository

import com.zerodev.smsforwarder.data.local.dao.RuleDao
import com.zerodev.smsforwarder.data.mapper.EntityMapper.toDomain
import com.zerodev.smsforwarder.data.mapper.EntityMapper.toEntity
import com.zerodev.smsforwarder.data.mapper.EntityMapper.toDomainList
import com.zerodev.smsforwarder.domain.model.Rule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing SMS forwarding rules.
 * Provides a clean API for rule CRUD operations.
 */
@Singleton
class RuleRepository @Inject constructor(
    private val ruleDao: RuleDao
) {
    
    /**
     * Get all rules as a Flow for reactive updates.
     * @return Flow of all rules ordered by creation time (newest first)
     */
    fun getAllRules(): Flow<List<Rule>> {
        return ruleDao.getAllRules().map { entities ->
            entities.toDomainList()
        }
    }
    
    /**
     * Get all active rules as a Flow.
     * @return Flow of active rules only
     */
    fun getActiveRules(): Flow<List<Rule>> {
        return ruleDao.getActiveRules().map { entities ->
            entities.toDomainList()
        }
    }
    
    /**
     * Get a specific rule by ID.
     * @param id The rule ID
     * @return The rule if found, null otherwise
     */
    suspend fun getRuleById(id: Long): Rule? {
        return ruleDao.getRuleById(id)?.toDomain()
    }
    
    /**
     * Create a new rule.
     * @param rule The rule to create (ID will be ignored)
     * @return The ID of the created rule
     */
    suspend fun createRule(rule: Rule): Long {
        val now = Clock.System.now()
        val ruleToInsert = rule.copy(
            id = 0, // Let database generate ID
            createdAt = now,
            updatedAt = now
        )
        
        return ruleDao.insertRule(ruleToInsert.toEntity())
    }
    
    /**
     * Update an existing rule.
     * @param rule The rule with updated values
     */
    suspend fun updateRule(rule: Rule) {
        val updatedRule = rule.copy(updatedAt = Clock.System.now())
        ruleDao.updateRule(updatedRule.toEntity())
    }
    
    /**
     * Delete a rule by ID.
     * @param id The rule ID to delete
     */
    suspend fun deleteRule(id: Long) {
        ruleDao.deleteRuleById(id)
    }
    
    /**
     * Delete all rules.
     */
    suspend fun deleteAllRules() {
        ruleDao.deleteAllRules()
    }
    
    /**
     * Toggle the active status of a rule.
     * @param id The rule ID
     * @param isActive The new active status
     */
    suspend fun toggleRuleStatus(id: Long, isActive: Boolean) {
        ruleDao.toggleRuleStatus(id, isActive)
    }
    
    /**
     * Get count of active rules.
     * @return Number of active rules
     */
    suspend fun getActiveRuleCount(): Int {
        return ruleDao.getActiveRuleCount()
    }
    
    /**
     * Check if any active rules exist.
     * @return true if there are active rules, false otherwise
     */
    suspend fun hasActiveRules(): Boolean {
        return getActiveRuleCount() > 0
    }
} 