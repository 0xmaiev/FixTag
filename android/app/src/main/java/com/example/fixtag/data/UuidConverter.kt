package com.example.fixtag.data

import androidx.room.TypeConverter
import kotlin.uuid.Uuid

object UuidConverter {
    @TypeConverter
    fun toByteArray(uuid: Uuid): ByteArray {
        return uuid.toByteArray()
    }

    @TypeConverter
    fun fromByteArray(byteArray: ByteArray): Uuid {
        return Uuid.fromByteArray(byteArray)
    }
}
