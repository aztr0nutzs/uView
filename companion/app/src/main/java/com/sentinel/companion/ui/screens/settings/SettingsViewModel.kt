package com.sentinel.companion.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.companion.data.model.AppPrefs
import com.sentinel.companion.data.model.ConnectionPrefs
import com.sentinel.companion.data.model.StreamQuality
import com.sentinel.companion.data.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val connectionPrefs: ConnectionPrefs = ConnectionPrefs(),
    val appPrefs: AppPrefs = AppPrefs(),
    val showDisconnectConfirm: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepo: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(prefsRepo.connectionPrefs, prefsRepo.appPrefs) { conn, app ->
                SettingsUiState(connectionPrefs = conn, appPrefs = app)
            }.collect { _uiState.value = it }
        }
    }

    fun setNotifications(enabled: Boolean) = updateAppPrefs { it.copy(notificationsEnabled = enabled) }
    fun setMotionAlerts(enabled: Boolean)   = updateAppPrefs { it.copy(motionAlertsEnabled = enabled) }
    fun setConnectionAlerts(enabled: Boolean) = updateAppPrefs { it.copy(connectionAlertsEnabled = enabled) }
    fun setDataSaver(enabled: Boolean)      = updateAppPrefs { it.copy(dataSaverMode = enabled) }
    fun setBiometricLock(enabled: Boolean)  = updateAppPrefs { it.copy(biometricLock = enabled) }
    fun setLocalOnlyMode(enabled: Boolean)  = updateAppPrefs { it.copy(localOnlyMode = enabled) }
    fun setStreamQuality(quality: StreamQuality) = updateAppPrefs { it.copy(streamQuality = quality) }

    fun disconnect(onDisconnected: () -> Unit) {
        viewModelScope.launch {
            prefsRepo.saveConnectionPrefs(ConnectionPrefs())
            onDisconnected()
        }
    }

    private fun updateAppPrefs(transform: (AppPrefs) -> AppPrefs) {
        viewModelScope.launch {
            prefsRepo.saveAppPrefs(transform(_uiState.value.appPrefs))
        }
    }
}
