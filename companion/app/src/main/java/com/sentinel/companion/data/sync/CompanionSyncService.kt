package com.sentinel.companion.data.sync

import com.sentinel.companion.data.db.AlertDao
import com.sentinel.companion.data.db.DeviceDao
import com.sentinel.companion.data.model.Alert
import com.sentinel.companion.data.model.AlertType
import com.sentinel.companion.data.model.DeviceProfile
import com.sentinel.companion.data.model.DeviceState
import com.sentinel.companion.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source of truth for what "online" means: a real per-device reachability probe.
 *
 * Reads/writes go through [DeviceDao] (the same table SetupViewModel writes to)
 * so a device added via the setup wizard is immediately picked up by sync, the
 * dashboard, and the foreground monitor without any bridging layer.
 *
 * The companion app does not yet have a remote Sentinel Hub API contract — until
 * it does, we honestly do not pretend to receive a curated event stream from a
 * server. Instead we sync local DB state by probing each enabled device's
 * host:port and (for HTTP-style URLs) the stream URL itself, then write back
 * ONLINE/OFFLINE plus a measured latency. Status transitions emit real Alert
 * rows so the alerts feed is grounded in observed events instead of fixtures.
 */
data class SyncOutcome(
    val ok: Boolean,
    val checkedCount: Int,
    val onlineCount: Int,
    val offlineCount: Int,
    val transitions: Int,
    val finishedAtMs: Long,
    val error: String? = null,
)

enum class SyncPhase { IDLE, RUNNING, OK, FAILED }

data class SyncState(
    val phase: SyncPhase = SyncPhase.IDLE,
    val lastOutcome: SyncOutcome? = null,
)

