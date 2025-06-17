package com.zerodev.smsforwarder.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zerodev.smsforwarder.R
import com.zerodev.smsforwarder.MainActivity
import com.zerodev.smsforwarder.domain.model.SmsMessage
import com.zerodev.smsforwarder.domain.usecase.SmsForwardingUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Foreground service for processing SMS forwarding.
 * This service runs in the foreground to ensure SMS processing continues
 * even when the app is in the background or the system is under memory pressure.
 */
@AndroidEntryPoint
class SmsForwardingService : Service() {
    
    companion object {
        private const val TAG = "SmsForwardingService"
        
        // Service actions
        const val ACTION_PROCESS_SMS = "com.zerodev.smsforwarder.PROCESS_SMS"
        const val ACTION_START_MONITORING = "com.zerodev.smsforwarder.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.zerodev.smsforwarder.STOP_MONITORING"
        
        // Intent extras
        const val EXTRA_SMS_BODY = "sms_body"
        const val EXTRA_SMS_SENDER = "sms_sender"
        const val EXTRA_SMS_TIMESTAMP = "sms_timestamp"
        
        // Notification constants
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sms_forwarding_channel"
        private const val CHANNEL_NAME = "SMS Forwarding"
        private const val CHANNEL_DESCRIPTION = "Notifications for SMS forwarding service"
    }
    
    @Inject
    lateinit var smsForwardingUseCase: SmsForwardingUseCase
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isMonitoring = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_PROCESS_SMS -> {
                handleProcessSms(intent)
            }
            ACTION_START_MONITORING -> {
                startMonitoring()
            }
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
            }
        }
        
        // Return START_STICKY to ensure the service is restarted if killed
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // This is a started service, not a bound service
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        serviceScope.cancel()
    }
    
    /**
     * Handle SMS processing intent.
     */
    private fun handleProcessSms(intent: Intent) {
        val smsBody = intent.getStringExtra(EXTRA_SMS_BODY)
        val smsSender = intent.getStringExtra(EXTRA_SMS_SENDER)
        val smsTimestamp = intent.getLongExtra(EXTRA_SMS_TIMESTAMP, 0L)
        
        if (smsBody.isNullOrBlank() || smsSender.isNullOrBlank()) {
            Log.w(TAG, "Invalid SMS data received")
            return
        }
        
        val smsMessage = SmsMessage(
            body = smsBody,
            sender = smsSender,
            timestamp = Instant.fromEpochSeconds(smsTimestamp)
        )
        
        Log.d(TAG, "Processing SMS: ${smsMessage.getTruncatedBody(50)}")
        
        // Start foreground service for SMS processing
        startForeground(NOTIFICATION_ID, createProcessingNotification())
        
        serviceScope.launch {
            try {
                val results = smsForwardingUseCase.processSms(smsMessage)
                Log.d(TAG, "SMS processing completed. ${results.size} forwarding attempts made")
                
                // Update notification with results
                val notification = createCompletedNotification(results.size)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.notify(NOTIFICATION_ID, notification)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
                
                // Update notification with error
                val notification = createErrorNotification()
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.notify(NOTIFICATION_ID, notification)
                
            } finally {
                // Stop foreground service after processing
                if (!isMonitoring) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }
    
    /**
     * Start SMS monitoring mode.
     */
    private fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        Log.d(TAG, "Starting SMS monitoring")
        
        // Start foreground service for monitoring
        startForeground(NOTIFICATION_ID, createMonitoringNotification())
    }
    
    /**
     * Stop SMS monitoring mode.
     */
    private fun stopMonitoring() {
        if (!isMonitoring) return
        
        isMonitoring = false
        Log.d(TAG, "Stopping SMS monitoring")
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    /**
     * Create notification channel for Android O and above.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_DESCRIPTION
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }
    
    /**
     * Create notification for SMS processing.
     */
    private fun createProcessingNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Processing SMS")
            .setContentText("Forwarding SMS to configured endpoints...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    /**
     * Create notification for SMS monitoring.
     */
    private fun createMonitoringNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Forwarder Active")
            .setContentText("Monitoring SMS messages for forwarding")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    /**
     * Create notification for completed SMS processing.
     */
    private fun createCompletedNotification(forwardingCount: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val text = if (forwardingCount > 0) {
            "SMS forwarded to $forwardingCount endpoint${if (forwardingCount > 1) "s" else ""}"
        } else {
            "SMS processed (no matching rules)"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Processing Complete")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(isMonitoring)
            .setSilent(true)
            .build()
    }
    
    /**
     * Create notification for processing errors.
     */
    private fun createErrorNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Processing Error")
            .setContentText("Failed to process SMS. Check app for details.")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(isMonitoring)
            .setSilent(true)
            .build()
    }
} 