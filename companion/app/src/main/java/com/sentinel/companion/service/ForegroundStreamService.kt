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
import com.sentinel.companion.data.db.AlertDao
import com.sentinel.companion.data.model.Alert
import com.sentinel.companion.data.model.AlertType
import com.sentinel.companion.data.model.DeviceProfile
import com.sentinel.companion.data.model.DeviceState
import com.sentinel.companion.data.model.StreamProtocol
import com.sentinel.companion.data.network.EndpointTestResult
import com.sentinel.companion.data.network.StreamEndpointTester
import com.sentinel.companion.data.repository.DeviceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundStreamService : Service() {

    @Inject lateinit var repo: DeviceRepository
    @Inject lateinit var endpointTester: StreamEndpointTester
    @Inject lateinit var alertDao: AlertDao

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
            ACTION_STOP   -> {
                stopMonitoring()
                return START_NOT_STICKY
            }
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

                    val results = coroutineScope {
                        devices.map { device ->
                            async { probeWithOfflineConfirmation(device) }
                        }.awaitAll()
                    }

                    val nowMs = System.currentTimeMillis()
                    results.forEach { result ->
                        val currentState = result.device.stateEnum()
                        if (result.newState != currentState || result.latencyMs != result.device.latencyMs) {
                            Timber.d(
                                "${result.device.name}: $currentState -> ${result.newState} (${result.detail})"
                            )
                            repo.updateState(result.device.id, result.newState, result.latencyMs)
                        }

                        if (shouldEmitConnectionAlert(currentState, result.newState)) {
                            emitAlert(result.device, result.newState, result.detail, nowMs)
                            if (result.newState == DeviceState.OFFLINE) {
                                notifyDeviceOffline(result.device.name)
                            }
                        }
                    }

                    val onlineCount = results.count { it.newState == DeviceState.ONLINE }
                    updateNotification("$onlineCount/${devices.size} device(s) online")

                } catch (e: Exception) {
                    Timber.w(e, "Monitor loop error")
                }

                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private data class ProbeOutcome(
        val device: DeviceProfile,
        val newState: DeviceState,
        val latencyMs: Int,
        val detail: String,
    )

    private suspend fun probeWithOfflineConfirmation(device: DeviceProfile): ProbeOutcome {
        val first = probeDevice(device)
        if (device.stateEnum() != DeviceState.ONLINE || first.newState != DeviceState.OFFLINE) {
            return first
        }

        delay(RECONNECT_DELAY_MS)
        return probeDevice(device)
    }

    private suspend fun probeDevice(device: DeviceProfile): ProbeOutcome {
        val started = System.nanoTime()
        val protocol = device.protocolEnum()
        val result = endpointTester.test(
            protocol = protocol,
            host = device.host,
            port = device.port,
            path = device.path,
            authType = device.authTypeEnum(),
            username = device.username,
            password = device.password,
        )

        val latencyMs = if (result is EndpointTestResult.Ok) {
            elapsedMs(started)
        } else {
            0
        }

        return when (result) {
            is EndpointTestResult.Ok -> ProbeOutcome(
                device = device,
                newState = DeviceState.ONLINE,
                latencyMs = latencyMs,
                detail = result.verifiedSignal,
            )
            is EndpointTestResult.Unsupported -> probeTcpFallback(device, protocol, result.message)
            else -> ProbeOutcome(
                device = device,
                newState = DeviceState.OFFLINE,
                latencyMs = 0,
                detail = result.message,
            )
        }
    }

    private fun probeTcpFallback(
        device: DeviceProfile,
        protocol: StreamProtocol,
        unsupportedReason: String,
    ): ProbeOutcome {
        val started = System.nanoTime()
        val open = isPortOpen(device.host, device.port)
        val state = if (open) DeviceState.ONLINE else DeviceState.OFFLINE
        val latency = if (open) elapsedMs(started) else 0
        val detail = if (open) {
            "TCP fallback for ${protocol.label}: ${device.host}:${device.port} open; $unsupportedReason"
        } else {
            "TCP fallback for ${protocol.label}: ${device.host}:${device.port} unreachable; $unsupportedReason"
        }
        return ProbeOutcome(device, state, latency, detail)
    }

    private fun isPortOpen(host: String, port: Int, timeoutMs: Int = 1500): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            true
        }
    } catch (_: Exception) {
        false
    }

    private fun elapsedMs(startedNs: Long): Int =
        ((System.nanoTime() - startedNs) / 1_000_000).toInt().coerceAtLeast(1).coerceAtMost(999)

    private fun shouldEmitConnectionAlert(previous: DeviceState, next: DeviceState): Boolean =
        (previous == DeviceState.ONLINE && next == DeviceState.OFFLINE) ||
            (previous == DeviceState.OFFLINE && next == DeviceState.ONLINE)

    private suspend fun emitAlert(
        device: DeviceProfile,
        newState: DeviceState,
        detail: String,
        nowMs: Long,
    ) {
        val (type, message) = when (newState) {
            DeviceState.ONLINE -> AlertType.CONNECTION_RESTORED to "${device.name} came back online ($detail)"
            DeviceState.OFFLINE -> AlertType.CONNECTION_LOST to "${device.name} unreachable ($detail)"
            else -> return
        }
        alertDao.insert(
            Alert(
                id = UUID.randomUUID().toString(),
                cameraId = device.id,
                cameraName = device.name,
                type = type.name,
                message = message,
                timestampMs = nowMs,
                isRead = false,
            )
        )
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
            context.stopService(Intent(context, ForegroundStreamService::class.java))
        }
    }
}
