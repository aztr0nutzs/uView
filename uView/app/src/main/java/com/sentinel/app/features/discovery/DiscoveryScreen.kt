package com.sentinel.app.features.discovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.app.domain.model.CameraSourceType
import com.sentinel.app.domain.model.DiscoveredDevice
import com.sentinel.app.domain.model.DiscoveryConfidence
import com.sentinel.app.domain.model.DiscoveryMethod
import com.sentinel.app.domain.service.DiscoveryCapabilities
import com.sentinel.app.ui.components.GhostButton
import com.sentinel.app.ui.components.PrimaryButton
import com.sentinel.app.ui.preview.SampleData
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanPrimary
import com.sentinel.app.ui.theme.CyanSubtle
import com.sentinel.app.ui.theme.SentinelTheme
import com.sentinel.app.ui.theme.StatusOffline
import com.sentinel.app.ui.theme.StatusOnline
import com.sentinel.app.ui.theme.SurfaceElevated
import com.sentinel.app.ui.theme.SurfaceHighest
import com.sentinel.app.ui.theme.SurfaceStroke
import com.sentinel.app.ui.theme.TextDisabled
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary
import com.sentinel.app.ui.theme.WarningAmber

@Composable
fun DiscoveryScreen(
    onNavigateBack: () -> Unit,
    onAddCamera: (String) -> Unit,
    viewModel: DiscoveryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DiscoveryContent(
        state          = state,
        onNavigateBack = onNavigateBack,
        onStartScan    = { viewModel.startScan() },
        onCancelScan   = viewModel::cancelScan,
        onClearResults = viewModel::clearResults,
        onAddCamera    = onAddCamera
    )
}

@Composable
private fun DiscoveryContent(
    state: DiscoveryUiState,
    onNavigateBack: () -> Unit,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onClearResults: () -> Unit,
    onAddCamera: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(BackgroundDeep)) {

        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
            }
            Text(
                "Network Discovery",
                style    = MaterialTheme.typography.titleLarge,
                color    = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (state.isScanning) {
                IconButton(onClick = onCancelScan) {
                    Icon(Icons.Default.Close, "Cancel", tint = TextSecondary)
                }
            }
        }

        LazyColumn(
            contentPadding    = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Capabilities banner ───────────────────────────────────────
            state.capabilities?.let { caps ->
                item { CapabilitiesBanner(caps) }
            }

            // ── Scanning progress / result summary ────────────────────────
            item {
                AnimatedVisibility(visible = state.isScanning) {
                    ScanProgressCard(state.activeStrategies)
                }
            }

            if (state.scanComplete && !state.isScanning) {
                item { ScanSummaryCard(state) }
            }

            // ── Action buttons ────────────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!state.isScanning) {
                        PrimaryButton(
                            text     = if (state.scanComplete) "Scan Again" else "Start Scan",
                            onClick  = onStartScan,
                            icon     = Icons.Default.Radar,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    GhostButton(
                        text     = "Add Manually",
                        onClick  = { onAddCamera("") },
                        icon     = Icons.Default.Add,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Results header ────────────────────────────────────────────
            if (state.devices.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${state.devices.size} device${if (state.devices.size != 1) "s" else ""} found",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        if (!state.isScanning) {
                            Text(
                                "Clear",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = CyanPrimary,
                                modifier = Modifier.clickable(onClick = onClearResults)
                            )
                        }
                    }
                }
            }

            // ── Device cards ──────────────────────────────────────────────
            if (state.devices.isEmpty() && !state.isScanning && !state.scanComplete) {
                item { ScanIdlePrompt() }
            } else {
                items(state.devices, key = { it.ipAddress }) { device ->
                    DiscoveredDeviceCard(device = device, onAddCamera = { onAddCamera(device.ipAddress) })
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CapabilitiesBanner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CapabilitiesBanner(caps: DiscoveryCapabilities) {
    val wifiOk = caps.wifiConnected
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (wifiOk) CyanSubtle else StatusOffline.copy(alpha = 0.1f))
            .border(1.dp,
                if (wifiOk) CyanPrimary.copy(alpha = 0.2f) else StatusOffline.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!wifiOk) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.WifiOff, null, tint = StatusOffline, modifier = Modifier.size(16.dp))
                Text("WiFi not connected — connect to your home network to scan",
                    style = MaterialTheme.typography.bodySmall, color = StatusOffline)
            }
        } else {
            Text(
                "Scanning ${caps.detectedSubnet ?: "local network"}.0/24 using: " +
                buildList {
                    if (caps.arpTableReadable) add("ARP table")
                    if (caps.nsdAvailable)     add("mDNS")
                    if (caps.multicastAvailable) add("ONVIF WS-Discovery")
                    add("TCP probe")
                }.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = CyanPrimary.copy(alpha = 0.9f)
            )
            Text(
                "Only devices on your local network are scanned. No data leaves your device.",
                style = MaterialTheme.typography.labelSmall,
                color = CyanPrimary.copy(alpha = 0.6f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ScanProgressCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScanProgressCard(activeStrategies: Set<DiscoveryMethod>) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceStroke, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScanPulseRow()
        LinearProgressIndicator(
            modifier     = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
            color        = CyanPrimary,
            trackColor   = SurfaceHighest
        )
        // Active strategy indicators
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StrategyChip("ARP",   DiscoveryMethod.ARP_TABLE           in activeStrategies)
            StrategyChip("mDNS",  DiscoveryMethod.MDNS                in activeStrategies)
            StrategyChip("ONVIF", DiscoveryMethod.ONVIF_WS_DISCOVERY  in activeStrategies)
            StrategyChip("TCP",   DiscoveryMethod.TCP_PORT_PROBE       in activeStrategies)
        }
    }
}

