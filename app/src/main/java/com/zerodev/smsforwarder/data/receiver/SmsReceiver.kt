package com.zerodev.smsforwarder.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.zerodev.smsforwarder.domain.model.SmsMessage
import com.zerodev.smsforwarder.data.service.SmsForwardingService
import kotlinx.datetime.Clock

/**
 * BroadcastReceiver for intercepting incoming SMS messages.
 * This receiver handles SMS_RECEIVED broadcasts and forwards valid messages
 * to the SmsForwardingService for processing.
 */
class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "SMS received, action: ${intent?.action}")
        
        if (context == null || intent == null) {
            Log.w(TAG, "Context or intent is null")
            return
        }
        
        // Only process SMS_RECEIVED intents
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d(TAG, "Ignoring non-SMS intent: ${intent.action}")
            return
        }
        
        try {
            // Extract SMS messages from the intent
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            if (smsMessages.isNullOrEmpty()) {
                Log.w(TAG, "No SMS messages found in intent")
                return
            }
            
            // Process each SMS message
            for (smsMessage in smsMessages) {
                val sender = smsMessage.originatingAddress ?: "Unknown"
                val body = smsMessage.messageBody ?: ""
                val timestamp = Clock.System.now() // Use current time as fallback
                
                Log.d(TAG, "Processing SMS from: $sender, body length: ${body.length}")
                
                if (body.isNotBlank()) {
                    val domainSms = SmsMessage(
                        body = body,
                        sender = sender,
                        timestamp = timestamp
                    )
                    
                    // Start the foreground service to handle SMS forwarding
                    val serviceIntent = Intent(context, SmsForwardingService::class.java).apply {
                        action = SmsForwardingService.ACTION_PROCESS_SMS
                        putExtra(SmsForwardingService.EXTRA_SMS_BODY, body)
                        putExtra(SmsForwardingService.EXTRA_SMS_SENDER, sender)
                        putExtra(SmsForwardingService.EXTRA_SMS_TIMESTAMP, timestamp.epochSeconds)
                    }
                    
                    context.startForegroundService(serviceIntent)
                    Log.d(TAG, "Started SmsForwardingService for SMS processing")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS: ${e.message}", e)
        }
    }
} 