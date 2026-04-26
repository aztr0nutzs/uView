package com.sentinel.companion.data.repository

import com.sentinel.companion.data.db.AlertDao
import com.sentinel.companion.data.db.CameraDao
import com.sentinel.companion.data.model.Alert
import com.sentinel.companion.data.model.Camera
import com.sentinel.companion.data.model.CameraStatus
import com.sentinel.companion.data.model.SystemStatus
import com.sentinel.companion.data.sync.CompanionSyncService
import com.sentinel.companion.data.sync.SyncPhase
import com.sentinel.companion.data.sync.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authoritative source of truth (post-mock):
 *   • cameras  → Room `cameras` table, written by setup wizard + sync service
 *   • alerts   → Room `alerts` table, written only by [CompanionSyncService] from
 *                observed camera status transitions (no fixture seeding)
 *   • status   → derived from cameras + alerts + ConnectionPrefs + SyncState
 */
@Singleton
class CameraRepository @Inject constructor(
    private val cameraDao: CameraDao,
    private val alertDao: AlertDao,
    private val prefsRepo: PreferencesRepository,
    private val syncService: CompanionSyncService,
) {

    val cameras: Flow<List<Camera>> = cameraDao.observeAll()
    val alerts: Flow<List<Alert>> = alertDao.observeAll()
    val syncState: StateFlow<SyncState> = syncService.state

    val systemStatus: Flow<SystemStatus> = combine(
        cameras,
        alertDao.observeUnreadCount(),
        prefsRepo.connectionPrefs,
        syncService.state,
    ) { cams, unread, conn, sync ->
        SystemStatus(
            isConnected       = sync.lastOutcome?.ok == true,
            hostAddress       = conn.hostAddress,
            totalCameras      = cams.size,
            onlineCameras     = cams.count { it.statusEnum() == CameraStatus.ONLINE },
            offlineCameras    = cams.count { it.statusEnum() == CameraStatus.OFFLINE },
            connectingCameras = cams.count { it.statusEnum() == CameraStatus.CONNECTING },
            disabledCameras   = cams.count { it.statusEnum() == CameraStatus.DISABLED },
            unreadAlerts      = unread,
            // Uptime is a backend concept we do not yet have a contract for — keep at 0
            // until the server contract lands; UI renders 0 as "—" instead of faking it.
            uptimeMs          = 0L,
            lastSyncMs        = sync.lastOutcome?.finishedAtMs ?: 0L,
        )
    }

    fun getCameraById(id: String): Flow<Camera?> = cameraDao.observeById(id)

    fun getAlertsByCameraId(cameraId: String): Flow<List<Alert>> =
        alerts.map { list -> list.filter { it.cameraId == cameraId } }

    suspend fun toggleFavorite(cameraId: String) {
        val current = cameraDao.getById(cameraId) ?: return
        cameraDao.setFavorite(cameraId, !current.isFavorite)
    }

    suspend fun toggleEnabled(cameraId: String) {
        val current = cameraDao.getById(cameraId) ?: return
        cameraDao.setEnabled(cameraId, !current.isEnabled)
    }

    /** Re-probe a single camera by triggering a full sync pass. */
    suspend fun reconnectCamera(cameraId: String) {
        cameraDao.updateStatus(cameraId, CameraStatus.CONNECTING.name, 0, System.currentTimeMillis())
        syncService.syncOnce()
    }

    suspend fun markAlertRead(alertId: String) = alertDao.markRead(alertId)
    suspend fun markAllAlertsRead() = alertDao.markAllRead()

    suspend fun addCamera(camera: Camera) = cameraDao.upsert(camera)
    suspend fun deleteCamera(cameraId: String) = cameraDao.deleteById(cameraId)

    /** Manual refresh trigger from the UI. */
    suspend fun refresh() {
        syncService.syncOnce()
    }

    fun isSyncing(): Boolean = syncState.value.phase == SyncPhase.RUNNING
}
