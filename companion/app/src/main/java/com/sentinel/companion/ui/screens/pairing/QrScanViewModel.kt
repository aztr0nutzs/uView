package com.sentinel.companion.ui.screens.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.companion.data.pairing.PairingClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface QrScanState {
    object Scanning : QrScanState
    object Pairing : QrScanState
    data class Paired(val host: String, val port: Int, val deviceName: String) : QrScanState
    data class Failed(val reason: String) : QrScanState
}

@HiltViewModel
class QrScanViewModel @Inject constructor(
    private val pairingClient: PairingClient,
) : ViewModel() {

    private val _state = MutableStateFlow<QrScanState>(QrScanState.Scanning)
    val state: StateFlow<QrScanState> = _state.asStateFlow()

    @Volatile private var consumed = false

    fun onQrDecoded(text: String) {
        if (consumed) return
        consumed = true
        _state.value = QrScanState.Pairing
        viewModelScope.launch {
            when (val r = pairingClient.pair(text)) {
                is PairingClient.Result.Success ->
                    _state.value = QrScanState.Paired(r.host, r.port, r.deviceName)
                is PairingClient.Result.Failure ->
                    _state.value = QrScanState.Failed(r.reason)
            }
        }
    }

    fun retry() {
        consumed = false
        _state.value = QrScanState.Scanning
    }
}
