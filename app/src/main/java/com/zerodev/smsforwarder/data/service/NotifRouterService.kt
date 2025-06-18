package com.zerodev.smsforwarder.data.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.gson.Gson
import com.zerodev.smsforwarder.domain.usecase.SmsForwardingUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

/**
 * NotificationListenerService implementation that captures notifications from other apps
 * and forwards them based on configured rules.
 * 
 * This service extends Android's NotificationListenerService to intercept notifications
 * and extract relevant data for pattern matching and forwarding.
 */
@AndroidEntryPoint
class NotifRouterService : NotificationListenerService() {
    
    companion object {
        private const val TAG = "NotifRouterService"
        
        /**
         * Check if notification listener permission is granted.
         * 
         * @param context Application context
         * @return true if permission is granted, false otherwise
         */
        fun isNotificationAccessGranted(context: Context): Boolean {
            val packageName = context.packageName
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return flat?.contains(packageName) == true
        }
        
        /**
         * Open notification access settings screen.
         * 
         * @param context Application context
         */
        fun openNotificationAccessSettings(context: Context) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        
        /**
         * Check if the service is enabled in system settings.
         * 
         * @param context Application context
         * @return true if service is enabled, false otherwise
         */
        fun isServiceEnabled(context: Context): Boolean {
            val cn = ComponentName(context, NotifRouterService::class.java)
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return flat?.contains(cn.flattenToString()) == true
        }
    }
    
    @Inject
    lateinit var forwardingUseCase: SmsForwardingUseCase
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔔 NotifRouterService created")
        Log.d(TAG, "🔔 Service package: $packageName")
        Log.d(TAG, "🔔 Service enabled: ${isServiceEnabled(this)}")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🔔 NotifRouterService destroyed")
    }
    
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "🔔 Notification Listener connected - ready to receive notifications")
    }
    
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "🔔 Notification Listener disconnected")
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        sbn?.let { notification ->
            serviceScope.launch {
                try {
                    processNotification(notification)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing notification", e)
                }
            }
        }
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // We don't need to do anything when notifications are removed
    }
    
    /**
     * Process a received notification and forward it if it matches any rules.
     * 
     * @param sbn StatusBarNotification to process
     */
    private suspend fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification
        
        // Skip our own notifications to avoid loops
        if (packageName == this.packageName) {
            return
        }
        
        // Skip system notifications that are not user-facing
        if (isSystemNotification(sbn)) {
            Log.d(TAG, "📱 Skipping system notification from $packageName")
            return
        }
        
        // Extract notification data
        val notificationData = extractNotificationData(sbn)
        
        Log.d(TAG, "🔔 Received notification from ${notificationData.appLabel} ($packageName)")
        Log.d(TAG, "   Title: ${notificationData.title}")
        Log.d(TAG, "   Text: ${notificationData.text}")
        Log.d(TAG, "   PostTime: ${notificationData.postTime}")
        
        // Forward the notification using the forwarding use case
        forwardingUseCase.processNotification(
            packageName = packageName,
            appLabel = notificationData.appLabel,
            title = notificationData.title,
            text = notificationData.text,
            postTime = notificationData.postTime,
            extras = notificationData.extras
        )
    }
    
    /**
     * Check if this is a system notification that should be ignored.
     * 
     * @param sbn StatusBarNotification to check
     * @return true if this is a system notification, false otherwise
     */
    private fun isSystemNotification(sbn: StatusBarNotification): Boolean {
        val packageName = sbn.packageName
        val notification = sbn.notification
        
        // Skip system packages
        if (packageName.startsWith("android") || 
            packageName.startsWith("com.android.systemui") ||
            packageName == "com.google.android.gms") {
            return true
        }
        
        // Skip ongoing notifications (like music players, downloads)
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) {
            return true
        }
        
        // Skip notifications without content
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        
        if (title.isNullOrBlank() && text.isNullOrBlank()) {
            return true
        }
        
        return false
    }
    
    /**
     * Extract notification data from StatusBarNotification.
     * 
     * @param sbn StatusBarNotification to extract data from
     * @return NotificationData containing extracted information
     */
    private fun extractNotificationData(sbn: StatusBarNotification): NotificationData {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras ?: Bundle()
        val postTime = sbn.postTime
        
        // Get app label
        val appLabel = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName.split(".").lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
        }
        
        // Extract notification content
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
        
        // Extract JSON-serializable extras
        val serializableExtras = mutableMapOf<String, Any?>()
        for (key in extras.keySet()) {
            try {
                when {
                    extras.containsKey(key) -> {
                        // Use type-safe getters instead of deprecated get()
                        val value = when {
                            extras.getString(key) != null -> extras.getString(key)
                            extras.getCharSequence(key) != null -> extras.getCharSequence(key)?.toString()
                            else -> {
                                // Try primitive types with try-catch for better detection
                                try {
                                    when {
                                        extras.getInt(key, Int.MIN_VALUE) != Int.MIN_VALUE -> extras.getInt(key)
                                        extras.getLong(key, Long.MIN_VALUE) != Long.MIN_VALUE -> extras.getLong(key)
                                        extras.getDouble(key, Double.NaN).let { !it.isNaN() } -> extras.getDouble(key)
                                        extras.getFloat(key, Float.NaN).let { !it.isNaN() } -> extras.getFloat(key)
                                        else -> {
                                            // For boolean, we need to check if the key actually contains a boolean
                                            try {
                                                extras.getBoolean(key)
                                            } catch (e: ClassCastException) {
                                                null
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                        value?.let { serializableExtras[key] = it }
                    }
                }
            } catch (e: Exception) {
                // Skip problematic extras
            }
        }
        
        return NotificationData(
            packageName = packageName,
            appLabel = appLabel,
            title = title,
            text = bigText ?: text,
            postTime = postTime,
            extras = serializableExtras
        )
    }
    
    /**
     * Data class to hold extracted notification information.
     * 
     * @property packageName Package name of the app that posted the notification
     * @property appLabel Human-readable app name
     * @property title Notification title
     * @property text Notification text content
     * @property postTime Timestamp when notification was posted
     * @property extras JSON-serializable extras from the notification
     */
    private data class NotificationData(
        val packageName: String,
        val appLabel: String,
        val title: String,
        val text: String,
        val postTime: Long,
        val extras: Map<String, Any?>
    )
} 