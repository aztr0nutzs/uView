package com.sentinel.companion.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sentinel.companion.data.model.DeviceProfile

@Database(
    entities = [DeviceProfile::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao

    companion object {
        const val DB_NAME = "sentinel_companion.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
