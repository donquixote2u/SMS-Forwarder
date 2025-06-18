package com.zerodev.smsforwarder.data.remote.client

import com.zerodev.smsforwarder.data.remote.api.ForwardingApiService
import com.zerodev.smsforwarder.domain.model.Rule
import com.zerodev.smsforwarder.domain.model.SmsMessage
import kotlinx.coroutines.delay
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP client for forwarding SMS messages and notifications to external APIs.
 * Handles retry logic and different HTTP methods.
 */
@Singleton
class HttpClient @Inject constructor(
    private val apiService: ForwardingApiService
) {
    
    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY = 1000L
        private const val RETRY_MULTIPLIER = 2L
    }
    
    /**
     * Forward an SMS message to an endpoint according to the rule configuration.
     * 
     * @param smsMessage The SMS message to forward
     * @param rule The forwarding rule configuration
     * @return ForwardingResult indicating success or failure
     */
    suspend fun forwardSms(smsMessage: SmsMessage, rule: Rule): ForwardingResult {
        val payload = smsMessage.toApiPayload()
        
        return forwardPayload(payload, rule, "SMS")
    }
    
    /**
     * Forward a notification to an endpoint according to the rule configuration.
     * 
     * @param packageName Package name of the app that posted the notification
     * @param appLabel Human-readable app name
     * @param title Notification title
     * @param text Notification text content
     * @param postTime Timestamp when notification was posted
     * @param extras JSON-serializable extras from the notification
     * @param rule The forwarding rule configuration
     * @return ForwardingResult indicating success or failure
     */
    suspend fun forwardNotification(
        packageName: String,
        appLabel: String,
        title: String,
        text: String,
        postTime: Long,
        extras: Map<String, Any?>,
        rule: Rule
    ): ForwardingResult {
        val payload = mapOf(
            "sourceType" to "NOTIFICATION",
            "packageName" to packageName,
            "appLabel" to appLabel,
            "title" to title,
            "text" to text,
            "postTime" to postTime,
            "extras" to extras,
            "timestamp" to System.currentTimeMillis()
        )
        
        return forwardPayload(payload, rule, "NOTIFICATION")
    }
    
    /**
     * Forward a generic payload to an endpoint according to the rule configuration.
     * 
     * @param payload The data payload to forward
     * @param rule The forwarding rule configuration
     * @param sourceType Source type for logging ("SMS" or "NOTIFICATION")
     * @return ForwardingResult indicating success or failure
     */
    private suspend fun forwardPayload(
        payload: Map<String, Any?>,
        rule: Rule,
        sourceType: String
    ): ForwardingResult {
        val headers = rule.headers.toMutableMap().apply {
            putIfAbsent("Content-Type", "application/json")
            putIfAbsent("User-Agent", "SMS-Forwarder/1.0")
            putIfAbsent("X-Source-Type", sourceType)
        }
        
        // Filter out null values and convert to Map<String, Any>
        val cleanPayload = payload.filterValues { it != null }.mapValues { it.value!! }
        
        return executeWithRetry(
            maxAttempts = MAX_RETRY_ATTEMPTS,
            initialDelay = INITIAL_RETRY_DELAY
        ) {
            when (rule.method.uppercase()) {
                "GET" -> {
                    val queryParams = cleanPayload.mapValues { it.value.toString() }
                    apiService.getFromEndpoint(rule.endpoint, headers, queryParams)
                }
                "POST" -> {
                    apiService.postToEndpoint(rule.endpoint, headers, cleanPayload)
                }
                "PUT" -> {
                    apiService.putToEndpoint(rule.endpoint, headers, cleanPayload)
                }
                "PATCH" -> {
                    apiService.patchToEndpoint(rule.endpoint, headers, cleanPayload)
                }
                else -> {
                    // Default to POST for unknown methods
                    apiService.postToEndpoint(rule.endpoint, headers, cleanPayload)
                }
            }
        }
    }
    
    /**
     * Execute an HTTP request with exponential backoff retry logic.
     * 
     * @param maxAttempts Maximum number of retry attempts
     * @param initialDelay Initial delay before first retry
     * @param request The HTTP request to execute
     * @return ForwardingResult indicating success or failure
     */
    private suspend fun executeWithRetry(
        maxAttempts: Int,
        initialDelay: Long,
        request: suspend () -> Response<Any>
    ): ForwardingResult {
        var lastException: Exception? = null
        var currentDelay = initialDelay
        
        repeat(maxAttempts) { attempt ->
            try {
                val response = request()
                
                return if (response.isSuccessful) {
                    ForwardingResult.Success(
                        responseCode = response.code(),
                        responseBody = response.body()?.toString()
                    )
                } else {
                    ForwardingResult.HttpError(
                        responseCode = response.code(),
                        responseBody = response.errorBody()?.string(),
                        message = "HTTP ${response.code()}: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                lastException = e
                
                // Don't retry on the last attempt
                if (attempt < maxAttempts - 1) {
                    delay(currentDelay)
                    currentDelay *= RETRY_MULTIPLIER
                }
            }
        }
        
        return ForwardingResult.NetworkError(
            exception = lastException ?: Exception("Unknown network error"),
            message = "Failed after $maxAttempts attempts: ${lastException?.message}"
        )
    }
}

/**
 * Result of a forwarding attempt (SMS or notification).
 */
sealed class ForwardingResult {
    /**
     * Successful forwarding.
     */
    data class Success(
        val responseCode: Int,
        val responseBody: String?
    ) : ForwardingResult()
    
    /**
     * HTTP error (4xx, 5xx status codes).
     */
    data class HttpError(
        val responseCode: Int,
        val responseBody: String?,
        val message: String
    ) : ForwardingResult()
    
    /**
     * Network error (connection issues, timeouts, etc.).
     */
    data class NetworkError(
        val exception: Exception,
        val message: String
    ) : ForwardingResult()
} 