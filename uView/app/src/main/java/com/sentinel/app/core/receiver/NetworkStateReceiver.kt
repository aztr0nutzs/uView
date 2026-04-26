package com.sentinel.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.sentinel.app.core.service.MonitorServiceController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * NetworkStateReceiver
 *
 * Listens for WiFi connectivity changes and:
 *   - Attempts to restart monitoring when WiFi comes back after an outage
 *   - Logs connection events for diagnostics
 *
 * Registered dynamically in [MainActivity] (not in the manifest) to
 * avoid waking the app unnecessarily when the user is not monitoring.
 *
 * Note: On Android 7.0+ (API 24+), CONNECTIVITY_ACTION is not delivered
 * to manifest-declared receivers. Dynamic registration is required.
 */
@AndroidEntryPoint
class NetworkStateReceiver @Inject constructor() : BroadcastReceiver() {

    @Inject lateinit var monitorServiceController: MonitorServiceController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION,
            "android.net.wifi.WIFI_STATE_CHANGED",
            "android.net.wifi.STATE_CHANGE" -> {
                val connected = isWifiConnected(context)
                Timber.d("NetworkStateReceiver: WiFi connected=$connected")

                if (connected) {
                    // WiFi just came back — try to resume monitoring
                    scope.launch {
                        monitorServiceController.startIfEnabled()
                    }
                }
                // Note: we do NOT stop monitoring when WiFi drops.
                // The playback service handles reconnects internally.
                // Stopping the FGS on disconnect would lose the persistent notification
                // which the user relies on as a "monitoring is active" indicator.
            }
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps    = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
