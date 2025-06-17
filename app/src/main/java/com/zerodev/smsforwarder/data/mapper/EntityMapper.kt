package com.zerodev.smsforwarder.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zerodev.smsforwarder.data.local.entity.RuleEntity
import com.zerodev.smsforwarder.data.local.entity.HistoryEntity
import com.zerodev.smsforwarder.domain.model.Rule
import com.zerodev.smsforwarder.domain.model.ForwardingHistory
import com.zerodev.smsforwarder.domain.model.SmsMessage

/**
 * Mapper functions to convert between domain models and database entities.
 */
object EntityMapper {
    
    private val gson = Gson()
    
    /**
     * Convert RuleEntity to Rule domain model.
     */
    fun RuleEntity.toDomain(): Rule {
        val headersMap = try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(headers, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
        
        return Rule(
            id = id,
            name = name,
            pattern = pattern,
            isRegex = isRegex,
            endpoint = endpoint,
            method = method,
            headers = headersMap,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    /**
     * Convert Rule domain model to RuleEntity.
     */
    fun Rule.toEntity(): RuleEntity {
        val headersJson = try {
            gson.toJson(headers)
        } catch (e: Exception) {
            "{}"
        }
        
        return RuleEntity(
            id = id,
            name = name,
            pattern = pattern,
            isRegex = isRegex,
            endpoint = endpoint,
            method = method,
            headers = headersJson,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    /**
     * Convert HistoryEntity to ForwardingHistory domain model.
     */
    fun HistoryEntity.toDomain(ruleName: String = "N/A"): ForwardingHistory {
        val smsMessage = SmsMessage(
            body = smsBody,
            sender = smsFrom,
            timestamp = smsTimestamp
        )
        
        return ForwardingHistory(
            id = id,
            ruleId = ruleId,
            ruleName = ruleName,
            smsMessage = smsMessage,
            matchedRule = matchedRule,
            requestPayload = requestPayload,
            responseCode = responseCode,
            responseBody = responseBody,
            status = status,
            errorMessage = errorMessage,
            createdAt = createdAt
        )
    }
    
    /**
     * Convert ForwardingHistory domain model to HistoryEntity.
     */
    fun ForwardingHistory.toEntity(): HistoryEntity {
        return HistoryEntity(
            id = id,
            ruleId = ruleId,
            smsBody = smsMessage.body,
            smsFrom = smsMessage.sender,
            smsTimestamp = smsMessage.timestamp,
            matchedRule = matchedRule,
            requestPayload = requestPayload,
            responseCode = responseCode,
            responseBody = responseBody,
            status = status,
            errorMessage = errorMessage,
            createdAt = createdAt
        )
    }
    
    /**
     * Convert list of RuleEntity to list of Rule domain models.
     */
    fun List<RuleEntity>.toDomainList(): List<Rule> {
        return map { it.toDomain() }
    }
    
    /**
     * Convert list of Rule domain models to list of RuleEntity.
     */
    fun List<Rule>.toEntityList(): List<RuleEntity> {
        return map { it.toEntity() }
    }
}