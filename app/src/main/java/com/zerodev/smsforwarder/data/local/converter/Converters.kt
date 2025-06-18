package com.zerodev.smsforwarder.data.local.converter

import androidx.room.TypeConverter
import com.zerodev.smsforwarder.domain.model.ForwardingStatus
import kotlinx.datetime.Instant

/**
 * Room type converters for handling complex data types.
 */
class Converters {
    
    /**
     * Convert Instant to Long for database storage (milliseconds).
     */
    @TypeConverter
    fun fromInstant(instant: Instant): Long {
        return instant.toEpochMilliseconds()
    }
    
    /**
     * Convert Long to Instant from database (milliseconds).
     */
    @TypeConverter
    fun toInstant(epochMilliseconds: Long): Instant {
        return Instant.fromEpochMilliseconds(epochMilliseconds)
    }
    
    /**
     * Convert ForwardingStatus enum to String for database storage.
     */
    @TypeConverter
    fun fromForwardingStatus(status: ForwardingStatus): String {
        return status.name
    }
    
    /**
     * Convert String to ForwardingStatus enum from database.
     */
    @TypeConverter
    fun toForwardingStatus(statusName: String): ForwardingStatus {
        return ForwardingStatus.valueOf(statusName)
    }
} 