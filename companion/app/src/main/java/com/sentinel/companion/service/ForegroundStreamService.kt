package com.sentinel.companion.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sentinel.companion.MainActivity
import com.sentinel.companion.R
import com.sentinel.companion.data.model.DeviceState
import com.sentinel.companion.data.repository.DeviceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundStreamService : Service() {

    @Inject lateinit var repo: DeviceRepository

    private val scope     = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("SENTINEL COMPANION", "Monitoring active streams"))
        Timber.d("ForegroundStreamService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START  -> startMonitoring()
            ACTION_STOP   -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        Timber.d("ForegroundStreamService destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Monitoring loop ───────────────────────────────────────────────────────

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            Timber.d("Device monitoring loop started")
            while (true) {
                try {
                    val devices = repo.devices.first().filter { it.isEnabled }
                    Timber.d("Probing ${devices.size} enabled devices")

                    devices.forEach { device ->
                        launch {
                            val reachable = isPortOpen(device.host, device.port)
                            val newState  = if (reachable) DeviceState.ONLINE else DeviceState.OFFLINE
                            val currentState = DeviceState.entries.firstOrNull { it.name == device.state }
                                ?: DeviceState.UNKNOWN

                            if (newState != currentState) {
                                Timber.d("${device.name}: $currentState → $newState")
                                repo.updateState(
                                    id        = device.id,
                                    state     = newState,
                                    latencyMs = if (reachable) measureLatency(device.host, device.port) else 0,
                                )
                            }

                            if (!reachable && currentState == DeviceState.ONLINE) {
                                // Back-off before marking truly offline
                                delay(RECONNECT_DELAY_MS)
                                val retry = isPortOpen(device.host, device.port)
                                if (!retry) {
                                    repo.updateState(device.id, DeviceState.OFFLINE)
                                    notifyDeviceOffline(device.name)
                                } else {
                                    repo.updateState(device.id, DeviceState.ONLINE, measureLatency(device.host, device.port))
                                }
                            }
                        }
                    }

                    val onlineCount = devices.count { it.state == DeviceState.ONLINE.name }
                    updateNotification("$onlineCount/${devices.size} device(s) online")

                } catch (e: Exception) {
                    Timber.w(e, "Monitor loop error")
                }

                delay(POLL_INTERVAL_MS)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int = 1500): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            true
        }
    } catch (_: Exception) { false }

    private fun measureLatency(host: String, port: Int): Int {
        val start = System.currentTimeMillis()
        return if (isPortOpen(host, port, 1000)) {
            (System.currentTimeMillis() - start).toInt().coerceAtMost(999)
        } else 0
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Stream Monitor",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps camera streams monitored in the background"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, text: String): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notif = buildNotification("SENTINEL COMPANION", text)
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, notif)
    }

    private fun notifyDeviceOffline(deviceName: String) {
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Device Offline")
            .setContentText("$deviceName lost connection")
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ALERT_BASE + deviceName.hashCode(), notif)
    }

    companion object {
        const val CHANNEL_ID            = "sentinel_stream_monitor"
        const val NOTIF_ID              = 1001
        const val NOTIF_ALERT_BASE      = 2000
        const val ACTION_START          = "com.sentinel.companion.START_MONITOR"
        const val ACTION_STOP           = "com.sentinel.companion.STOP_MONITOR"
        const val POLL_INTERVAL_MS      = 30_000L
        const val RECONNECT_DELAY_MS    = 5_000L

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, ForegroundStreamService::class.java).apply {
                    action = ACTION_START
                }
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ForegroundStreamService::class.java).apply {
                    action = ACTION_STOP
                }
            )
        }
    }
}