@Singleton
class CompanionSyncService @Inject constructor(
    private val deviceDao: DeviceDao,
    private val alertDao: AlertDao,
    private val prefsRepo: PreferencesRepository,
) {
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()

    /** Run one sync pass; safe to call concurrently — second invocation no-ops. */
    suspend fun syncOnce(): SyncOutcome {
        if (_state.value.phase == SyncPhase.RUNNING) {
            return _state.value.lastOutcome ?: SyncOutcome(false, 0, 0, 0, 0, System.currentTimeMillis(), "Already running")
        }
        _state.value = _state.value.copy(phase = SyncPhase.RUNNING)

        // Local-only mode means "the user opted out of network probing" — be honest
        // about that instead of marking everything OFFLINE on principle.
        val appPrefs = prefsRepo.appPrefs.first()
        if (appPrefs.localOnlyMode) {
            val outcome = SyncOutcome(
                ok = true, checkedCount = 0, onlineCount = 0, offlineCount = 0,
                transitions = 0, finishedAtMs = System.currentTimeMillis(),
                error = "LOCAL_ONLY_MODE — network sync skipped",
            )
            _state.value = SyncState(SyncPhase.OK, outcome)
            return outcome
        }

        val devices = deviceDao.snapshotEnabled()
        if (devices.isEmpty()) {
            val outcome = SyncOutcome(
                ok = true, checkedCount = 0, onlineCount = 0, offlineCount = 0,
                transitions = 0, finishedAtMs = System.currentTimeMillis(),
                error = null,
            )
            _state.value = SyncState(SyncPhase.OK, outcome)
            return outcome
        }

        val probed = try {
            withContext(Dispatchers.IO) {
                coroutineScope {
                    devices.map { device ->
                        async { device to probe(device) }
                    }.awaitAll()
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Sync pass failed")
            val outcome = SyncOutcome(
                ok = false, checkedCount = 0, onlineCount = 0, offlineCount = 0,
                transitions = 0, finishedAtMs = System.currentTimeMillis(),
                error = "Sync error: ${e.javaClass.simpleName}: ${e.message ?: "unknown"}",
            )
            _state.value = SyncState(SyncPhase.FAILED, outcome)
            return outcome
        }

        val nowMs = System.currentTimeMillis()
        var online = 0
        var offline = 0
        var transitions = 0

        for ((device, probeResult) in probed) {
            val newState = if (probeResult.reachable) DeviceState.ONLINE else DeviceState.OFFLINE
            val prevState = device.stateEnum()
            if (newState == DeviceState.ONLINE) online++ else offline++

            deviceDao.updateState(device.id, newState.name, probeResult.latencyMs, nowMs)

            // Real alert generation: only on observed transitions.
            if (prevState != newState &&
                (prevState == DeviceState.ONLINE || prevState == DeviceState.OFFLINE ||
                 prevState == DeviceState.CONNECTING || prevState == DeviceState.UNKNOWN)
            ) {
                transitions++
                emitAlert(device, newState, probeResult.detail, nowMs)
            }
        }

        val outcome = SyncOutcome(
            ok = true,
            checkedCount = probed.size,
            onlineCount  = online,
            offlineCount = offline,
            transitions  = transitions,
            finishedAtMs = nowMs,
            error        = null,
        )
        _state.value = SyncState(SyncPhase.OK, outcome)
        return outcome
    }

    private data class ProbeResult(val reachable: Boolean, val latencyMs: Int, val detail: String)

    private suspend fun probe(device: DeviceProfile): ProbeResult = withContext(Dispatchers.IO) {
        val url = device.streamUrl()
        val (host, port, isHttp) = parseEndpoint(url)
            ?: return@withContext ProbeResult(false, 0, "Unparseable stream URL")

        val started = System.nanoTime()
        try {
            if (isHttp) {
                // For HTTP-style streams, a real GET against the URL itself is the
                // most truthful signal — TCP-only would miss path/auth issues.
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "SentinelCompanion/1.0")
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    val latency = ((System.nanoTime() - started) / 1_000_000).toInt()
                    val ok = resp.code in 200..399 || resp.code == 401 // 401 = server alive, just needs creds
                    ProbeResult(ok, latency, "HTTP ${resp.code}")
                }
            } else {
                Socket().use { sock ->
                    sock.connect(InetSocketAddress(host, port), 2500)
                    val latency = ((System.nanoTime() - started) / 1_000_000).toInt()
                    ProbeResult(true, latency, "TCP $host:$port open")
                }
            }
        } catch (e: Exception) {
            ProbeResult(false, 0, "${e.javaClass.simpleName}: ${e.message ?: "unreachable"}")
        }
    }

    private fun parseEndpoint(url: String): Triple<String, Int, Boolean>? {
        return try {
            val uri = java.net.URI(url)
            val scheme = uri.scheme?.lowercase() ?: return null
            val host = uri.host ?: return null
            val isHttp = scheme == "http" || scheme == "https"
            val defaultPort = when (scheme) {
                "rtsp" -> 554
                "http" -> 80
                "https" -> 443
                else -> -1
            }
            val port = if (uri.port > 0) uri.port else defaultPort
            if (port <= 0) return null
            Triple(host, port, isHttp)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun emitAlert(device: DeviceProfile, newState: DeviceState, detail: String, nowMs: Long) {
        val (type, message) = when (newState) {
            DeviceState.ONLINE  -> AlertType.CONNECTION_RESTORED to "${device.name} came back online ($detail)"
            DeviceState.OFFLINE -> AlertType.CONNECTION_LOST to "${device.name} unreachable ($detail)"
            else                -> return
        }
        // The Alert row keeps `cameraId` / `cameraName` column names from the v1
        // schema; semantically they now hold the device id/name. Renaming the
        // columns would require a destructive migration with no UX benefit.
        alertDao.insert(
            Alert(
                id          = UUID.randomUUID().toString(),
                cameraId    = device.id,
                cameraName  = device.name,
                type        = type.name,
                message     = message,
                timestampMs = nowMs,
                isRead      = false,
            )
        )
    }
}
