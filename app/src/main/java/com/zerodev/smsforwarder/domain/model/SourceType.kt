package com.zerodev.smsforwarder.domain.model

/**
 * Enum representing the source type for forwarding rules.
 * Determines whether a rule applies to SMS messages or app notifications.
 */
enum class SourceType {
    /**
     * Rule applies to incoming SMS messages.
     */
    SMS,
    
    /**
     * Rule applies to notifications from other apps.
     */
    NOTIFICATION
} 