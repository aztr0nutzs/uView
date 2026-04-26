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
import androidx.compose.material.icons.filled.HelpOutline
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
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
import com.sentinel.app.domain.model.canBeSuggestedFromDiscovery
import com.sentinel.app.domain.model.supportInfo
import com.sentinel.app.domain.service.DiscoveryCapabilities
import com.sentinel.app.ui.components.GhostButton
import com.sentinel.app.ui.components.PrimaryButton
import com.sentinel.app.ui.preview.SampleData
import com.sentinel.app.ui.components.FastenerDots
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanPrimary
import com.sentinel.app.ui.theme.CyanSubtle
import com.sentinel.app.ui.theme.CyanTertiaryDim
import com.sentinel.app.ui.theme.GreenOnline
import com.sentinel.app.ui.theme.OrangePrimary
import com.sentinel.app.ui.theme.SentinelTheme
import com.sentinel.app.ui.theme.StatusOffline
import com.sentinel.app.ui.theme.StatusOnline
import com.sentinel.app.ui.theme.SurfaceBase
import com.sentinel.app.ui.theme.SurfaceElevated
import com.sentinel.app.ui.theme.SurfaceHighest
import com.sentinel.app.ui.theme.SurfaceLowest
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

        // ── Tactical top bar ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLowest)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceHighest)
                    .border(2.dp, OrangePrimary.copy(alpha = 0.35f))
                    .clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = OrangePrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SENTINEL_HUB // NETWORK_RECON",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = OrangePrimary,
                    letterSpacing = 1.6.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (state.isScanning) "SWEEPING_LAN…"
                    else "FIND_CAMERAS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = TextPrimary,
                    letterSpacing = (-0.2).sp
                )
            }
            if (state.isScanning) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceHighest)
                        .border(1.dp, StatusOffline.copy(alpha = 0.4f))
                        .clickable(onClick = onCancelScan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Cancel scan", tint = StatusOffline, modifier = Modifier.size(18.dp))
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
                            text     = if (state.scanComplete) "Scan Again" else "Start LAN Scan",
                            onClick  = onStartScan,
                            icon     = Icons.Default.Radar,
                            enabled  = state.capabilities?.wifiConnected != false,
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
                            "RESULTS // ${state.devices.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = OrangePrimary,
                            letterSpacing = 1.6.sp
                        )
                        if (!state.isScanning) {
                            Text(
                                "CLEAR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                color = CyanTertiaryDim,
                                letterSpacing = 1.4.sp,
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
    val accent = if (wifiOk) CyanTertiaryDim else StatusOffline
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceBase)
            .border(1.dp, accent.copy(alpha = 0.35f))
            .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!wifiOk) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.WifiOff, null, tint = StatusOffline, modifier = Modifier.size(16.dp))
                    Text(
                        "Phone isn't on Wi-Fi yet. Hop onto the network your cameras live on, then come back.",
                        style = MaterialTheme.typography.bodySmall, color = StatusOffline
                    )
                }
            } else {
                Text(
                    "SUBNET // ${caps.detectedSubnet ?: "auto"}.0/24",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = CyanTertiaryDim,
                    letterSpacing = 1.4.sp
                )
                val tools = buildList {
                    if (caps.arpTableReadable) add("ARP table")
                    if (caps.nsdAvailable)     add("mDNS")
                    if (caps.multicastAvailable) add("ONVIF discovery")
                    add("TCP port probes")
                }.joinToString(" · ")
                Text(
                    "$tools run locally only. Results are hints; stream setup still requires a supported direct RTSP, MJPEG, HLS, IP Webcam, DroidCam, or custom URL source.",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                )
            }
        }
        FastenerDots(color = accent.copy(alpha = 0.35f))
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
            StrategyChip("ONVIF_ID", DiscoveryMethod.ONVIF_WS_DISCOVERY  in activeStrategies)
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
        Text(
            "Sweeping the local network — hang tight…",
            style = MaterialTheme.typography.bodySmall,
            color = CyanPrimary.copy(alpha = alpha)
        )
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
    val count = state.devices.size
    val cameraish = state.devices.count {
        it.confidence == DiscoveryConfidence.CONFIRMED || it.confidence == DiscoveryConfidence.PROBABLE
    }
    val headline = when {
        count == 0 -> "Sweep done — nothing answered."
        cameraish == 0 -> "Sweep done — found $count device${if (count != 1) "s" else ""}, but none look like cameras."
        cameraish == count -> "Sweep done — $count look${if (cameraish == 1) "s" else ""} like camera${if (cameraish != 1) "s" else ""}."
        else -> "Sweep done — $count device${if (count != 1) "s" else ""}, $cameraish look${if (cameraish == 1) "s" else ""} like camera${if (cameraish != 1) "s" else ""}."
    }
    val accent = if (cameraish > 0) GreenOnline else if (count > 0) WarningAmber else TextDisabled

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceBase)
            .border(1.dp, accent.copy(alpha = 0.4f))
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp).background(accent.copy(alpha = 0.15f)).border(1.dp, accent.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(headline, style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary, fontWeight = FontWeight.SemiBold)
                val strategies = state.activeStrategies.joinToString(" · ") { it.shortLabel() }
                val tail = buildList {
                    if (state.scanDurationMs > 0) add("${state.scanDurationMs / 1000}s")
                    if (strategies.isNotEmpty()) add(strategies)
                }.joinToString(" · ")
                if (tail.isNotEmpty()) {
                    Text(tail, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                if (count == 0) {
                    Text(
                        "Either nothing's powered on, the cameras are on a different subnet, or they don't broadcast. Try Add Manually.",
                        style = MaterialTheme.typography.labelSmall, color = TextSecondary
                    )
                }
            }
            state.errorMessage?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = StatusOffline)
            }
        }
        FastenerDots(color = accent.copy(alpha = 0.35f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DiscoveredDeviceCard — tactical device card with reasoning + next-step
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DiscoveredDeviceCard(device: DiscoveredDevice, onAddCamera: () -> Unit) {
    val suggestedType = device.probableSourceType
    val canUseSuggestion = suggestedType?.canBeSuggestedFromDiscovery == true
    val icon = when (device.probableSourceType) {
        CameraSourceType.ANDROID_IPWEBCAM,
        CameraSourceType.ANDROID_DROIDCAM,
        CameraSourceType.ANDROID_ALFRED,
        CameraSourceType.ANDROID_CUSTOM    -> Icons.Default.PhoneAndroid
        CameraSourceType.ONVIF             -> Icons.Default.Router
        null                               -> if (device.confidence == DiscoveryConfidence.UNKNOWN)
                                                  Icons.Default.HelpOutline
                                              else Icons.Default.Videocam
        else                               -> Icons.Default.Videocam
    }
    val accent = when (device.confidence) {
        DiscoveryConfidence.CONFIRMED -> GreenOnline
        DiscoveryConfidence.PROBABLE  -> CyanTertiaryDim
        DiscoveryConfidence.POSSIBLE  -> WarningAmber
        DiscoveryConfidence.UNKNOWN   -> TextDisabled
    }
    val headline = device.classifyHeadline()
    val reasonLine = device.classifyReason()
    val nextStep = device.nextStepHint()
    val protoLine = device.protocolLine()
    val hostLine = device.hostLine()
    val reachable = device.openPorts.isNotEmpty() ||
        device.discoveryMethod == DiscoveryMethod.ONVIF_WS_DISCOVERY ||
        device.discoveryMethod == DiscoveryMethod.MDNS

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (device.isAlreadyAdded) SurfaceElevated.copy(alpha = 0.6f) else SurfaceBase)
            .border(1.dp, accent.copy(alpha = if (device.isAlreadyAdded) 0.25f else 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Top row: icon + headline + add/added action ───────────────
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(SurfaceLowest)
                        .border(1.5.dp, accent.copy(alpha = if (device.isAlreadyAdded) 0.35f else 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        headline.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = if (device.isAlreadyAdded) TextSecondary else TextPrimary,
                        letterSpacing = 0.3.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        hostLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (device.isAlreadyAdded) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null, tint = StatusOnline.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        Text("In list", color = StatusOnline.copy(alpha = 0.7f),
                            fontSize = 9.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = 0.6.sp)
                    }
                } else {
                    val actionAccent = if (canUseSuggestion) accent else WarningAmber
                    Box(
                        modifier = Modifier
                            .background(actionAccent.copy(alpha = 0.12f))
                            .border(1.dp, actionAccent.copy(alpha = 0.55f))
                            .clickable(onClick = onAddCamera)
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            if (canUseSuggestion) "SETUP" else "MANUAL",
                            color = actionAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }

            // ── Confidence + reachability ribbon ──────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConfidencePill(device.confidence, accent)
                ReachabilityPill(reachable)
                device.discoveryMethod.takeIf { it != DiscoveryMethod.MANUAL }?.let {
                    DetailChip(it.shortLabel(), TextDisabled)
                }
            }

            // ── Protocol / port readout ───────────────────────────────────
            if (protoLine.isNotEmpty()) {
                Text(
                    protoLine,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Reasoning: WHY we think this ──────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "WHY",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = accent.copy(alpha = 0.8f),
                    letterSpacing = 1.4.sp
                )
                Text(
                    reasonLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            // ── What to do next ───────────────────────────────────────────
            nextStep?.let {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "NEXT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = OrangePrimary.copy(alpha = 0.85f),
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }

            device.banner?.takeIf { it.isNotBlank() }?.let { banner ->
                Text(
                    "Banner: \"$banner\"",
                    fontSize = 10.sp,
                    color = TextDisabled,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom signal stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (device.isAlreadyAdded) 1.dp else 2.dp)
                    .background(accent.copy(alpha = if (device.isAlreadyAdded) 0.3f else 0.7f))
            )
        }
        FastenerDots(color = accent.copy(alpha = if (device.isAlreadyAdded) 0.2f else 0.45f))
    }
}

@Composable
private fun ConfidencePill(level: DiscoveryConfidence, accent: Color) {
    val text = when (level) {
        DiscoveryConfidence.CONFIRMED -> "CONFIRMED"
        DiscoveryConfidence.PROBABLE  -> "LIKELY"
        DiscoveryConfidence.POSSIBLE  -> "MAYBE"
        DiscoveryConfidence.UNKNOWN   -> "UNCLEAR"
    }
    Box(
        modifier = Modifier
            .background(accent.copy(alpha = 0.15f))
            .border(1.dp, accent.copy(alpha = 0.55f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = accent,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ReachabilityPill(reachable: Boolean) {
    val color = if (reachable) GreenOnline else WarningAmber
    val text = if (reachable) "REACHABLE" else "NO_RESPONSE"
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.5f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(color))
        Text(
            text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun DetailChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.4f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun ScanIdlePrompt() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceBase)
            .border(1.dp, CyanTertiaryDim.copy(alpha = 0.25f))
            .padding(vertical = 36.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.Radar, null, tint = OrangePrimary.copy(alpha = 0.7f), modifier = Modifier.size(48.dp))
        Text(
            "READY FOR LAN SCAN",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            color = TextPrimary,
            letterSpacing = 1.2.sp
        )
        Text(
            "Run a local discovery pass for camera-like devices on your current Wi-Fi network.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            "Discovery can identify ONVIF, mDNS, ARP, and camera-port signals. It does not auto-configure ONVIF profiles or verify stream credentials.",
            fontSize = 11.sp,
            color = TextDisabled,
            lineHeight = 15.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Classification + copy helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun DiscoveredDevice.hostLine(): String {
    val name = hostname?.takeIf { it.isNotBlank() && it != ipAddress }
    return if (name != null) "$name · $ipAddress" else ipAddress
}

private fun DiscoveredDevice.classifyHeadline(): String = when (confidence) {
    DiscoveryConfidence.CONFIRMED -> when {
        discoveryMethod == DiscoveryMethod.ONVIF_WS_DISCOVERY -> {
            val brand = onvifManufacturer?.trim()?.takeIf { it.isNotEmpty() }
            val model = onvifModel?.trim()?.takeIf { it.isNotEmpty() }
            when {
                brand != null && model != null -> "ONVIF camera · $brand $model"
                brand != null                  -> "ONVIF camera · $brand"
                else                           -> "ONVIF device"
            }
        }
        probableSourceType == CameraSourceType.ANDROID_IPWEBCAM -> "Phone running IP Webcam"
        probableSourceType == CameraSourceType.ANDROID_DROIDCAM -> "Phone running DroidCam"
        else -> "Camera detected"
    }
    DiscoveryConfidence.PROBABLE -> when (probableSourceType) {
        CameraSourceType.RTSP, CameraSourceType.ONVIF -> "Looks like an IP camera"
        CameraSourceType.MJPEG                        -> "Looks like an MJPEG camera"
        CameraSourceType.HLS                          -> "Looks like an HLS source"
        CameraSourceType.ANDROID_IPWEBCAM,
        CameraSourceType.ANDROID_DROIDCAM,
        CameraSourceType.ANDROID_ALFRED,
        CameraSourceType.ANDROID_CUSTOM               -> "Looks like a phone running a camera app"
        else                                          -> "Probably a camera"
    }
    DiscoveryConfidence.POSSIBLE -> when (probableSourceType) {
        null -> "Could be a camera"
        else -> "Maybe a ${probableSourceType.displayName.lowercase()}"
    }
    DiscoveryConfidence.UNKNOWN -> macVendor?.let { "Unknown $it device" } ?: "Unidentified device"
}

private fun DiscoveredDevice.classifyReason(): String {
    val bits = mutableListOf<String>()
    when (discoveryMethod) {
        DiscoveryMethod.ONVIF_WS_DISCOVERY -> bits += "answered ONVIF discovery; profile stream setup is not wired in this build"
        DiscoveryMethod.MDNS -> bits += mdnsServiceName
            ?.let { "broadcasting \"$it\" over mDNS" }
            ?: "advertised itself over mDNS"
        DiscoveryMethod.ARP_TABLE -> bits += "shows up on the ARP table (so it's alive on the LAN)"
        DiscoveryMethod.TCP_PORT_PROBE -> bits += "answered a TCP probe"
        DiscoveryMethod.MANUAL -> {}
    }
    val cameraPorts = openPorts.filter {
        it in setOf(554, 4747, 8080, 8081, 8554, 1935, 37777, 80, 443, 8443)
    }
    if (cameraPorts.isNotEmpty()) {
        val portNames = cameraPorts.take(4).joinToString(", ") { it.portLabel() }
        bits += "open ports: $portNames"
    } else if (openPorts.isNotEmpty()) {
        bits += "open ports: ${openPorts.take(3).joinToString(",") { it.toString() }}"
    }
    onvifModel?.takeIf { it.isNotBlank() && discoveryMethod != DiscoveryMethod.ONVIF_WS_DISCOVERY }?.let {
        bits += "model $it"
    }
    macVendor?.let {
        bits += "MAC vendor looks like $it"
    }
    val bannerHint = banner?.takeIf { it.isNotBlank() }?.let { b ->
        when {
            b.contains("rtsp", ignoreCase = true)    -> "RTSP banner on the wire"
            b.contains("droidcam", ignoreCase = true) -> "banner says DroidCam"
            b.contains("ipwebcam", ignoreCase = true) ||
            b.contains("ip webcam", ignoreCase = true) -> "banner mentions IP Webcam"
            b.contains("hikvision", ignoreCase = true) -> "Hikvision banner"
            b.contains("dahua", ignoreCase = true)    -> "Dahua banner"
            b.contains("axis", ignoreCase = true)     -> "Axis banner"
            else -> "banner: \"${b.take(40)}${if (b.length > 40) "…" else ""}\""
        }
    }
    bannerHint?.let { bits += it }
    return bits.joinToString(" · ").ifEmpty {
        "We caught it on the network but can't tell what it is yet."
    }
}

private fun DiscoveredDevice.nextStepHint(): String? {
    if (isAlreadyAdded) return "Already in your camera list — no action needed."
    val support = probableSourceType?.supportInfo
    if (support?.isSelectable == false) {
        return "${probableSourceType.displayName} cannot be auto-configured here. Use Add Manually only if you have a direct RTSP/MJPEG/HLS URL."
    }
    return when (confidence) {
        DiscoveryConfidence.CONFIRMED -> when (discoveryMethod) {
            DiscoveryMethod.ONVIF_WS_DISCOVERY ->
                "ONVIF identity is confirmed, but profile setup is unavailable. Add manually with a direct RTSP URL if you know it."
            DiscoveryMethod.MDNS ->
                "Open manual setup and use the shown host/port. No stream was decoded by discovery."
            else ->
                "Open manual setup and use the shown host/port. Discovery does not verify credentials or decode the stream."
        }
        DiscoveryConfidence.PROBABLE ->
            "Open manual setup for ${ipAddress}${if (port > 0) ":$port" else ""}. You may need a path and credentials before the stream connects."
        DiscoveryConfidence.POSSIBLE ->
            "Could be a camera, could be something else. No stream was confirmed; add manually only if you know the endpoint."
        DiscoveryConfidence.UNKNOWN ->
            "Probably not a camera. Add manually only if you know exactly what this is."
    }
}

private fun DiscoveredDevice.protocolLine(): String {
    val parts = mutableListOf<String>()
    val type = probableSourceType?.let { type ->
        when (type) {
            CameraSourceType.RTSP -> "RTSP"
            CameraSourceType.ONVIF -> "ONVIF identity only"
            CameraSourceType.MJPEG -> "MJPEG/HTTP"
            CameraSourceType.HLS   -> "HLS"
            CameraSourceType.ANDROID_DROIDCAM -> "DroidCam"
            CameraSourceType.ANDROID_IPWEBCAM -> "IP Webcam"
            CameraSourceType.ANDROID_ALFRED   -> "Alfred"
            CameraSourceType.ANDROID_CUSTOM   -> "Phone (custom)"
            else -> null
        }
    }
    if (type != null) parts += type
    if (port > 0) parts += "port $port"
    return if (parts.isNotEmpty()) "Protocol: ${parts.joinToString(" · ")}" else ""
}

private fun Int.portLabel(): String = when (this) {
    554   -> "RTSP/554"
    8554  -> "RTSP/8554"
    4747  -> "DroidCam/4747"
    8080  -> "HTTP/8080"
    8081  -> "HTTP/8081"
    1935  -> "RTMP/1935"
    37777 -> "Dahua/37777"
    80    -> "HTTP/80"
    443   -> "HTTPS/443"
    8443  -> "HTTPS/8443"
    else  -> ":$this"
}

private fun DiscoveryMethod.shortLabel(): String = when (this) {
    DiscoveryMethod.ARP_TABLE          -> "ARP"
    DiscoveryMethod.MDNS               -> "mDNS"
    DiscoveryMethod.ONVIF_WS_DISCOVERY -> "ONVIF id"
    DiscoveryMethod.TCP_PORT_PROBE     -> "TCP probe"
    DiscoveryMethod.MANUAL             -> "manual"
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
