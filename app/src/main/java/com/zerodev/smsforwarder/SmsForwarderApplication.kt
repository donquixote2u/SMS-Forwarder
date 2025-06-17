package com.zerodev.smsforwarder

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for SMS Forwarder.
 * This class initializes Hilt dependency injection and WorkManager configuration.
 */
@HiltAndroidApp
class SmsForwarderApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    companion object {
        private const val TAG = "SmsForwarderApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SMS Forwarder application started")
    }
    
    /**
     * Provide WorkManager configuration with Hilt worker factory.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
} 