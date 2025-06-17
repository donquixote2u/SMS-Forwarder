package com.zerodev.smsforwarder.domain.usecase

import android.util.Log
import com.google.gson.Gson
import com.zerodev.smsforwarder.data.local.entity.ForwardingStatus
import com.zerodev.smsforwarder.data.remote.client.ForwardingResult
import com.zerodev.smsforwarder.data.remote.client.HttpClient
import com.zerodev.smsforwarder.data.repository.HistoryRepository
import com.zerodev.smsforwarder.data.repository.RuleRepository
import com.zerodev.smsforwarder.domain.model.ForwardingHistory
import com.zerodev.smsforwarder.domain.model.Rule
import com.zerodev.smsforwarder.domain.model.SmsMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for handling SMS forwarding business logic.
 * This is the main orchestrator that coordinates rule matching and HTTP forwarding.
 */
@Singleton
class SmsForwardingUseCase @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val historyRepository: HistoryRepository,
    private val httpClient: HttpClient
) {
    
    companion object {
        private const val TAG = "SmsForwardingUseCase"
    }
    
    private val gson = Gson()
    
    /**
     * Process an incoming SMS message by finding matching rules and forwarding to their endpoints.
     * This method runs in parallel for all matching rules.
     * 
     * @param smsMessage The incoming SMS message to process
     * @return List of ForwardingHistory entries representing the forwarding attempts
     */
    suspend fun processSms(smsMessage: SmsMessage): List<ForwardingHistory> {
        Log.d(TAG, "Processing SMS from ${smsMessage.getMaskedSender()}")
        
        if (!smsMessage.isValid()) {
            Log.w(TAG, "Invalid SMS message, skipping processing")
            return emptyList()
        }
        
        // Get all active rules
        val activeRules = ruleRepository.getActiveRules().first()
        
        if (activeRules.isEmpty()) {
            Log.d(TAG, "No active rules found, skipping processing")
            return emptyList()
        }
        
        // Find matching rules
        val matchingRules = activeRules.filter { rule ->
            rule.matches(smsMessage.body)
        }
        
        Log.d(TAG, "Found ${matchingRules.size} matching rules out of ${activeRules.size} active rules")
        
        if (matchingRules.isEmpty()) {
            return emptyList()
        }
        
        // Process all matching rules in parallel
        return coroutineScope {
            matchingRules.map { rule ->
                async {
                    forwardSmsToRule(smsMessage, rule)
                }
            }.awaitAll()
        }
    }
    
    /**
     * Forward an SMS message to a specific rule's endpoint.
     * 
     * @param smsMessage The SMS message to forward
     * @param rule The rule configuration
     * @return ForwardingHistory entry representing the forwarding attempt
     */
    private suspend fun forwardSmsToRule(smsMessage: SmsMessage, rule: Rule): ForwardingHistory {
        Log.d(TAG, "Forwarding SMS to rule: ${rule.name}")
        
        val requestPayload = gson.toJson(smsMessage.toApiPayload())
        
        // Create initial history entry with PENDING status
        val initialHistory = ForwardingHistory(
            ruleId = rule.id,
            ruleName = rule.name,
            smsMessage = smsMessage,
            requestPayload = requestPayload,
            status = ForwardingStatus.PENDING,
            createdAt = Clock.System.now()
        )
        
        val historyId = historyRepository.createHistory(initialHistory)
        
        try {
            // Attempt to forward the SMS
            val result = httpClient.forwardSms(smsMessage, rule)
            
            // Update history based on result
            val updatedHistory = when (result) {
                is ForwardingResult.Success -> {
                    Log.d(TAG, "Successfully forwarded SMS to ${rule.name} (${result.responseCode})")
                    initialHistory.copy(
                        id = historyId,
                        status = ForwardingStatus.SUCCESS,
                        responseCode = result.responseCode,
                        responseBody = result.responseBody
                    )
                }
                is ForwardingResult.HttpError -> {
                    Log.w(TAG, "HTTP error forwarding SMS to ${rule.name}: ${result.message}")
                    initialHistory.copy(
                        id = historyId,
                        status = ForwardingStatus.FAILED,
                        responseCode = result.responseCode,
                        responseBody = result.responseBody,
                        errorMessage = result.message
                    )
                }
                is ForwardingResult.NetworkError -> {
                    Log.e(TAG, "Network error forwarding SMS to ${rule.name}: ${result.message}")
                    initialHistory.copy(
                        id = historyId,
                        status = ForwardingStatus.FAILED,
                        errorMessage = result.message
                    )
                }
            }
            
            historyRepository.updateHistory(updatedHistory)
            return updatedHistory
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error forwarding SMS to ${rule.name}", e)
            
            val errorHistory = initialHistory.copy(
                id = historyId,
                status = ForwardingStatus.FAILED,
                errorMessage = "Unexpected error: ${e.message}"
            )
            
            historyRepository.updateHistory(errorHistory)
            return errorHistory
        }
    }
    
    /**
     * Get statistics about SMS forwarding.
     * 
     * @return ForwardingStatistics with counts and success rates
     */
    suspend fun getForwardingStatistics(): ForwardingStatistics {
        val historyStats = historyRepository.getHistoryStatistics()
        val activeRuleCount = ruleRepository.getActiveRuleCount()
        
        val totalAttempts = historyStats.values.sum()
        val successfulAttempts = historyStats[ForwardingStatus.SUCCESS] ?: 0
        val failedAttempts = historyStats[ForwardingStatus.FAILED] ?: 0
        val pendingAttempts = historyStats[ForwardingStatus.PENDING] ?: 0
        val retryingAttempts = historyStats[ForwardingStatus.RETRYING] ?: 0
        
        val successRate = if (totalAttempts > 0) {
            (successfulAttempts.toDouble() / totalAttempts) * 100
        } else {
            0.0
        }
        
        return ForwardingStatistics(
            activeRuleCount = activeRuleCount,
            totalAttempts = totalAttempts,
            successfulAttempts = successfulAttempts,
            failedAttempts = failedAttempts,
            pendingAttempts = pendingAttempts,
            retryingAttempts = retryingAttempts,
            successRate = successRate
        )
    }
}

/**
 * Statistics about SMS forwarding performance.
 */
data class ForwardingStatistics(
    val activeRuleCount: Int,
    val totalAttempts: Int,
    val successfulAttempts: Int,
    val failedAttempts: Int,
    val pendingAttempts: Int,
    val retryingAttempts: Int,
    val successRate: Double
) 