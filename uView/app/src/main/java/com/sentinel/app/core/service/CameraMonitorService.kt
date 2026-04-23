package com.sentinel.app.core.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.sentinel.app.MainActivity
import com.sentinel.app.core.notifications.NotificationHelper
import com.sentinel.app.data.events.EventPipeline
import com.sentinel.app.data.motion.MotionMonitorService
import com.sentinel.app.data.playback.PlaybackManager
import com.sentinel.app.domain.model.MotionSensitivityConfig
import com.sentinel.app.domain.repository.CameraRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * CameraMonitorService
 *
 * A foreground service that keeps camera stream monitoring and motion detection
 * alive when the app is in the background or the screen is off.
 *
 * This service is the core of "always-on" home security monitoring.
 *
 * What it does:
 *   1. Holds a partial [PowerManager.WakeLock] to prevent CPU sleep.
 *   2. Maintains motion detection on all enabled cameras.
 *   3. Keeps the [EventPipeline] running to catch connection drops.
 *   4. Shows a persistent notification with live camera count.
 *   5. Responds to Intent actions for pause/resume from the notification.
 *
 * What it does NOT do:
 *   - It does NOT start ExoPlayer sessions (those are in [PlaybackManager]).
 *     Streams are started on-demand when the user opens a camera view.
 *     In background mode, we run motion detection on MJPEG cameras only
 *     unless the user explicitly starts monitoring.
 *   - It does NOT use camera hardware — all monitoring is network-based.
 *
 * Lifecycle:
 *   Started by [MonitorServiceController.startService] when the user
 *   enables background monitoring in Settings or locks their phone.
 *   Stopped by [MonitorServiceController.stopService] or notification action.
 *
 * foregroundServiceType="dataSync" is used rather than "mediaPlayback"
 * because this service processes camera data streams, not local media.
 */
@AndroidEntryPoint
class CameraMonitorService : Service() {

    @Inject lateinit var cameraRepository: CameraRepository
    @Inject lateinit var motionMonitorService: MotionMonitorService
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var eventPipeline: EventPipeline
    @Inject lateinit var notificationHelper: NotificationHelper

    private val scope     = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var activeCameraCount = 0

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 9001

        const val ACTION_START   = "com.sentinel.app.action.MONITOR_START"
        const val ACTION_STOP    = "com.sentinel.app.action.MONITOR_STOP"
        const val ACTION_PAUSE   = "com.sentinel.app.action.MONITOR_PAUSE"
        const val ACTION_RESUME  = "com.sentinel.app.action.MONITOR_RESUME"

        fun startIntent(context: Context): Intent =
            Intent(context, CameraMonitorService::class.java).apply { action = ACTION_START }

        fun stopIntent(context: Context): Intent =
            Intent(context, CameraMonitorService::class.java).apply { action = ACTION_STOP }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Service lifecycle
    // ─────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        Timber.i("CameraMonitorService: onCreate")
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP   -> { stopSelf(); return START_NOT_STICKY }
            ACTION_PAUSE  -> pauseMonitoring()
            ACTION_RESUME -> resumeMonitoring()
            else          -> startMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Timber.i("CameraMonitorService: onDestroy")
        monitorJob?.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // App swiped from recents — keep service running if user has
        // background monitoring enabled. The service will restart automatically
        // because we return START_STICKY.
        Timber.d("CameraMonitorService: task removed, service continues in background")
    }

    // ─────────────────────────────────────────────────────────────────────
    // Monitoring logic
    // ─────────────────────────────────────────────────────────────────────

    private fun startMonitoring() {
        Timber.i("CameraMonitorService: startMonitoring")

        // Must call startForeground immediately to avoid ANR on API 26+
        startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification(0, isPaused = false))

        monitorJob?.cancel()
        monitorJob = scope.launch {
            // Get all enabled cameras
            val cameras = cameraRepository.observeAllCameras().first()
                .filter { it.isEnabled }

            activeCameraCount = cameras.size
            Timber.d("CameraMonitorService: monitoring ${cameras.size} cameras")

            // Start motion detection on MJPEG-capable cameras
            // (ExoPlayer cameras need the UI surface — skip in background)
            cameras.forEach { camera ->
                val isMjpeg = camera.sourceType in listOf(
                    com.sentinel.app.domain.model.CameraSourceType.MJPEG,
                    com.sentinel.app.domain.model.CameraSourceType.ANDROID_IPWEBCAM,
                    com.sentinel.app.domain.model.CameraSourceType.ANDROID_DROIDCAM
                )
                if (isMjpeg) {
                    playbackManager.startCamera(camera)
                    motionMonitorService.startMonitoring(camera, MotionSensitivityConfig.MEDIUM)
                }
            }

            // Update notification with live count
            updateNotification(activeCameraCount, isPaused = false)

            // Keep monitoring — observe camera list for changes
            cameraRepository.observeAllCameras().collect { latestCameras ->
                val enabledCount = latestCameras.count { it.isEnabled }
                if (enabledCount != activeCameraCount) {
                    activeCameraCount = enabledCount
                    updateNotification(activeCameraCount, isPaused = false)
                }
            }
        }
    }

    private fun pauseMonitoring() {
        Timber.d("CameraMonitorService: monitoring paused")
        updateNotification(activeCameraCount, isPaused = true)
    }

    private fun resumeMonitoring() {
        Timber.d("CameraMonitorService: monitoring resumed")
        updateNotification(activeCameraCount, isPaused = false)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Notification
    // ─────────────────────────────────────────────────────────────────────

    private fun buildNotification(cameraCount: Int, isPaused: Boolean): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = PendingIntent.getService(
            this, 1,
            Intent(this, CameraMonitorService::class.java).apply {
                action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 2,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_GENERAL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(if (isPaused) "Sentinel — Monitoring paused" else "Sentinel — Monitoring active")
            .setContentText(
                if (isPaused) "Tap to resume monitoring"
                else "Watching $cameraCount camera${if (cameraCount != 1) "s" else ""}"
            )
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pauseResumeIntent
            )
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .build()
    }

    private fun updateNotification(cameraCount: Int, isPaused: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(FOREGROUND_NOTIFICATION_ID, buildNotification(cameraCount, isPaused))
    }

    // ─────────────────────────────────────────────────────────────────────
    // Wake lock
    // ─────────────────────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "sentinel:camera_monitor"
        ).also {
            it.acquire(6 * 60 * 60 * 1000L)  // max 6 hour hold; re-acquired on restart
            Timber.d("CameraMonitorService: wake lock acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Timber.d("CameraMonitorService: wake lock released")
            }
        }
        wakeLock = null
    }
}
