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
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.res.painterResource
import com.sentinel.app.R
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
import com.sentinel.app.ui.components.TacticalFramePanel
import com.sentinel.app.ui.preview.SampleData
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanPrimary
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        TacticalHubTopBar(
            unreadEvents = state.recentEvents.count { !it.isRead },
            onNotificationsClick = onNavigateEvents,
            onSettingsClick = onNavigateSettings
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 116.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                SignalFilterField()
                Spacer(Modifier.height(28.dp))
            }
            item {
                RoomChipRow(rooms = feedRooms)
                Spacer(Modifier.height(34.dp))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(32.dp).height(2.dp).background(OrangePrimary))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ACTIVE_RECON_FEEDS",
                        color = OrangePrimary,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        letterSpacing = 0.sp,
                        fontSize = 20.sp
                    )
                }
            }

            if (state.pinnedCameras.isNotEmpty()) {
                items(state.pinnedCameras) { camera ->
                    ReconFeedRow(camera = camera, onOpen = { onNavigateCameraDetail(camera.id) })
                    Spacer(Modifier.height(4.dp))
                }
            } else {
                item {
                    TacticalFramePanel(leftAccent = OrangePrimary, contentPadding = 0.dp) {
                        EmptyStateView(
                            icon = Icons.Default.Timeline,
                            title = "NO_FEEDS_ACTIVE",
                            subtitle = "Add and pin cameras to populate tactical recon feed.",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            if (state.recentEvents.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYSTEM_LOGS",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanPrimary,
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                        Text(
                            text = "OPEN_LOG",
                            style = MaterialTheme.typography.labelSmall,
                            color = OrangePrimary,
                            modifier = Modifier.clickable(onClick = onNavigateEvents)
                        )
                    }
                    TacticalFramePanel(leftAccent = CyanPrimary, contentPadding = 0.dp) {
                        Column(Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)) {
                            state.recentEvents.take(3).forEachIndexed { index, event ->
                                EventRow(event = event, onClick = onNavigateEvents)
                                if (index < state.recentEvents.take(3).lastIndex) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(start = 68.dp)
                                            .height(1.dp)
                                            .background(SurfaceStroke.copy(alpha = 0.45f))
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (!state.isLoading) {
                item {
                    TacticalFramePanel(leftAccent = CyanPrimary, contentPadding = 0.dp) {
                        EmptyStateView(
                            icon = Icons.Default.Timeline,
                            title = "NO_EVENTS_LOGGED",
                            subtitle = "Motion alerts, connection changes, and snapshots will appear here",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
            item {
                QuickActionsRow(
                    onAddCamera = onNavigateAddCamera,
                    onScanNetwork = onNavigateDiscovery,
                    onMultiView = onNavigateMultiView,
                    onEvents = onNavigateEvents,
                    onSettings = onNavigateSettings
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) {
                            Box(Modifier.size(7.dp).background(CyanPrimary.copy(alpha = 0.62f)))
                        }
                    }
                    Text(
                        text = "SECURE DATA STREAM ENCRYPTED: AES-256-GCM",
                        color = CyanPrimary.copy(alpha = 0.55f),
                        fontSize = 8.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
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
            .height(80.dp)
            .background(SurfaceBase.copy(alpha = 0.9f))
            .drawBehind {
                drawRect(
                    color = SurfaceLowest,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, 6.dp.toPx())
                )
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SurfaceHighest)
                .border(2.dp, OrangePrimary.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PersonPin, null, tint = OrangePrimary)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("TACTICAL_HUB_v4.2", color = OrangePrimary, fontWeight = FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, letterSpacing = 0.sp, fontSize = 23.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 12.dp)) {
            stateLabel()
            Text(
                text = "09:42:12:04",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        }
        IconButton(onClick = onNotificationsClick, modifier = Modifier.size(36.dp)) {
            Image(painterResource(R.drawable.alerts), contentDescription = "Events", modifier = Modifier.size(28.dp))
        }
        IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.BatteryAlert, null, tint = OrangePrimary, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable private fun stateLabel() {
    Text("SYSTEM_UPTIME", color = CyanPrimary, fontSize = 10.sp, letterSpacing = 2.sp)
}

@Composable
private fun SignalFilterField() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("SIGNAL_FILTER", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, letterSpacing = 2.sp, modifier = Modifier.padding(start = 4.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLowest)
                    .height(56.dp)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = CyanPrimary.copy(alpha = 0.65f), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("QUERY_FEED_IDENTIFIER...", color = TextDisabled, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, letterSpacing = 1.sp)
            }
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(2) {
                            Box(Modifier.size(3.dp).background(TextPrimary.copy(alpha = 0.2f)))
                        }
                    }
                }
            }
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
                    .padding(horizontal = 22.dp, vertical = 8.dp)
            ) {
                Text(
                    text = room,
                    color = if (selected) BackgroundDeep else CyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    letterSpacing = 1.4.sp
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(camera.name.uppercase(), color = if (camera.isOnline) TextPrimary else TextSecondary, fontWeight = FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, letterSpacing = 0.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(camera.sourceType.name, color = CyanPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Text("LAT:${camera.healthStatus?.latencyMs ?: "---"}MS", color = TextSecondary, fontSize = 10.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (camera.isOnline) "ONLINE" else "OFFLINE", color = if (camera.isOnline) StatusOnline else StatusOffline, fontSize = 10.sp, letterSpacing = 1.sp)
                Spacer(Modifier.width(7.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(16.dp)
                        .background(SurfaceLowest)
                        .border(1.dp, if (camera.isOnline) StatusOnline.copy(alpha = 0.3f) else StatusOffline.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .width(20.dp)
                            .height(6.dp)
                            .background(if (camera.isOnline) StatusOnline else SurfaceHighest)
                    )
                }
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
                Image(
                    painter = painterResource(R.drawable.alerts),
                    contentDescription = "Events",
                    modifier = Modifier.size(24.dp),
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
            Image(painterResource(R.drawable.settings), contentDescription = "Settings", modifier = Modifier.size(24.dp))
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
            iconRes = R.drawable.add_cam,
            label = "Add Camera",
            onClick = onAddCamera,
            highlight = true,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            iconRes = R.drawable.net_config,
            label = "Scan",
            onClick = onScanNetwork,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            iconRes = R.drawable.live_view,
            label = "Multi-View",
            onClick = onMultiView,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            iconRes = R.drawable.alerts,
            label = "Events",
            onClick = onEvents,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    @DrawableRes iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier
            .background(if (highlight) OrangePrimary else SurfaceBase)
            .border(
                1.dp,
                if (highlight) OrangePrimary else CyanPrimary.copy(alpha = 0.24f)
            )
            .clickable(onClick = onClick)
            .drawBehind {
                drawRect(
                    color = if (highlight) SurfaceLowest else SurfaceStroke,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx())
                )
            }
            .padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label.uppercase().replace(" ", "_"),
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) SurfaceLowest else CyanPrimary
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
                Image(
                    painter = painterResource(R.drawable.snapshot),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
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
