package com.zerodev.smsforwarder.domain.usecase

import android.util.Log
import com.google.gson.Gson
import com.zerodev.smsforwarder.data.remote.client.ForwardingResult
import com.zerodev.smsforwarder.data.remote.client.HttpClient
import com.zerodev.smsforwarder.data.repository.HistoryRepository
import com.zerodev.smsforwarder.data.repository.RuleRepository
import com.zerodev.smsforwarder.domain.model.ForwardingHistory
import com.zerodev.smsforwarder.domain.model.ForwardingStatus
import com.zerodev.smsforwarder.domain.model.Rule
import com.zerodev.smsforwarder.domain.model.SmsMessage
import com.zerodev.smsforwarder.domain.model.SourceType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for handling SMS and notification forwarding business logic.
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
     * This method runs in parallel for all matching rules and ALWAYS logs the SMS to history for debugging.
     * 
     * @param smsMessage The incoming SMS message to process
     * @return List of ForwardingHistory entries representing the forwarding attempts
     */
    suspend fun processSms(smsMessage: SmsMessage): List<ForwardingHistory> {
        Log.d(TAG, "📱 === SMS RECEIVED ===")
        Log.d(TAG, "📱 From: ${smsMessage.getMaskedSender()}")
        Log.d(TAG, "📱 Body: ${smsMessage.body}")
        Log.d(TAG, "📱 Timestamp: ${smsMessage.timestamp}")
        
        if (!smsMessage.isValid()) {
            Log.w(TAG, "❌ Invalid SMS message, saving to history as invalid")
            val invalidHistory = createFailedHistory(
                sourceType = "SMS",
                senderNumber = smsMessage.sender,
                messageBody = smsMessage.body,
                errorMessage = "Invalid SMS message (empty body or sender)",
                timestamp = smsMessage.timestamp
            )
            historyRepository.createHistory(invalidHistory)
            return emptyList()
        }
        
        return processMessage(
            content = smsMessage.body,
            sourceType = SourceType.SMS,
            packageName = null,
            senderNumber = smsMessage.sender,
            messageBody = smsMessage.body,
            timestamp = smsMessage.timestamp
        )
    }
    
    /**
     * Process an incoming notification by finding matching rules and forwarding to their endpoints.
     * This method runs in parallel for all matching rules and ALWAYS logs the notification to history for debugging.
     * 
     * @param packageName Package name of the app that posted the notification
     * @param appLabel Human-readable app name
     * @param title Notification title
     * @param text Notification text content
     * @param postTime Timestamp when notification was posted
     * @param extras JSON-serializable extras from the notification
     * @return List of ForwardingHistory entries representing the forwarding attempts
     */
    suspend fun processNotification(
        packageName: String,
        appLabel: String,
        title: String,
        text: String,
        postTime: Long,
        extras: Map<String, Any?>
    ): List<ForwardingHistory> {
        Log.d(TAG, "🔔 === NOTIFICATION RECEIVED ===")
        Log.d(TAG, "🔔 From: $appLabel ($packageName)")
        Log.d(TAG, "🔔 Title: $title")
        Log.d(TAG, "🔔 Text: $text")
        Log.d(TAG, "🔔 PostTime: $postTime")
        
        // Combine title and text for pattern matching
        val content = if (title.isNotBlank() && text.isNotBlank()) {
            "$title: $text"
        } else {
            title.ifBlank { text }
        }
        
        if (content.isBlank()) {
            Log.w(TAG, "❌ Empty notification content, saving to history as invalid")
            val invalidHistory = createFailedHistory(
                sourceType = "NOTIFICATION",
                sourcePackage = packageName,
                sourceAppName = appLabel,
                notificationTitle = title,
                notificationText = text,
                messageBody = content,
                errorMessage = "Empty notification content",
                timestamp = Instant.fromEpochMilliseconds(postTime)
            )
            historyRepository.createHistory(invalidHistory)
            return emptyList()
        }
        
        return processMessage(
            content = content,
            sourceType = SourceType.NOTIFICATION,
            packageName = packageName,
            sourceAppName = appLabel,
            notificationTitle = title,
            notificationText = text,
            messageBody = content,
            timestamp = Instant.fromEpochMilliseconds(postTime),
            extras = extras
        )
    }
    
    /**
     * Generic message processing for both SMS and notifications.
     * 
     * @param content Content to match against (SMS body or notification title+text)
     * @param sourceType Source type (SMS or NOTIFICATION)
     * @param packageName Package name for notifications
     * @param senderNumber Phone number for SMS
     * @param sourceAppName App name for notifications
     * @param notificationTitle Notification title
     * @param notificationText Notification text
     * @param messageBody Message body content
     * @param timestamp When the message was received
     * @param extras Additional data for notifications
     * @return List of ForwardingHistory entries
     */
    private suspend fun processMessage(
        content: String,
        sourceType: SourceType,
        packageName: String? = null,
        senderNumber: String? = null,
        sourceAppName: String? = null,
        notificationTitle: String? = null,
        notificationText: String? = null,
        messageBody: String,
        timestamp: Instant,
        extras: Map<String, Any?>? = null
    ): List<ForwardingHistory> {
        
        // Get all active rules for this source type
        val activeRules = ruleRepository.getActiveRules().first()
            .filter { rule -> rule.source == sourceType }
        
        Log.d(TAG, "Found ${activeRules.size} active ${sourceType.name} rules")
        
        if (activeRules.isEmpty()) {
            Log.d(TAG, "No active ${sourceType.name} rules found, saving message as no rules configured")
            val noRulesHistory = createFailedHistory(
                sourceType = sourceType.name,
                senderNumber = senderNumber,
                sourcePackage = packageName,
                sourceAppName = sourceAppName,
                notificationTitle = notificationTitle,
                notificationText = notificationText,
                messageBody = messageBody,
                errorMessage = "No active ${sourceType.name} rules configured",
                timestamp = timestamp
            )
            historyRepository.createHistory(noRulesHistory)
            return emptyList()
        }
        
        // Find matching rules
        val matchingRules = activeRules.filter { rule ->
            val packageMatches = rule.appliesToPackage(packageName ?: "")
            val contentMatches = rule.matches(content)
            val matches = packageMatches && contentMatches
            
            Log.d(TAG, "Rule '${rule.name}' - Package matches: $packageMatches, Content matches: $contentMatches, Overall: $matches")
            matches
        }
        
        Log.d(TAG, "Found ${matchingRules.size} matching rules out of ${activeRules.size} active ${sourceType.name} rules")
        
        if (matchingRules.isEmpty()) {
            Log.d(TAG, "No matching rules found, saving message as no match")
            val noMatchHistory = createFailedHistory(
                sourceType = sourceType.name,
                senderNumber = senderNumber,
                sourcePackage = packageName,
                sourceAppName = sourceAppName,
                notificationTitle = notificationTitle,
                notificationText = notificationText,
                messageBody = messageBody,
                errorMessage = "Content did not match any rule patterns",
                timestamp = timestamp
            )
            historyRepository.createHistory(noMatchHistory)
            return emptyList()
        }
        
        // Process all matching rules in parallel
        return coroutineScope {
            matchingRules.map { rule ->
                async {
                    forwardMessageToRule(
                        rule = rule,
                        content = content,
                        sourceType = sourceType.name,
                        senderNumber = senderNumber,
                        sourcePackage = packageName,
                        sourceAppName = sourceAppName,
                        notificationTitle = notificationTitle,
                        notificationText = notificationText,
                        messageBody = messageBody,
                        timestamp = timestamp,
                        extras = extras
                    )
                }
            }.awaitAll()
        }
    }
    
    /**
     * Forward a message to a specific rule's endpoint.
     * 
     * @param rule The rule configuration
     * @param content Content that matched the rule
     * @param sourceType Source type ("SMS" or "NOTIFICATION")
     * @param senderNumber Phone number for SMS
     * @param sourcePackage Package name for notifications
     * @param sourceAppName App name for notifications
     * @param notificationTitle Notification title
     * @param notificationText Notification text
     * @param messageBody Message body content
     * @param timestamp When the message was received
     * @param extras Additional data for notifications
     * @return ForwardingHistory entry representing the forwarding attempt
     */
    private suspend fun forwardMessageToRule(
        rule: Rule,
        content: String,
        sourceType: String,
        senderNumber: String? = null,
        sourcePackage: String? = null,
        sourceAppName: String? = null,
        notificationTitle: String? = null,
        notificationText: String? = null,
        messageBody: String,
        timestamp: Instant,
        extras: Map<String, Any?>? = null
    ): ForwardingHistory {
        Log.d(TAG, "Forwarding ${sourceType.lowercase()} to rule: ${rule.name}")
        
        val requestBody = createRequestPayload(
            sourceType = sourceType,
            senderNumber = senderNumber,
            sourcePackage = sourcePackage,
            sourceAppName = sourceAppName,
            notificationTitle = notificationTitle,
            notificationText = notificationText,
            messageBody = messageBody,
            timestamp = timestamp,
            extras = extras
        )
        
        // Create initial history entry
        val initialHistory = ForwardingHistory(
            ruleId = rule.id,
            matchedRule = true,
            senderNumber = senderNumber,
            messageBody = messageBody,
            sourceType = sourceType,
            sourcePackage = sourcePackage,
            sourceAppName = sourceAppName,
            notificationTitle = notificationTitle,
            notificationText = notificationText,
            endpoint = rule.endpoint,
            method = rule.method,
            requestHeaders = rule.headers,
            requestBody = requestBody,
            status = ForwardingStatus.RECEIVED,
            timestamp = timestamp,
            forwardedAt = Clock.System.now()
        )
        
        val historyId = historyRepository.createHistory(initialHistory)
        
        try {
            // Attempt to forward the message
            val result = when (sourceType) {
                "SMS" -> httpClient.forwardSms(
                    SmsMessage(messageBody, senderNumber ?: "", timestamp), 
                    rule
                )
                "NOTIFICATION" -> httpClient.forwardNotification(
                    packageName = sourcePackage ?: "",
                    appLabel = sourceAppName ?: "",
                    title = notificationTitle ?: "",
                    text = notificationText ?: "",
                    postTime = timestamp.toEpochMilliseconds(),
                    extras = extras ?: emptyMap(),
                    rule = rule
                )
                else -> throw IllegalArgumentException("Unknown source type: $sourceType")
            }
            
            // Update history based on result
            val updatedHistory = when (result) {
                is ForwardingResult.Success -> {
                    Log.d(TAG, "✅ Successfully forwarded ${sourceType.lowercase()} to ${rule.name} (${result.responseCode})")
                    initialHistory.copy(
                        id = historyId,
                        status = ForwardingStatus.SUCCESS,
                        responseCode = result.responseCode,
                        responseBody = result.responseBody
                    )
                }
                is ForwardingResult.HttpError -> {
                    Log.w(TAG, "⚠️ HTTP error forwarding ${sourceType.lowercase()} to ${rule.name}: ${result.message}")
                    initialHistory.copy(
                        id = historyId,
                        status = ForwardingStatus.FAILED,
                        responseCode = result.responseCode,
                        responseBody = result.responseBody,
                        errorMessage = result.message
                    )
                }
                is ForwardingResult.NetworkError -> {
                    Log.e(TAG, "❌ Network error forwarding ${sourceType.lowercase()} to ${rule.name}: ${result.message}")
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
            Log.e(TAG, "💥 Unexpected error forwarding ${sourceType.lowercase()} to ${rule.name}", e)
            
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
     * Create a failed history entry for messages that couldn't be processed.
     */
    private fun createFailedHistory(
        sourceType: String,
        senderNumber: String? = null,
        sourcePackage: String? = null,
        sourceAppName: String? = null,
        notificationTitle: String? = null,
        notificationText: String? = null,
        messageBody: String,
        errorMessage: String,
        timestamp: Instant
    ): ForwardingHistory {
        return ForwardingHistory(
            ruleId = null,
            matchedRule = false,
            senderNumber = senderNumber,
            messageBody = messageBody,
            sourceType = sourceType,
            sourcePackage = sourcePackage,
            sourceAppName = sourceAppName,
            notificationTitle = notificationTitle,
            notificationText = notificationText,
            status = ForwardingStatus.NO_RULE_MATCHED,
            errorMessage = errorMessage,
            timestamp = timestamp
        )
    }
    
    /**
     * Create the request payload for forwarding.
     */
    private fun createRequestPayload(
        sourceType: String,
        senderNumber: String? = null,
        sourcePackage: String? = null,
        sourceAppName: String? = null,
        notificationTitle: String? = null,
        notificationText: String? = null,
        messageBody: String,
        timestamp: Instant,
        extras: Map<String, Any?>? = null
    ): String {
        val payload = mutableMapOf<String, Any?>(
            "sourceType" to sourceType,
            "messageBody" to messageBody,
            "timestamp" to timestamp.toEpochMilliseconds()
        )
        
        when (sourceType) {
            "SMS" -> {
                payload["senderNumber"] = senderNumber
            }
            "NOTIFICATION" -> {
                payload["sourcePackage"] = sourcePackage
                payload["sourceAppName"] = sourceAppName
                payload["notificationTitle"] = notificationTitle
                payload["notificationText"] = notificationText
                if (extras != null) {
                    payload["extras"] = extras
                }
            }
        }
        
        return gson.toJson(payload)
    }
    
    /**
     * Get statistics about forwarding.
     * 
     * @return ForwardingStatistics with counts and success rates
     */
    suspend fun getForwardingStatistics(): ForwardingStatistics {
        val historyStats = historyRepository.getHistoryStatistics()
        val activeRuleCount = ruleRepository.getActiveRuleCount()
        
        val totalAttempts = historyStats.values.sum()
        val successfulAttempts = historyStats[ForwardingStatus.SUCCESS] ?: 0
        val failedAttempts = historyStats[ForwardingStatus.FAILED] ?: 0
        val retryAttempts = historyStats[ForwardingStatus.RETRY] ?: 0
        val matchedAttempts = historyStats.filterKeys { 
            it != ForwardingStatus.NO_RULE_MATCHED && it != ForwardingStatus.RECEIVED 
        }.values.sum()
        
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
            retryAttempts = retryAttempts,
            matchedAttempts = matchedAttempts,
            successRate = successRate
        )
    }
}

/**
 * Statistics about forwarding performance.
 */
data class ForwardingStatistics(
    val activeRuleCount: Int,
    val totalAttempts: Int,
    val successfulAttempts: Int,
    val failedAttempts: Int,
    val retryAttempts: Int,
    val matchedAttempts: Int,
    val successRate: Double
) 