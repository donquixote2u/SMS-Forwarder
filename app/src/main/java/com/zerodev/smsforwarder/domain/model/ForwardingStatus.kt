package com.zerodev.smsforwarder.domain.model

/**
 * Enum representing the status of a forwarding attempt (SMS or notification).
 */
enum class ForwardingStatus {
    /**
     * Successfully forwarded to the endpoint.
     */
    SUCCESS,
    
    /**
     * Failed to forward due to network or server error.
     */
    FAILED,
    
    /**
     * Currently being retried after a failure.
     */
    RETRY,
    
    /**
     * No rules matched the message/notification content.
     */
    NO_RULE_MATCHED,
    
    /**
     * Message/notification was received but not yet processed.
     */
    RECEIVED
} 