package com.sentinel.app.features.dashboard

// UI AUTHORITY: This dashboard surface is governed by root `uview_screen1.html`.
// Preserve its tactical HUD colorway, density, framing, typography feel, glow,
// edge treatment, and layout rhythm. Deviations require SCREEN_AUTHORITY_MAP.md review.

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.DashboardSummary
import com.sentinel.app.domain.model.PlayerState
import com.sentinel.app.ui.components.CameraFeedTile
import com.sentinel.app.ui.components.ChamferThumbnail
import com.sentinel.app.ui.components.EmptyStateView
import com.sentinel.app.ui.components.EventRow
import com.sentinel.app.ui.components.SectionCard
import com.sentinel.app.ui.preview.SampleData
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanPrimary
import com.sentinel.app.ui.theme.CyanSubtle
import com.sentinel.app.ui.theme.OrangePrimary
import com.sentinel.app.ui.theme.RecordingRed
import com.sentinel.app.ui.theme.SentinelTheme
import com.sentinel.app.ui.theme.SurfaceLow
import com.sentinel.app.ui.theme.SurfaceBase
import com.sentinel.app.ui.theme.SurfaceLowest
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
fun DashboardScreen(
    onNavigateCameras: () -> Unit,
    onNavigateMultiView: () -> Unit,
    onNavigateAddCamera: () -> Unit,
    onNavigateEvents: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateDiscovery: () -> Unit,
    onNavigateCameraDetail: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardContent(
        state = state,
        onNavigateCameras = onNavigateCameras,
        onNavigateMultiView = onNavigateMultiView,
        onNavigateAddCamera = onNavigateAddCamera,
        onNavigateEvents = onNavigateEvents,
        onNavigateSettings = onNavigateSettings,
        onNavigateDiscovery = onNavigateDiscovery,
        onNavigateCameraDetail = onNavigateCameraDetail
    )
}

@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onNavigateCameras: () -> Unit,
    onNavigateMultiView: () -> Unit,
    onNavigateAddCamera: () -> Unit,
    onNavigateEvents: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateDiscovery: () -> Unit,
    onNavigateCameraDetail: (String) -> Unit
) {
    val feedRooms = buildList {
        add("ALL_NODES")
        state.pinnedCameras.map { it.room.ifBlank { "UNASSIGNED" } }
            .distinct()
            .take(4)
            .forEach { add(it.uppercase().replace(" ", "_")) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TacticalHubTopBar(unreadEvents = state.recentEvents.count { !it.isRead }, onNavigateEvents, onNavigateSettings)
        }
        item {
            SignalFilterField()
        }
        item {
            RoomChipRow(rooms = feedRooms)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(32.dp).height(2.dp).background(OrangePrimary))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "ACTIVE_RECON_FEEDS",
                    color = OrangePrimary,
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    letterSpacing = (-0.2).sp,
                    fontSize = 20.sp
                )
            }
        }

        if (state.pinnedCameras.isNotEmpty()) {
            items(state.pinnedCameras) { camera ->
                ReconFeedRow(camera = camera, onOpen = { onNavigateCameraDetail(camera.id) })
            }
        } else {
            item {
                SectionCard {
                    EmptyStateView(
                        icon = Icons.Default.Timeline,
                        title = "NO_FEEDS_ACTIVE",
                        subtitle = "Add and pin cameras to populate tactical recon feed.",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (state.recentEvents.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT EVENTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanPrimary,
                        modifier = Modifier.clickable(onClick = onNavigateEvents)
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            item {
                SectionCard {
                    state.recentEvents.forEachIndexed { index, event ->
                        EventRow(event = event, onClick = onNavigateEvents)
                        if (index < state.recentEvents.lastIndex) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 68.dp)
                                    .height(1.dp)
                                    .background(SurfaceStroke)
                            )
                        }
                    }
                }
            }
        } else if (!state.isLoading) {
            item {
                SectionCard(title = "Recent Events") {
                    EmptyStateView(
                        icon = Icons.Default.Timeline,
                        title = "No events yet",
                        subtitle = "Motion alerts, connection changes, and snapshots will appear here",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            QuickActionsRow(
                onAddCamera = onNavigateAddCamera,
                onScanNetwork = onNavigateDiscovery,
                onMultiView = onNavigateMultiView,
                onEvents = onNavigateEvents,
                onSettings = onNavigateSettings
            )
        }
    }
}

@Composable
private fun TacticalHubTopBar(
    unreadEvents: Int,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceBase.copy(alpha = 0.9f))
            .drawBehind {
                drawRect(
                    color = SurfaceLow,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx())
                )
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SurfaceHighest)
                .border(2.dp, OrangePrimary.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Notifications, null, tint = OrangePrimary)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("TACTICAL_HUB_v4.2", color = OrangePrimary, fontWeight = FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, letterSpacing = (-0.4).sp, fontSize = 24.sp)
            stateLabel()
        }
        IconButton(onClick = onNotificationsClick) {
            Icon(if (unreadEvents > 0) Icons.Default.Notifications else Icons.Default.NotificationsNone, null, tint = OrangePrimary)
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.BatteryAlert, null, tint = OrangePrimary)
        }
    }
}

@Composable private fun stateLabel() {
    Text("SYSTEM_UPTIME", color = CyanPrimary, fontSize = 10.sp, letterSpacing = 2.sp)
}

