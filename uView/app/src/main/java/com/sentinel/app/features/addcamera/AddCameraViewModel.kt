package com.sentinel.app.features.addcamera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.app.domain.model.AndroidPhoneSourceConfig
import com.sentinel.app.domain.model.CameraConnectionProfile
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraSourceType
import com.sentinel.app.domain.model.ConnectionTestResult
import com.sentinel.app.domain.model.StreamQualityProfile
import com.sentinel.app.domain.model.StreamTransport
import com.sentinel.app.domain.model.isSelectableInShipBuild
import com.sentinel.app.domain.repository.CameraRepository
import com.sentinel.app.domain.service.CameraConnectionTester
import com.sentinel.app.data.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Wizard Step definitions
// ─────────────────────────────────────────────────────────────────────────────

enum class WizardStep(val title: String) {
    SELECT_TYPE("Source Type"),
    NAME_AND_ROOM("Name & Room"),
    NETWORK("Network Details"),
    CREDENTIALS("Credentials"),
    TEST("Test Connection"),
    CONFIRM("Review & Save")
}

// ─────────────────────────────────────────────────────────────────────────────
// AddCameraUiState — holds the full wizard draft
// ─────────────────────────────────────────────────────────────────────────────

data class AddCameraUiState(
    val step: WizardStep = WizardStep.SELECT_TYPE,

    // Step 1
    val selectedSourceType: CameraSourceType? = null,

    // Step 2
    val cameraName: String = "",
    val cameraRoom: String = "",
    val existingRooms: List<String> = emptyList(),

    // Step 3 — standard cameras
    val host: String = "",
    val port: String = "",
    val streamPath: String = "",
    val transport: StreamTransport = StreamTransport.AUTO,
    val useTls: Boolean = false,
    val streamQuality: StreamQualityProfile = StreamQualityProfile.AUTO,

    // Step 3 — Android phone specific
    val phoneNickname: String = "",
    val phoneEndpointUrl: String = "",
    val phoneIsLanOnly: Boolean = true,
    val phoneAudioAvailable: Boolean = false,

    // Step 4 — credentials
    val username: String = "",
    val password: String = "",

    // Step 5 — test
    val testResult: ConnectionTestResult? = null,
    val isTesting: Boolean = false,
    val testSkipped: Boolean = false,

    // Global
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap()
) {
    val isPhoneSource get() = selectedSourceType in listOf(
        CameraSourceType.ANDROID_DROIDCAM,
        CameraSourceType.ANDROID_ALFRED,
        CameraSourceType.ANDROID_IPWEBCAM,
        CameraSourceType.ANDROID_CUSTOM
    )

    val canProceed: Boolean get() = when (step) {
        WizardStep.SELECT_TYPE   -> selectedSourceType?.isSelectableInShipBuild == true
        WizardStep.NAME_AND_ROOM -> cameraName.isNotBlank() && cameraRoom.isNotBlank()
        WizardStep.NETWORK       -> if (isPhoneSource) phoneEndpointUrl.isNotBlank()
                                    else host.isNotBlank() && port.isNotBlank()
        WizardStep.CREDENTIALS   -> true   // credentials are optional
        WizardStep.TEST          -> true   // user can skip test
        WizardStep.CONFIRM       -> true
    }

    val stepProgress: Float get() {
        val idx = WizardStep.values().indexOf(step)
        return (idx + 1).toFloat() / WizardStep.values().size
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AddCameraViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class AddCameraViewModel @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val connectionTester: CameraConnectionTester,
    private val playbackManager: PlaybackManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AddCameraUiState())
    val state: StateFlow<AddCameraUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val rooms = cameraRepository.getAllRooms()
            _state.update { it.copy(existingRooms = rooms) }
        }
    }

    fun selectSourceType(type: CameraSourceType) {
        if (!type.isSelectableInShipBuild) {
            _state.update {
                it.copy(validationErrors = it.validationErrors + ("sourceType" to "${type.displayName} is unavailable in this build."))
            }
            return
        }

        val defaultPort = when (type) {
            CameraSourceType.RTSP                -> "554"
            CameraSourceType.MJPEG               -> "8080"
            CameraSourceType.ONVIF               -> "80"
            CameraSourceType.HLS                 -> "80"
            CameraSourceType.ANDROID_DROIDCAM    -> "4747"
            CameraSourceType.ANDROID_IPWEBCAM    -> "8080"
            CameraSourceType.ANDROID_ALFRED      -> ""
            CameraSourceType.ANDROID_CUSTOM      -> ""
            CameraSourceType.GENERIC_URL         -> ""
            CameraSourceType.DEMO                -> ""
        }
        _state.update { it.copy(selectedSourceType = type, port = defaultPort) }
    }

    fun setName(v: String)            = _state.update { it.copy(cameraName = v) }
    fun setRoom(v: String)            = _state.update { it.copy(cameraRoom = v) }
    fun setHost(v: String)            = _state.update { it.copy(host = v) }
    fun setPort(v: String)            = _state.update { it.copy(port = v) }
    fun setStreamPath(v: String)      = _state.update { it.copy(streamPath = v) }
    fun setTransport(v: StreamTransport) = _state.update { it.copy(transport = v) }
    fun setUseTls(v: Boolean)         = _state.update { it.copy(useTls = v) }
    fun setQuality(v: StreamQualityProfile) = _state.update { it.copy(streamQuality = v) }
    fun setPhoneNickname(v: String)   = _state.update { it.copy(phoneNickname = v) }
    fun setPhoneEndpoint(v: String)   = _state.update { it.copy(phoneEndpointUrl = v) }
    fun setPhoneLanOnly(v: Boolean)   = _state.update { it.copy(phoneIsLanOnly = v) }
    fun setPhoneAudio(v: Boolean)     = _state.update { it.copy(phoneAudioAvailable = v) }
    fun setUsername(v: String)        = _state.update { it.copy(username = v) }
    fun setPassword(v: String)        = _state.update { it.copy(password = v) }

    fun nextStep() {
        val steps = WizardStep.values()
        val current = _state.value.step
        val nextIdx = steps.indexOf(current) + 1
        if (nextIdx < steps.size) {
            _state.update { it.copy(step = steps[nextIdx]) }
        }
    }

    fun prevStep() {
        val steps = WizardStep.values()
        val current = _state.value.step
        val prevIdx = steps.indexOf(current) - 1
        if (prevIdx >= 0) {
            _state.update { it.copy(step = steps[prevIdx]) }
        }
    }

    fun runConnectionTest() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(isTesting = true) }
        val mockCamera = buildDraftCamera(s)
        val result = connectionTester.testConnection(mockCamera)
        _state.update { it.copy(isTesting = false, testResult = result, testSkipped = false) }
    }

    fun skipTest() = _state.update { it.copy(testSkipped = true) }

    fun saveCamera() = viewModelScope.launch {
        val selected = _state.value.selectedSourceType
        if (selected?.isSelectableInShipBuild != true) {
            _state.update {
                it.copy(
                    isSaving = false,
                    validationErrors = it.validationErrors + ("sourceType" to "Select a supported direct stream source before saving.")
                )
            }
            return@launch
        }
        _state.update { it.copy(isSaving = true) }
        val camera = buildDraftCamera(_state.value)
        cameraRepository.saveCamera(camera)
        // Attempt to start playback immediately so newly added cameras begin streaming
        try {
            playbackManager.startCamera(camera)
        } catch (e: Exception) {
            // Non-fatal: log and continue — UI will still show saved camera
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                // update state with a non-blocking validation error if needed
                _state.update { it.copy(isSaving = false, saveSuccess = true, validationErrors = it.validationErrors + ("playback" to "Failed to start stream: ${e.message}")) }
            }
            return@launch
        }
        _state.update { it.copy(isSaving = false, saveSuccess = true) }
    }

    private fun buildDraftCamera(s: AddCameraUiState): CameraDevice {
        val profile = CameraConnectionProfile(
            host = if (s.isPhoneSource) s.phoneEndpointUrl else s.host,
            port = s.port.toIntOrNull() ?: 554,
            path = s.streamPath,
            username = s.username,
            password = s.password,
            transport = s.transport,
            useTls = s.useTls
        )
        val phoneConfig = if (s.isPhoneSource && s.selectedSourceType != null) {
            AndroidPhoneSourceConfig(
                phoneNickname = s.phoneNickname.ifBlank { s.cameraName },
                appMethod = s.selectedSourceType,
                endpointUrl = s.phoneEndpointUrl,
                isLanOnly = s.phoneIsLanOnly,
                audioAvailable = s.phoneAudioAvailable,
                qualityProfile = s.streamQuality
            )
        } else null

        return CameraDevice(
            id = UUID.randomUUID().toString(),
            name = s.cameraName,
            room = s.cameraRoom,
            sourceType = s.selectedSourceType ?: CameraSourceType.GENERIC_URL,
            connectionProfile = profile,
            androidPhoneConfig = phoneConfig,
            preferredQuality = s.streamQuality,
            isEnabled = true
        )
    }
}
