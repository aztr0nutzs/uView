package com.sentinel.companion.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sentinel.companion.data.model.Alert
import com.sentinel.companion.data.model.Camera
import com.sentinel.companion.data.model.DeviceProfile

@Database(
    entities = [DeviceProfile::class, Camera::class, Alert::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun cameraDao(): CameraDao
    abstract fun alertDao(): AlertDao

    companion object {
        const val DB_NAME = "sentinel_companion.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
