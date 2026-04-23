package com.sentinel.app.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sentinel.app.core.service.MonitorServiceController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * BootReceiver
 *
 * Restarts [CameraMonitorService] after the device reboots, if the user
 * had background monitoring enabled.
 *
 * Requires: android.permission.RECEIVE_BOOT_COMPLETED
 *
 * Declared in the manifest so Android delivers BOOT_COMPLETED even
 * when the app is not running. The app will be cold-started by this
 * receiver before the service is launched.
 *
 * On API 29+, direct-boot aware components must handle the LOCKED_BOOT_COMPLETED
 * action to start before the user unlocks. We use BOOT_COMPLETED (post-unlock)
 * since camera streams require full network stack access.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var monitorServiceController: MonitorServiceController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            Timber.i("BootReceiver: device booted — attempting to start monitoring")
            scope.launch {
                // Delay slightly to let system services stabilize after boot
                kotlinx.coroutines.delay(8_000)
                monitorServiceController.startIfEnabled()
            }
        }
    }
}
