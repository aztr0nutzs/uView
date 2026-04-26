package com.sentinel.app.core.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sentinel.app.MainActivity
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.CameraEventType
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationHelper
 *
 * Creates notification channels and posts camera event notifications.
 *
 * Channels:
 *   CHANNEL_MOTION   — IMPORTANCE_HIGH — motion detection alerts (heads-up)
 *   CHANNEL_CONNECTION — IMPORTANCE_DEFAULT — connection lost/restored
 *   CHANNEL_GENERAL  — IMPORTANCE_LOW — snapshots, recordings (silent)
 *
 * Each notification deep-links back to the app. Phase 6 will extend this
 * to include an explicit deep-link to the specific camera detail screen.
 *
 * Note on POST_NOTIFICATIONS permission:
 *   On Android 13+ (API 33+), [android.permission.POST_NOTIFICATIONS] must
 *   be granted at runtime. This helper checks before posting. The UI layer
 *   (SettingsScreen) should prompt for this permission when notifications
 *   are toggled on.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_MOTION     = "sentinel_motion"
        const val CHANNEL_CONNECTION = "sentinel_connection"
        const val CHANNEL_GENERAL    = "sentinel_general"

        // Notification ID ranges — avoid collisions
        private const val MOTION_BASE     = 1000
        private const val CONNECTION_BASE = 2000
        private const val GENERAL_BASE    = 3000
    }

    /**
     * Create all notification channels. Call once on app startup.
     * Safe to call multiple times — Android deduplicates channels by ID.
     */
    fun createChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MOTION,
                "Motion Detection",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when motion is detected on a camera"
                enableVibration(true)
                setShowBadge(true)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CONNECTION,
                "Camera Connection",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Camera connection lost and restored alerts"
                enableVibration(false)
                setShowBadge(true)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GENERAL,
                "General Alerts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Snapshots, recordings, and other camera activity"
                setShowBadge(false)
            }
        )

        Timber.d("NotificationHelper: channels created")
    }

    /**
     * Post a notification for a [CameraEvent].
     * Chooses the appropriate channel based on event type.
     * Silently skips if POST_NOTIFICATIONS is not granted (API 33+).
     */
    @SuppressLint("MissingPermission")
    fun postEventNotification(event: CameraEvent) {
        if (!hasNotificationPermission()) return

        val (channelId, notifId, title, body) = buildNotificationContent(event)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("camera_id", event.cameraId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(
                when (channelId) {
                    CHANNEL_MOTION     -> NotificationCompat.PRIORITY_HIGH
                    CHANNEL_CONNECTION -> NotificationCompat.PRIORITY_DEFAULT
                    else               -> NotificationCompat.PRIORITY_LOW
                }
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
            Timber.d("NotificationHelper: posted ${event.eventType.name} for ${event.cameraName}")
        } catch (e: SecurityException) {
            Timber.w(e, "NotificationHelper: notification permission denied at post time")
        }
    }

    /**
     * Cancel all notifications for a specific camera.
     */
    fun cancelForCamera(cameraId: String) {
        val nm = NotificationManagerCompat.from(context)
        // Cancel using deterministic IDs derived from cameraId hash
        val hash = cameraId.hashCode()
        nm.cancel(MOTION_BASE     + (hash % 100).coerceAtLeast(0))
        nm.cancel(CONNECTION_BASE + (hash % 100).coerceAtLeast(0))
    }

    /**
     * Cancel all Sentinel notifications.
     */
    fun cancelAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private data class NotifContent(
        val channelId: String,
        val notifId: Int,
        val title: String,
        val body: String
    )

    private fun buildNotificationContent(event: CameraEvent): NotifContent {
        val hash = event.cameraId.hashCode().coerceAtLeast(0)
        return when (event.eventType) {
            CameraEventType.MOTION_DETECTED -> NotifContent(
                channelId = CHANNEL_MOTION,
                notifId   = MOTION_BASE + (hash % 100),
                title     = "Motion: ${event.cameraName}",
                body      = event.description.ifBlank { "Motion detected" }
            )
            CameraEventType.CONNECTION_LOST -> NotifContent(
                channelId = CHANNEL_CONNECTION,
                notifId   = CONNECTION_BASE + (hash % 100),
                title     = "Camera offline: ${event.cameraName}",
                body      = event.description.ifBlank { "Camera went offline" }
            )
            CameraEventType.CONNECTION_RESTORED -> NotifContent(
                channelId = CHANNEL_CONNECTION,
                notifId   = CONNECTION_BASE + (hash % 100),
                title     = "Camera back online: ${event.cameraName}",
                body      = "Stream restored"
            )
            CameraEventType.SNAPSHOT_TAKEN -> NotifContent(
                channelId = CHANNEL_GENERAL,
                notifId   = GENERAL_BASE + (hash % 100),
                title     = "Snapshot saved",
                body      = "${event.cameraName} — ${event.description}"
            )
            else -> NotifContent(
                channelId = CHANNEL_GENERAL,
                notifId   = GENERAL_BASE + (hash % 100),
                title     = event.eventType.displayName,
                body      = "${event.cameraName}: ${event.description}"
            )
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true  // Permission not required below API 33
        }
    }
}
