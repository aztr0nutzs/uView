package com.sentinel.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.sentinel.app.data.local.dao.CameraDao
import com.sentinel.app.data.local.dao.CameraEventDao
import com.sentinel.app.data.local.entities.CameraEntity
import com.sentinel.app.data.local.entities.CameraEventEntity
import com.sentinel.app.domain.model.CameraEventType
import com.sentinel.app.domain.model.CameraSourceType
import com.sentinel.app.domain.model.CameraStatus
import com.sentinel.app.domain.model.StreamQualityProfile
import com.sentinel.app.domain.model.StreamTransport

@Database(
    entities = [CameraEntity::class, CameraEventEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(SentinelTypeConverters::class)
abstract class SentinelDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    abstract fun cameraEventDao(): CameraEventDao
}

// ─────────────────────────────────────────────────────────────────────────────
// TypeConverters for enum persistence
// ─────────────────────────────────────────────────────────────────────────────

class SentinelTypeConverters {

    @TypeConverter
    fun fromCameraSourceType(value: CameraSourceType): String = value.name

    @TypeConverter
    fun toCameraSourceType(value: String): CameraSourceType =
        CameraSourceType.valueOf(value)

    @TypeConverter
    fun fromCameraStatus(value: CameraStatus): String = value.name

    @TypeConverter
    fun toCameraStatus(value: String): CameraStatus =
        CameraStatus.valueOf(value)

    @TypeConverter
    fun fromCameraEventType(value: CameraEventType): String = value.name

    @TypeConverter
    fun toCameraEventType(value: String): CameraEventType =
        CameraEventType.valueOf(value)

    @TypeConverter
    fun fromStreamTransport(value: StreamTransport): String = value.name

    @TypeConverter
    fun toStreamTransport(value: String): StreamTransport =
        StreamTransport.valueOf(value)

    @TypeConverter
    fun fromStreamQualityProfile(value: StreamQualityProfile): String = value.name

    @TypeConverter
    fun toStreamQualityProfile(value: String): StreamQualityProfile =
        StreamQualityProfile.valueOf(value)
}
