package com.sentinel.companion.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sentinel.companion.data.model.Alert
import com.sentinel.companion.data.model.DeviceProfile

@Database(
    entities = [DeviceProfile::class, Alert::class],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun alertDao(): AlertDao

    companion object {
        const val DB_NAME = "sentinel_companion.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cameras` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `room` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `streamUrl` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `password` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `latencyMs` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `lastSeenMs` INTEGER NOT NULL,
                        `addedMs` INTEGER NOT NULL,
                        `snapshotPath` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `alerts` (
                        `id` TEXT NOT NULL,
                        `cameraId` TEXT NOT NULL,
                        `cameraName` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `timestampMs` INTEGER NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        // The split-brain Camera/CameraDao path was removed — DeviceProfile is the
        // single source of truth. Drop the now-orphan `cameras` table; the
        // `alerts` table keeps its `cameraId`/`cameraName` columns (now holding
        // device id/name) so historical alert rows survive the migration.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `cameras`")
            }
        }
    }
}