@Composable
private fun SignalFilterField() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("SIGNAL_FILTER", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLowest)
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = CyanPrimary.copy(alpha = 0.7f))
            Spacer(Modifier.width(8.dp))
            Text("QUERY_FEED_IDENTIFIER...", color = TextDisabled, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        }
    }
}

@Composable
private fun RoomChipRow(rooms: List<String>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(rooms) { room ->
            val selected = room == "ALL_NODES"
            Box(
                modifier = Modifier
                    .background(if (selected) OrangePrimary else SurfaceElevated, RoundedCornerShape(2.dp))
                    .border(1.dp, if (selected) OrangePrimary else CyanPrimary.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = room,
                    color = if (selected) BackgroundDeep else CyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

@Composable
private fun ReconFeedRow(camera: CameraDevice, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (camera.isOnline) SurfaceBase else SurfaceBase.copy(alpha = 0.55f))
            .drawBehind {
                drawRect(
                    color = SurfaceLowest,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, 6.dp.toPx())
                )
            }
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChamferThumbnail(status = camera.displayStatus) {
            Box(Modifier.fillMaxSize().background(SurfaceHighest), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (camera.isOnline) Icons.Default.GridView else Icons.Default.Settings,
                    contentDescription = null,
                    tint = if (camera.isOnline) CyanPrimary else TextDisabled
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(camera.name.uppercase(), color = if (camera.isOnline) TextPrimary else TextSecondary, fontWeight = FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(camera.sourceType.name, color = CyanPrimary, fontSize = 10.sp)
                Spacer(Modifier.width(8.dp))
                Text("LAT:${camera.healthStatus?.latencyMs ?: "---"}MS", color = TextSecondary, fontSize = 10.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Circle, null, modifier = Modifier.size(8.dp), tint = if (camera.isOnline) StatusOnline else StatusOffline)
                Spacer(Modifier.width(4.dp))
                Text(if (camera.isOnline) "ONLINE" else "OFFLINE", color = if (camera.isOnline) StatusOnline else StatusOffline, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Text("UUID:${camera.id.take(8).uppercase()}", fontSize = 9.sp, color = TextDisabled)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DashboardTopBar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardTopBar(
    unreadEvents: Int,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sentinel",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Home Security Hub",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        // Notification bell with unread badge
        Box {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = if (unreadEvents > 0) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                    contentDescription = "Events",
                    tint = if (unreadEvents > 0) CyanPrimary else TextSecondary
                )
            }
            if (unreadEvents > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RecordingRed)
                        .align(Alignment.TopEnd)
                        .padding(end = 2.dp, top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadEvents > 9) "9+" else unreadEvents.toString(),
                        fontSize = 8.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DashboardStatRow — the 4-card summary strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardStatRow(summary: DashboardSummary, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            value = summary.totalCameras.toString(),
            label = "Total",
            color = CyanPrimary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = summary.onlineCameras.toString(),
            label = "Online",
            color = StatusOnline,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = summary.offlineCameras.toString(),
            label = "Offline",
            color = StatusOffline,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            value = summary.recentEventCount.toString(),
            label = "Events",
            color = WarningAmber,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QuickActionsRow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(
    onAddCamera: () -> Unit,
    onScanNetwork: () -> Unit,
    onMultiView: () -> Unit,
    onEvents: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionButton(
            icon = Icons.Default.Add,
            label = "Add Camera",
            onClick = onAddCamera,
            highlight = true,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.Radar,
            label = "Scan",
            onClick = onScanNetwork,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.GridView,
            label = "Multi-View",
            onClick = onMultiView,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.Timeline,
            label = "Events",
            onClick = onEvents,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlight) CyanSubtle else SurfaceElevated)
            .border(
                1.dp,
                if (highlight) CyanPrimary.copy(alpha = 0.4f) else SurfaceStroke,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (highlight) CyanPrimary else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) CyanPrimary else TextSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StorageStatusCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StorageStatusCard(summary: DashboardSummary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceStroke, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Local Storage",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "${formatMb(summary.storageUsedMb)} / ${formatMb(summary.storageCapacityMb)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { summary.storagePercent },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when {
                summary.storagePercent > 0.9f -> StatusOffline
                summary.storagePercent > 0.7f -> WarningAmber
                else -> CyanPrimary
            },
            trackColor = SurfaceHighest
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${(summary.storagePercent * 100).toInt()}% used — Recording available",
            style = MaterialTheme.typography.labelSmall,
            color = TextDisabled
        )
    }
}

private fun formatMb(mb: Long): String = when {
    mb >= 1024 -> "${mb / 1024} GB"
    else       -> "$mb MB"
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF060B10)
@Composable
private fun DashboardPreview() {
    SentinelTheme {
        DashboardContent(
            state = DashboardUiState(
                summary = SampleData.dashboardSummary,
                pinnedCameras = listOf(SampleData.frontDoorCamera, SampleData.livingRoomPhone),
                recentEvents = SampleData.sampleEvents.take(3),
                isLoading = false
            ),
            onNavigateCameras = {},
            onNavigateMultiView = {},
            onNavigateAddCamera = {},
            onNavigateEvents = {},
            onNavigateSettings = {},
            onNavigateDiscovery = {},
            onNavigateCameraDetail = {}
        )
    }
}
