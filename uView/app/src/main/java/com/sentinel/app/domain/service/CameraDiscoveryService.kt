package com.sentinel.app.domain.service

import com.sentinel.app.domain.model.DiscoveredDevice
import com.sentinel.app.domain.model.ScanConfig
import com.sentinel.app.domain.model.ScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * CameraDiscoveryService — Phase 4
 *
 * Orchestrates all discovery strategies in parallel:
 *   1. ARP table read       — instant, finds all LAN hosts with MAC addresses
 *   2. mDNS / NSD           — finds devices advertising camera-related services
 *   3. ONVIF WS-Discovery   — UDP multicast, identifies ONVIF devices only
 *   4. TCP port probe        — fallback for devices not using any of the above
 *
 * Each strategy emits devices as they are found via [startScan]. Discovery
 * does not configure ONVIF media profiles, decode streams, or validate credentials.
 * The caller receives a hot [Flow<DiscoveredDevice>] — devices arrive
 * as soon as any strategy finds them.
 */
interface CameraDiscoveryService {

    /** Hot flow of discovered devices as they are found. */
    fun startScan(config: ScanConfig = ScanConfig()): Flow<DiscoveredDevice>

    /** Cancel any in-progress scan. */
    suspend fun cancelScan()

    /** Whether a scan is currently running. */
    val isScanning: StateFlow<Boolean>

    /** Probe a specific IP address using all applicable strategies. */
    suspend fun probeDevice(ipAddress: String): DiscoveredDevice?

    /**
     * Detect the local device's WiFi subnet (e.g. "192.168.1").
     * Returns null if WiFi is not connected or permissions are missing.
     */
    fun detectLocalSubnet(): String?

    /**
     * Validate that the device has the permissions and network conditions
     * required for each discovery strategy.
     */
    fun checkDiscoveryCapabilities(): DiscoveryCapabilities
}

/**
 * Reports which discovery strategies are available on the current device.
 */
data class DiscoveryCapabilities(
    val wifiConnected: Boolean,
    val multicastAvailable: Boolean,
    val arpTableReadable: Boolean,
    val nsdAvailable: Boolean,
    val detectedSubnet: String?
)
