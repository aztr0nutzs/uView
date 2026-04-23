package com.sentinel.app.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.app.core.power.PowerAwarenessManager
import com.sentinel.app.core.service.MonitorServiceController
import com.sentinel.app.data.preferences.AppPreferences
import com.sentinel.app.data.preferences.AppPreferencesDataSource
import com.sentinel.app.data.preferences.MotionSensitivity
import com.sentinel.app.domain.model.StreamQualityProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferencesDataSource,
    private val monitorServiceController: MonitorServiceController,
    private val powerAwarenessManager: PowerAwarenessManager
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
