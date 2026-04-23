package com.sentinel.app.core.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import com.sentinel.app.data.playback.PlaybackManager
import com.sentinel.app.data.preferences.AppPreferencesDataSource
import com.sentinel.app.domain.model.StreamQualityProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PowerAwarenessManager
 *
 * Monitors device battery level, charging state, and data saver status.
 * When conditions are unfavourable it signals that stream quality should
 * be reduced to preserve battery / mobile data.
 *
 * Thresholds:
 *   Battery < 20% AND not charging → LOW quality recommendation
 *   Battery < 10% AND not charging → MINIMAL quality recommendation
 *   Data Saver mode enabled        → MINIMAL quality recommendation
 *   Metered network (mobile data)  → MEDIUM quality recommendation (if not overridden by battery)
 *
 * These are RECOMMENDATIONS — the user's explicit quality setting in
 * preferences always overrides automatic reduction unless dataSaverMode
 * is explicitly enabled.
 *
 * The manager registers a sticky BroadcastReceiver for battery events.
 * Call [start] once (from SentinelApplication) and [stop] never — this
 * is a singleton that lives for the app's lifetime.
 */
@Singleton
class PowerAwarenessManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefsDataSource: AppPreferencesDataSource
) {
    data class PowerState(
        val batteryPercent: Int,
        val isCharging: Boolean,
        val isOnMeteredNetwork: Boolean,
        val dataSaverEnabled: Boolean,
        val recommendedQuality: StreamQualityProfile
    )

    private val _powerState = MutableStateFlow(
        PowerState(
            batteryPercent      = 100,
            isCharging          = true,
            isOnMeteredNetwork  = false,
            dataSaverEnabled    = false,
            recommendedQuality  = StreamQualityProfile.AUTO
        )
    )
    val powerState: StateFlow<PowerState> = _powerState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateFromBatteryIntent(intent)
        }
    }

    /**
     * Begin monitoring battery and network state.
     * Call once on app startup.
     */
    fun start() {
        // Register sticky battery receiver
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = context.registerReceiver(batteryReceiver, filter)
        stickyIntent?.let { updateFromBatteryIntent(it) }

        // Also observe preferences for data saver toggle
        scope.launch {
            prefsDataSource.preferences.collect { prefs ->
                _powerState.value = _powerState.value.copy(
                    dataSaverEnabled   = prefs.dataSaverMode,
                    isOnMeteredNetwork = isMeteredNetwork(),
                    recommendedQuality = computeRecommendation(
                        battery      = _powerState.value.batteryPercent,
                        charging     = _powerState.value.isCharging,
                        metered      = isMeteredNetwork(),
                        dataSaver    = prefs.dataSaverMode
                    )
                )
            }
        }

        Timber.d("PowerAwarenessManager: started")
    }

    fun stop() {
        try { context.unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
    }

    /**
     * Whether the current power state is considered "restricted".
     * Use this to decide whether to start background streams.
     */
    val isRestricted: Boolean
        get() = _powerState.value.recommendedQuality == StreamQualityProfile.MINIMAL

    private fun updateFromBatteryIntent(intent: Intent) {
        val level  = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale  = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val percent   = if (scale > 0) (level * 100 / scale) else _powerState.value.batteryPercent
        val charging  = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
        val metered   = isMeteredNetwork()
        val dataSaver = _powerState.value.dataSaverEnabled

        val recommended = computeRecommendation(percent, charging, metered, dataSaver)

        if (_powerState.value.batteryPercent != percent ||
            _powerState.value.isCharging != charging ||
            _powerState.value.recommendedQuality != recommended) {

            _powerState.value = PowerState(
                batteryPercent     = percent,
                isCharging         = charging,
                isOnMeteredNetwork = metered,
                dataSaverEnabled   = dataSaver,
                recommendedQuality = recommended
            )
            Timber.d("PowerAwarenessManager: battery=$percent% charging=$charging metered=$metered → $recommended")
        }
    }

    private fun computeRecommendation(
        battery: Int,
        charging: Boolean,
        metered: Boolean,
        dataSaver: Boolean
    ): StreamQualityProfile = when {
        dataSaver                      -> StreamQualityProfile.MINIMAL
        !charging && battery < 10      -> StreamQualityProfile.MINIMAL
        !charging && battery < 20      -> StreamQualityProfile.LOW
        metered                        -> StreamQualityProfile.MEDIUM
        else                           -> StreamQualityProfile.AUTO
    }

    private fun isMeteredNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps    = cm.getNetworkCapabilities(network) ?: return false
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
