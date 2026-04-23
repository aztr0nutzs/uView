package com.sentinel.app.features.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.ConnectionTestResult
import com.sentinel.app.domain.repository.CameraRepository
import com.sentinel.app.domain.service.CameraConnectionTester
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val cameras: List<CameraDevice> = emptyList(),
    val testResults: Map<String, ConnectionTestResult> = emptyMap(),
    val testingIds: Set<String> = emptySet(),
    val isTestingAll: Boolean = false,
    val loggingEnabled: Boolean = false
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val connectionTester: CameraConnectionTester
) : ViewModel() {

    private val _extra = MutableStateFlow(DiagnosticsUiState())

    val uiState: StateFlow<DiagnosticsUiState> = combine(
        cameraRepository.observeAllCameras(),
        _extra
    ) { cameras, extra ->
        extra.copy(cameras = cameras)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiagnosticsUiState())

    fun testCamera(camera: CameraDevice) = viewModelScope.launch {
        _extra.update { it.copy(testingIds = it.testingIds + camera.id) }
        val result = connectionTester.testConnection(camera)
        _extra.update { state ->
            state.copy(
                testingIds = state.testingIds - camera.id,
                testResults = state.testResults + (camera.id to result)
            )
        }
    }

    fun testAll() = viewModelScope.launch {
        val cameras = uiState.value.cameras
        _extra.update { it.copy(isTestingAll = true) }
        cameras.forEach { cam ->
            _extra.update { it.copy(testingIds = it.testingIds + cam.id) }
            val result = connectionTester.testConnection(cam)
            _extra.update { state ->
                state.copy(
                    testingIds = state.testingIds - cam.id,
                    testResults = state.testResults + (cam.id to result)
                )
            }
        }
        _extra.update { it.copy(isTestingAll = false) }
    }

    fun clearResults() = _extra.update { it.copy(testResults = emptyMap()) }
}
