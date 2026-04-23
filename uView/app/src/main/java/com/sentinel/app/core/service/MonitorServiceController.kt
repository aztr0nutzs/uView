package com.sentinel.app.core.service

import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import com.sentinel.app.data.preferences.AppPreferencesDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MonitorServiceController
 *
 * The single point of control for [CameraMonitorService] lifecycle.
 * ViewModels and screens call this rather than manipulating service
 * intents directly.
 *
 * Responsibilities:
 *   - Start the foreground service (respecting Android 14+ restrictions)
 *   - Stop the service cleanly
 *   - Check whether the service is currently running
 *   - Check app preferences before starting (background monitoring enabled?)
 *
 * Android 14+ (API 34) note:
 *   FGS of type "dataSync" are restricted — they cannot be started from
 *   the background. This controller is always called from a user-initiated
 *   action (Settings toggle, lock screen broadcast) which qualifies as
 *   a foreground context. If the restriction is hit, we log it and skip
 *   rather than crashing.
 */
@Singleton
class MonitorServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsDataSource: AppPreferencesDataSource
) {
    private var serviceRunning = false

    /**
     * Start background monitoring if:
     *   - The user has background monitoring enabled in preferences
     *   - We are not already running
     *
     * Safe to call multiple times — idempotent.
     */
    suspend fun startIfEnabled() {
        val prefs = prefsDataSource.preferences.first()
        if (!prefs.autoReconnectEnabled) {
            Timber.d("MonitorServiceController: background monitoring disabled in prefs — skipping")
            return
        }
        start()
    }

    /**
     * Unconditionally start the foreground service.
     */
    fun start() {
        if (serviceRunning) {
            Timber.d("MonitorServiceController: already running")
            return
        }
        try {
            val intent = CameraMonitorService.startIntent(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
            serviceRunning = true
            Timber.i("MonitorServiceController: service started")
        } catch (e: Exception) {
            // Can happen on Android 14+ if called from background context
            Timber.e(e, "MonitorServiceController: failed to start service — ${e.message}")
        }
    }

    /**
     * Stop the foreground service.
     */
    fun stop() {
        try {
            context.startService(CameraMonitorService.stopIntent(context))
            serviceRunning = false
            Timber.i("MonitorServiceController: service stopped")
        } catch (e: Exception) {
            Timber.e(e, "MonitorServiceController: failed to stop service")
        }
    }

    /**
     * Whether the service is believed to be running.
     * This is tracked in-process — not queried from ActivityManager,
     * which is deprecated and unreliable.
     */
    val isRunning: Boolean get() = serviceRunning
}
