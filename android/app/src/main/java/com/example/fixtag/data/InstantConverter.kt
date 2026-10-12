package com.example.fixtag.data

import androidx.room.TypeConverter
import kotlin.time.Instant

object InstantConverter {
    @TypeConverter
    fun toTimestamp(instant: Instant): Long {
        return instant.toEpochMilliseconds()
    }

    @TypeConverter
    fun fromTimestamp(value: Long): Instant {
        return Instant.fromEpochMilliseconds(value)
    }
}