@Composable
private fun ScanPulseRow() {
    val transition = rememberInfiniteTransition(label = "scanPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Radar, null, tint = CyanPrimary.copy(alpha = alpha), modifier = Modifier.size(18.dp))
        Text("Scanning local network…", style = MaterialTheme.typography.bodySmall, color = CyanPrimary.copy(alpha = alpha))
    }
}

@Composable
private fun StrategyChip(label: String, active: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) StatusOnline.copy(alpha = 0.12f) else SurfaceHighest)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(5.dp).clip(CircleShape)
            .background(if (active) StatusOnline else TextDisabled))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (active) StatusOnline else TextDisabled)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ScanSummaryCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScanSummaryCard(state: DiscoveryUiState) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceStroke, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = StatusOnline, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Scan complete — ${state.devices.size} device${if (state.devices.size != 1) "s" else ""} found",
                style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold
            )
            val strategies = state.activeStrategies.map { it.name.replace("_", " ").lowercase()
                .replaceFirstChar { c -> c.uppercase() } }.joinToString(", ")
            if (strategies.isNotEmpty()) {
                Text("Via: $strategies", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            if (state.scanDurationMs > 0) {
                Text("Completed in ${state.scanDurationMs / 1000}s",
                    style = MaterialTheme.typography.labelSmall, color = TextDisabled)
            }
        }
        state.errorMessage?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = StatusOffline)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DiscoveredDeviceCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiscoveredDeviceCard(device: DiscoveredDevice, onAddCamera: () -> Unit) {
    val icon = when (device.probableSourceType) {
        CameraSourceType.ANDROID_IPWEBCAM,
        CameraSourceType.ANDROID_DROIDCAM  -> Icons.Default.PhoneAndroid
        CameraSourceType.ONVIF             -> Icons.Default.Router
        else                               -> Icons.Default.Videocam
    }
    val confidenceColor = when (device.confidence) {
        DiscoveryConfidence.CONFIRMED -> StatusOnline
        DiscoveryConfidence.PROBABLE  -> CyanPrimary
        DiscoveryConfidence.POSSIBLE  -> WarningAmber
        DiscoveryConfidence.UNKNOWN   -> TextDisabled
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceStroke, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(CyanSubtle),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = CyanPrimary, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(12.dp))

        // Details
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                device.hostname ?: device.ipAddress,
                style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (device.hostname != null) {
                Text(device.ipAddress, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            // Badges row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                // Confidence
                item {
                    Badge(device.confidence.name.lowercase().replaceFirstChar { it.uppercase() }, confidenceColor)
                }
                // Discovery method
                item {
                    Badge(device.discoveryMethod.name.replace("_", " ").lowercase()
                        .replaceFirstChar { it.uppercase() }, TextDisabled)
                }
                // Source type
                device.probableSourceType?.let { item { Badge(it.displayName, CyanPrimary.copy(alpha = 0.7f)) } }
                // MAC vendor
                device.macVendor?.let { item { Badge(it, TextSecondary) } }
                // Manufacturer from ONVIF
                device.onvifManufacturer?.let { item { Badge(it, StatusOnline.copy(alpha = 0.8f)) } }
                // Open ports
                device.openPorts.take(3).forEach { port -> item { Badge(":$port", TextDisabled) } }
            }
            device.banner?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = TextDisabled,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.width(8.dp))

        // Add / Already Added
        if (device.isAlreadyAdded) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, null, tint = StatusOnline, modifier = Modifier.size(18.dp))
                Text("Added", style = MaterialTheme.typography.labelSmall, color = StatusOnline, fontSize = 9.sp)
            }
        } else {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(CyanSubtle)
                    .border(1.dp, CyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onAddCamera).padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text("Add", style = MaterialTheme.typography.labelMedium,
                    color = CyanPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Badge(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp)
    }
}

@Composable
private fun ScanIdlePrompt() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.Radar, null, tint = TextDisabled, modifier = Modifier.size(56.dp))
        Text("Tap Start Scan to search your LAN", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text("Uses ARP table + mDNS + ONVIF WS-Discovery + TCP probe",
            style = MaterialTheme.typography.labelSmall, color = TextDisabled)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060B10)
@Composable
private fun DiscoveryPreview() {
    SentinelTheme {
        DiscoveryContent(
            state = DiscoveryUiState(
                devices = SampleData.discoveredDevices.map {
                    it.copy(
                        discoveryMethod = if (it.port == 554) DiscoveryMethod.ONVIF_WS_DISCOVERY
                                         else DiscoveryMethod.ARP_TABLE,
                        confidence = if (it.port == 554) DiscoveryConfidence.CONFIRMED
                                     else DiscoveryConfidence.PROBABLE
                    )
                },
                isScanning   = false,
                scanComplete = true,
                scanDurationMs = 4200,
                activeStrategies = setOf(
                    DiscoveryMethod.ARP_TABLE, DiscoveryMethod.MDNS,
                    DiscoveryMethod.ONVIF_WS_DISCOVERY, DiscoveryMethod.TCP_PORT_PROBE
                ),
                capabilities = DiscoveryCapabilities(
                    wifiConnected = true,
                    multicastAvailable = true,
                    arpTableReadable = true,
                    nsdAvailable = true,
                    detectedSubnet = "192.168.1"
                )
            ),
            onNavigateBack = {}, onStartScan = {}, onCancelScan = {},
            onClearResults = {}, onAddCamera = {}
        )
    }
}
