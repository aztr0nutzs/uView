package com.sentinel.companion.data.repository

import com.sentinel.companion.data.db.AlertDao
import com.sentinel.companion.data.model.Alert
import com.sentinel.companion.data.sync.CompanionSyncService
import com.sentinel.companion.data.sync.SyncPhase
import com.sentinel.companion.data.sync.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alerts + sync facade. The split-brain Camera/CameraDao path was removed —
 * device truth lives in [DeviceRepository]. This repository owns only the
 * alerts feed and the sync trigger, both of which are device-agnostic
 * (alerts reference device ids by string, and sync operates on the devices
 * table directly via [CompanionSyncService]).
 */
@Singleton
class AlertsRepository @Inject constructor(
    private val alertDao: AlertDao,
    private val syncService: CompanionSyncService,
) {
    val alerts: Flow<List<Alert>> = alertDao.observeAll()
    val syncState: StateFlow<SyncState> = syncService.state

    suspend fun markAlertRead(alertId: String) = alertDao.markRead(alertId)
    suspend fun markAllAlertsRead() = alertDao.markAllRead()

    /** Manual refresh trigger from the UI. */
    suspend fun refresh() {
        syncService.syncOnce()
    }

    fun isSyncing(): Boolean = syncState.value.phase == SyncPhase.RUNNING
}
