package com.sentinel.app.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.app.core.power.PowerAwarenessManager
import com.sentinel.app.core.security.AppLockManager
import com.sentinel.app.core.service.MonitorServiceController
import com.sentinel.app.data.preferences.AppPreferences
import com.sentinel.app.data.preferences.AppPreferencesDataSource
import com.sentinel.app.data.preferences.MotionSensitivity
import com.sentinel.app.domain.model.StreamQualityProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferencesDataSource,
    private val monitorServiceController: MonitorServiceController,
    private val powerAwarenessManager: PowerAwarenessManager,
    private val appLockManager: AppLockManager
) : ViewModel() {

    val settings: StateFlow<AppPreferences> = prefs.preferences.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences()
    )

    val powerState: StateFlow<PowerAwarenessManager.PowerState> =
        powerAwarenessManager.powerState.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000),
            PowerAwarenessManager.PowerState(100, true, false, false,
                StreamQualityProfile.AUTO)
        )

    val isMonitoringServiceRunning: Boolean
        get() = monitorServiceController.isRunning

    // ── App Lock feedback ────────────────────────────────────────────────
    private val _appLockError = MutableStateFlow<String?>(null)
    val appLockError: StateFlow<String?> = _appLockError.asStateFlow()

    fun clearAppLockError() { _appLockError.value = null }

    /**
     * Toggle app lock. Requires a [FragmentActivity] reference to check
     * whether the device supports biometric/device-credential authentication.
     *
     * If the device has no lock screen configured and the user tries to
     * enable app lock, the toggle is rejected with an error message.
     */
    fun setAppLock(enabled: Boolean, activity: androidx.fragment.app.FragmentActivity) {
        viewModelScope.launch {
            val success = appLockManager.setEnabled(enabled, activity)
            if (!success) {
                _appLockError.value = "Cannot enable app lock — no PIN, pattern, or biometric is set up on this device"
            }
        }
    }

    // ── Standard preference setters ─────────────────────────────────────
    fun setDarkTheme(v: Boolean)              = viewModelScope.launch { prefs.setDarkTheme(v) }
    fun setAutoReconnect(v: Boolean)          = viewModelScope.launch { prefs.setAutoReconnect(v) }
    fun setNotifications(v: Boolean)          = viewModelScope.launch { prefs.setNotificationsEnabled(v) }
    fun setLocalOnly(v: Boolean)              = viewModelScope.launch { prefs.setLocalOnlyMode(v) }
    fun setDataSaver(v: Boolean)              = viewModelScope.launch { prefs.setDataSaverMode(v) }
    fun setDiagnosticsLogging(v: Boolean)     = viewModelScope.launch { prefs.setDiagnosticsLogging(v) }
    fun setStreamQuality(q: StreamQualityProfile) = viewModelScope.launch { prefs.setDefaultStreamQuality(q) }
    fun setMotionSensitivity(s: MotionSensitivity) = viewModelScope.launch { prefs.setMotionSensitivity(s) }
    fun setNetworkScan(v: Boolean)            = viewModelScope.launch { prefs.setNetworkScanEnabled(v) }

    fun startBackgroundMonitoring() = monitorServiceController.start()
    fun stopBackgroundMonitoring()  = monitorServiceController.stop()
}
