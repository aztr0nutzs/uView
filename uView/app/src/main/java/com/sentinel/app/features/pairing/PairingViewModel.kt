package com.sentinel.app.features.pairing

import android.os.Build
import androidx.lifecycle.ViewModel
import com.sentinel.app.core.pairing.PairingHost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val host: PairingHost,
) : ViewModel() {

    val state: StateFlow<PairingHost.State> = host.state

    fun start() = host.start(deviceName = defaultDeviceName())

    fun stop() = host.stop()

    override fun onCleared() {
        host.stop()
    }

    private fun defaultDeviceName(): String =
        listOfNotNull(Build.MANUFACTURER, Build.MODEL)
            .joinToString(" ")
            .ifBlank { "Sentinel Hub" }
}
