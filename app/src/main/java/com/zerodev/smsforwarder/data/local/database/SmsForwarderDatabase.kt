package com.zerodev.smsforwarder.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.zerodev.smsforwarder.data.local.converter.Converters
import com.zerodev.smsforwarder.data.local.dao.RuleDao
import com.zerodev.smsforwarder.data.local.dao.HistoryDao
import com.zerodev.smsforwarder.data.local.entity.RuleEntity
import com.zerodev.smsforwarder.data.local.entity.HistoryEntity

/**
 * Room database for SMS Forwarder app.
 * Contains rules and history tables with their respective DAOs.
 */
@Database(
    entities = [RuleEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SmsForwarderDatabase : RoomDatabase() {
    
    /**
     * DAO for rules table operations.
     */
    abstract fun ruleDao(): RuleDao
    
    /**
     * DAO for history table operations.
     */
    abstract fun historyDao(): HistoryDao
    
    companion object {
        /**
         * Database name.
         */
        const val DATABASE_NAME = "sms_forwarder_database"
        
        /**
         * Singleton instance of the database.
         */
        @Volatile
        private var INSTANCE: SmsForwarderDatabase? = null
        
        /**
         * Get the database instance.
         * Creates the database if it doesn't exist.
         * 
         * @param context Application context
         * @return Database instance
         */
        fun getDatabase(context: Context): SmsForwarderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmsForwarderDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 