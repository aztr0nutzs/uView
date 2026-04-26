package com.sentinel.companion.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.companion.data.model.Alert
import com.sentinel.companion.data.model.AlertType
import androidx.compose.ui.platform.LocalContext
import com.sentinel.companion.data.model.DeviceProfile
import com.sentinel.companion.data.model.DeviceState
import com.sentinel.companion.ui.screens.stream.StreamViewerActivity
import com.sentinel.companion.ui.components.CompanionTopBar
import com.sentinel.companion.ui.components.CornerBrackets
import com.sentinel.companion.ui.components.PulseDot
import com.sentinel.companion.ui.components.SectionCard
import com.sentinel.companion.ui.components.SectionHeader
import com.sentinel.companion.ui.components.StatCard
import com.sentinel.companion.ui.components.StatusBadge
import com.sentinel.companion.ui.components.SourceTypeBadge
import com.sentinel.companion.ui.components.TacticalLabel
import com.sentinel.companion.ui.components.alertColor
import com.sentinel.companion.ui.components.chamferClip
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.CyanSubtleBg
import com.sentinel.companion.ui.theme.CyanTertiaryDim
import com.sentinel.companion.ui.theme.ErrorRed
import com.sentinel.companion.ui.theme.GreenOnline
import com.sentinel.companion.ui.theme.OrangePrimary
import com.sentinel.companion.ui.theme.OrangeSubtle
import com.sentinel.companion.ui.theme.SurfaceBase
import com.sentinel.companion.ui.theme.SurfaceElevated
import com.sentinel.companion.ui.theme.SurfaceLowest
import com.sentinel.companion.ui.theme.SurfaceStroke
import com.sentinel.companion.ui.theme.TextDisabled
import com.sentinel.companion.ui.theme.TextPrimary
import com.sentinel.companion.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToCameras: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onAddDevice: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep),
    ) {
        CompanionTopBar(
            title       = "COMMAND_OVERVIEW",
            subtitle    = "SENTINEL_HUB // ${state.systemStatus.hostAddress.ifBlank { "LOCAL_MODE" }}",
            isConnected = state.systemStatus.isConnected,
            trailing    = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = if (state.isSyncing) OrangePrimary else TextSecondary,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(enabled = !state.isSyncing) { viewModel.refresh() },
                    )
                    BadgedBox(
                        badge = {
                            if (state.systemStatus.unreadAlerts > 0) {
                                Badge(containerColor = ErrorRed) {
                                    Text(state.systemStatus.unreadAlerts.toString(), fontSize = 8.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Alerts",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { onNavigateToAlerts() },
                        )
                    }
                }
            },
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Sync error banner (only when last sync actually failed) ───
            if (state.syncError != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ErrorRed.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "// SYNC_FAILED · ${state.syncError}",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // ── Empty state — no devices yet → only useful action is "add" ──
            if (state.isEmpty && !state.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceBase, RoundedCornerShape(12.dp))
                            .border(1.dp, SurfaceStroke, RoundedCornerShape(12.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.Videocam, null, tint = TextDisabled, modifier = Modifier.size(36.dp))
                        Text("NO_DEVICES_PAIRED", color = TextSecondary, fontSize = 12.sp,
                            fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        Text(
                            "Add a camera to start monitoring. Sentinel won't fabricate devices for you.",
                            color = TextDisabled,
                            fontSize = 11.sp,
                        )
                        Row(
                            modifier = Modifier
                                .background(OrangePrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.dp, OrangePrimary, RoundedCornerShape(8.dp))
                                .clickable(onClick = onAddDevice)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Filled.Add, null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                            Text("ADD_DEVICE", color = OrangePrimary, fontSize = 10.sp,
                                fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }

            // ── Stats row ──────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatCard(
                        label      = "TOTAL",
                        value      = state.systemStatus.totalCameras.toString(),
                        valueColor = TextPrimary,
                        modifier   = Modifier.weight(1f),
                    )
                    StatCard(
                        label      = "ONLINE",
                        value      = state.systemStatus.onlineCameras.toString(),
                        valueColor = GreenOnline,
                        modifier   = Modifier.weight(1f),
                    )
                    StatCard(
                        label      = "OFFLINE",
                        value      = state.systemStatus.offlineCameras.toString(),
                        valueColor = ErrorRed,
                        modifier   = Modifier.weight(1f),
                    )
                    StatCard(
                        label      = "ALERTS",
                        value      = state.systemStatus.unreadAlerts.toString(),
                        valueColor = if (state.systemStatus.unreadAlerts > 0) OrangePrimary else TextDisabled,
                        modifier   = Modifier.weight(1f),
                    )
                }
            }

            // ── Active recon feeds ─────────────────────────────────────────
            item {
                SectionHeader(
                    title   = "ACTIVE_RECON_FEEDS",
                    trailing = {
                        Text(
                            text = "SEE_ALL →",
                            color = CyanTertiaryDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToCameras() },
                        )
                    },
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(state.pinnedDevices) { device ->
                        FeedTile(
                            device = device,
                            onClick = { StreamViewerActivity.launch(context, device.id) },
                        )
                    }
                    if (state.pinnedDevices.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .width(200.dp)
                                    .aspectRatio(16f / 9f)
                                    .background(SurfaceBase, RoundedCornerShape(12.dp))
                                    .border(1.dp, SurfaceStroke, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("NO_ACTIVE_FEEDS", color = TextDisabled, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Quick actions ──────────────────────────────────────────────
            item { SectionHeader(title = "QUICK_ACTIONS") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickAction(icon = Icons.Filled.Add,           label = "ADD_CAM",   tint = GreenOnline,    modifier = Modifier.weight(1f), onClick = onAddDevice)
                    QuickAction(icon = Icons.Filled.NetworkCheck,  label = "SCAN_NET",  tint = CyanTertiaryDim, modifier = Modifier.weight(1f), onClick = onAddDevice)
                    QuickAction(icon = Icons.Filled.Notifications, label = "ALERTS",    tint = ErrorRed,       modifier = Modifier.weight(1f), onClick = onNavigateToAlerts)
                    QuickAction(icon = Icons.Filled.Refresh,       label = "RE_SYNC",   tint = OrangePrimary,  modifier = Modifier.weight(1f), onClick = { viewModel.refresh() })
                }
            }

            // ── Recent alerts ──────────────────────────────────────────────
            item {
                SectionHeader(
                    title   = "RECENT_ALERTS",
                    trailing = {
                        Text(
                            text = "SEE_ALL →",
                            color = CyanTertiaryDim,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToAlerts() },
                        )
                    },
                )
            }
            item {
                SectionCard {
                    if (state.recentAlerts.isEmpty()) {
                        Text("NO_RECENT_ALERTS", color = TextDisabled, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            state.recentAlerts.forEachIndexed { idx, alert ->
                                MiniAlertRow(alert = alert)
                                if (idx < state.recentAlerts.lastIndex) {
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceStroke))
                                }
                            }
                        }
                    }
                }
            }

            // ── System status ──────────────────────────────────────────────
            item { SectionHeader(title = "SYSTEM_STATUS") }
            item {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SystemMetricRow("UPTIME",       formatUptime(state.systemStatus.uptimeMs), CyanTertiaryDim)
                        SystemMetricRow("HOST_ADDRESS", state.systemStatus.hostAddress.ifBlank { "LOCAL" }, TextPrimary)
                        SystemMetricRow("LAST_SYNC",    formatRelative(state.systemStatus.lastSyncMs), TextSecondary)
                        SystemMetricRow("CONNECTING",   state.systemStatus.connectingCameras.toString(), OrangePrimary)
                        SystemMetricRow("DISABLED",     state.systemStatus.disabledCameras.toString(), TextDisabled)
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ─── Feed tile (horizontal scroll) ───────────────────────────────────────────

@Composable
private fun FeedTile(device: DeviceProfile, onClick: () -> Unit) {
    val isOnline = device.stateEnum() == DeviceState.ONLINE

    Box(
        modifier = Modifier
            .width(200.dp)
            .aspectRatio(16f / 9f)
            .chamferClip()
            .background(SurfaceElevated)
            .border(
                width = 2.dp,
                color = if (isOnline) OrangePrimary else ErrorRed.copy(alpha = 0.4f),
                shape = RoundedCornerShape(0.dp),
            )
            .clickable(onClick = onClick),
    ) {
        CornerBrackets(
            color = if (isOnline) OrangePrimary.copy(alpha = 0.7f) else ErrorRed.copy(alpha = 0.4f),
            size  = 14.dp,
            modifier = Modifier.fillMaxSize().padding(6.dp),
        )

        if (!isOnline) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.VideocamOff, null, tint = ErrorRed.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                    Text("SIGNAL_LOST", color = ErrorRed.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Videocam, null, tint = OrangePrimary.copy(alpha = 0.18f), modifier = Modifier.size(36.dp))
            }
        }

        // Bottom info bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))),
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(
                    text = device.name,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(device.protocolEnum().label, color = CyanTertiaryDim, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    if (isOnline && device.latencyMs > 0) {
                        Text("${device.latencyMs}ms", color = GreenOnline, fontSize = 8.sp)
                    }
                }
            }
        }

        // LIVE badge
        if (isOnline) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(ErrorRed.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                PulseDot(color = Color.White, size = 4.dp)
                Text("LIVE", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ─── Mini alert row ───────────────────────────────────────────────────────────

@Composable
private fun MiniAlertRow(alert: Alert) {
    val type = alert.typeEnum()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(alertColor(type), RoundedCornerShape(2.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = type.label.uppercase(),
                color = alertColor(type),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = alert.cameraName,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatRelative(alert.timestampMs),
            color = TextDisabled,
            fontSize = 10.sp,
        )
        if (!alert.isRead) {
            Box(Modifier.size(6.dp).background(OrangePrimary, RoundedCornerShape(50)))
        }
    }
}

// ─── System metric row ────────────────────────────────────────────────────────

@Composable
private fun SystemMetricRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextSecondary, fontSize = 12.sp, letterSpacing = 0.3.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─── Quick action tile ────────────────────────────────────────────────────────

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(tint.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
        )
    }
}

// ─── Time formatters ──────────────────────────────────────────────────────────

private fun formatUptime(ms: Long): String {
    if (ms <= 0L) return "—"
    val hours = ms / 3_600_000
    val mins  = (ms % 3_600_000) / 60_000
    return "${hours}h ${mins}m"
}

private fun formatRelative(ms: Long): String {
    if (ms == 0L) return "—"
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000      -> "${diff / 1000}s ago"
        diff < 3_600_000   -> "${diff / 60_000}m ago"
        diff < 86_400_000  -> "${diff / 3_600_000}h ago"
        else               -> SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(ms))
    }
}
