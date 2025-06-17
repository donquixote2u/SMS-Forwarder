package com.zerodev.smsforwarder.di

import android.content.Context
import androidx.room.Room
import com.zerodev.smsforwarder.data.local.database.SmsForwarderDatabase
import com.zerodev.smsforwarder.data.local.dao.RuleDao
import com.zerodev.smsforwarder.data.local.dao.HistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provide the SMS Forwarder database instance.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmsForwarderDatabase {
        return Room.databaseBuilder(
            context,
            SmsForwarderDatabase::class.java,
            SmsForwarderDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    /**
     * Provide the RuleDao from the database.
     */
    @Provides
    fun provideRuleDao(database: SmsForwarderDatabase): RuleDao {
        return database.ruleDao()
    }
    
    /**
     * Provide the HistoryDao from the database.
     */
    @Provides
    fun provideHistoryDao(database: SmsForwarderDatabase): HistoryDao {
        return database.historyDao()
    }
} 