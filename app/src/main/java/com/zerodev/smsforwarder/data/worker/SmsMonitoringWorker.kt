package com.zerodev.smsforwarder.data.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zerodev.smsforwarder.data.repository.RuleRepository
import com.zerodev.smsforwarder.data.service.SmsForwardingService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker for managing SMS monitoring service.
 * This worker ensures the SMS monitoring service starts and stays active
 * when there are active rules configured.
 */
@HiltWorker
class SmsMonitoringWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val ruleRepository: RuleRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "SmsMonitoringWorker"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting SMS monitoring worker")
        
        return try {
            // Check if there are any active rules
            val hasActiveRules = ruleRepository.hasActiveRules()
            
            if (hasActiveRules) {
                Log.d(TAG, "Active rules found, starting SMS monitoring service")
                
                // Start the SMS monitoring service
                val serviceIntent = Intent(applicationContext, SmsForwardingService::class.java).apply {
                    action = SmsForwardingService.ACTION_START_MONITORING
                }
                
                applicationContext.startForegroundService(serviceIntent)
                
                Result.success()
            } else {
                Log.d(TAG, "No active rules found, SMS monitoring not needed")
                
                // Stop the SMS monitoring service if running
                val serviceIntent = Intent(applicationContext, SmsForwardingService::class.java).apply {
                    action = SmsForwardingService.ACTION_STOP_MONITORING
                }
                
                applicationContext.startService(serviceIntent)
                
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in SMS monitoring worker", e)
            Result.failure()
        }
    }
}