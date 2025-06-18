package com.zerodev.smsforwarder.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zerodev.smsforwarder.data.local.entity.RuleEntity
import com.zerodev.smsforwarder.data.local.entity.HistoryEntity
import com.zerodev.smsforwarder.domain.model.Rule
import com.zerodev.smsforwarder.domain.model.ForwardingHistory
import com.zerodev.smsforwarder.domain.model.SmsMessage
import com.zerodev.smsforwarder.domain.model.SourceType
import com.zerodev.smsforwarder.domain.model.ForwardingStatus

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
        
        val sourceType = try {
            SourceType.valueOf(source)
        } catch (e: Exception) {
            SourceType.SMS // Default fallback
        }
        
        return Rule(
            id = id,
            name = name,
            pattern = pattern,
            source = sourceType,
            packageFilter = packageFilter,
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
            source = source.name,
            packageFilter = packageFilter,
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
    fun HistoryEntity.toDomain(): ForwardingHistory {
        return ForwardingHistory(
            id = id,
            ruleId = ruleId,
            matchedRule = matchedRule,
            senderNumber = senderNumber,
            messageBody = messageBody,
            sourceType = sourceType,
            sourcePackage = sourcePackage,
            sourceAppName = sourceAppName,
            notificationTitle = notificationTitle,
            notificationText = notificationText,
            endpoint = endpoint,
            method = method,
            requestHeaders = try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson<Map<String, String>>(requestHeaders, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            },
            requestBody = requestBody,
            responseCode = responseCode,
            responseBody = responseBody,
            status = ForwardingStatus.valueOf(status),
            errorMessage = errorMessage,
            timestamp = timestamp,
            forwardedAt = forwardedAt
        )
    }
    
    /**
     * Convert ForwardingHistory domain model to HistoryEntity.
     */
    fun ForwardingHistory.toEntity(): HistoryEntity {
        val headersJson = try {
            gson.toJson(requestHeaders)
        } catch (e: Exception) {
            "{}"
        }
        
        return HistoryEntity(
            id = id,
            ruleId = ruleId,
            matchedRule = matchedRule,
            senderNumber = senderNumber,
            messageBody = messageBody,
            sourceType = sourceType,
            sourcePackage = sourcePackage,
            sourceAppName = sourceAppName,
            notificationTitle = notificationTitle,
            notificationText = notificationText,
            endpoint = endpoint,
            method = method,
            requestHeaders = headersJson,
            requestBody = requestBody,
            responseCode = responseCode,
            responseBody = responseBody,
            status = status.name,
            errorMessage = errorMessage,
            timestamp = timestamp,
            forwardedAt = forwardedAt
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