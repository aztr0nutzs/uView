package com.sentinel.app.features.multiview

// UI AUTHORITY: This multi-view tactical grid is governed by root
// `uview_screen2.html`. Preserve its grid density, hard panel framing,
// live/offline overlays, HUD modules, glow accents, and tactical typography.
// Deviations require SCREEN_AUTHORITY_MAP.md review.

import androidx.annotation.OptIn
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import com.sentinel.app.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.PlayerState
import com.sentinel.app.ui.components.LiveStreamSurface
import com.sentinel.app.ui.components.FastenerDots
import com.sentinel.app.ui.components.TacticalBadge
import com.sentinel.app.ui.components.TacticalFramePanel
import com.sentinel.app.ui.components.TacticalOnlineBar
import com.sentinel.app.ui.components.PrimaryButton
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanAccent
import com.sentinel.app.ui.theme.CyanTertiaryDim
import com.sentinel.app.ui.theme.ErrorContainer
import com.sentinel.app.ui.theme.ErrorDim
import com.sentinel.app.ui.theme.ErrorRed
import com.sentinel.app.ui.theme.GreenOnline
import com.sentinel.app.ui.theme.OrangePrimary
import com.sentinel.app.ui.theme.SurfaceBase
import com.sentinel.app.ui.theme.SurfaceElevated
import com.sentinel.app.ui.theme.SurfaceHighest
import com.sentinel.app.ui.theme.SurfaceLowest
import com.sentinel.app.ui.theme.SurfaceStroke
import com.sentinel.app.ui.theme.TextDisabled
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary

@OptIn(UnstableApi::class)
@Composable
fun MultiViewScreen(
    onNavigateBack: () -> Unit,
    onNavigateCameraDetail: (String) -> Unit,
    onNavigateAddCamera: () -> Unit,
    viewModel: MultiViewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDeep)) {

        // ── TACTICAL_GRID header ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .background(Color(0xE8111111))
                .drawBehind {
                    drawRect(Color(0xFF0A0A0A), topLeft = Offset(0f, size.height - 4.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()))
                }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(painterResource(R.drawable.uview_icon), contentDescription = null, modifier = Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "TACTICAL_GRID_v4.2",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle  = FontStyle.Italic,
                    color      = OrangePrimary,
                    letterSpacing = (-0.5).sp
                )
                TacticalOnlineBar(
                    online = state.cameras.count { it.isOnline },
                    total  = state.cameras.size,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Grid layout selector
            Row(
                modifier = Modifier
                    .background(SurfaceLowest)
                    .padding(4.dp)
                    .drawBehind {
                        drawRect(OrangePrimary.copy(alpha = 0.3f),
                            topLeft = Offset(0f, 0f),
                            size    = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
                    },
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                listOf(
                    Triple(GridLayout.SINGLE, Icons.Default.CropSquare, "1×1"),
                    Triple(GridLayout.TWO_BY_TWO, Icons.Default.GridView, "2×2"),
                    Triple(GridLayout.THREE_BY_THREE, Icons.Default.Apps, "3×3")
                ).forEach { (layout, icon, _) ->
                    val selected = state.gridLayout == layout
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(if (selected) SurfaceElevated else Color.Transparent)
                            .clickable { viewModel.setLayout(layout) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null,
                            tint = if (selected) CyanAccent else TextDisabled,
                            modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Mute toggle
            IconButton(onClick = viewModel::toggleMute, modifier = Modifier.size(36.dp)) {
                Icon(if (state.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    null, tint = if (state.isMuted) OrangePrimary else TextSecondary,
                    modifier = Modifier.size(22.dp))
            }

            // RECONNECT ALL button
            Box(
                modifier = Modifier
                    .background(OrangePrimary)
                    .clickable(onClick = viewModel::refreshAll)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("DEPLOY", fontSize = 10.sp, fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic, color = Color(0xFF111111), letterSpacing = 0.5.sp)
            }
        }

        LazyColumn(
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier.weight(1f)
        ) {
            // ── Feed grid ─────────────────────────────────────────────────
            item {
                if (state.cameras.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Image(painterResource(R.drawable.devices_lite), contentDescription = null, modifier = Modifier.size(72.dp))
                            Text("NO_FEEDS_ACTIVE", fontSize = 14.sp, fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic, color = TextDisabled)
                            PrimaryButton("+ ADD_CAMERA", onClick = onNavigateAddCamera, icon = Icons.Default.Add)
                        }
                    }
                } else {
                    val cols = state.gridLayout.columns
                    val rows = (state.cameras.size + cols - 1) / cols
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        (0 until rows).forEach { rowIdx ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                (0 until cols).forEach { colIdx ->
                                    val cameraIdx = rowIdx * cols + colIdx
                                    val camera = state.cameras.getOrNull(cameraIdx)
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (camera != null) {
                                            val playerState by viewModel.observePlayerState(camera.id)
                                                .collectAsStateWithLifecycle(PlayerState.Idle)
                                            TacticalFeedTile(
                                                camera      = camera,
                                                playerState = playerState,
                                                isSelected  = state.selectedCameraId == camera.id,
                                                exoPlayer   = viewModel.getExoPlayer(camera.id),
                                                mjpegFrames = viewModel.getMjpegFrames(camera.id),
                                                onTap       = { viewModel.selectCamera(if (state.selectedCameraId == camera.id) null else camera.id) },
                                                onFullscreen = { onNavigateCameraDetail(camera.id) },
                                                onReconnect  = { viewModel.reconnect(camera.id) }
                                            )
                                        } else {
                                            // Empty slot — matches vacant grid cell
                                            Box(modifier = Modifier.aspectRatio(16f/9f)
                                                .background(SurfaceLowest)
                                                .border(1.dp, SurfaceStroke))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── HUD modules row — 3 columns ───────────────────────────────
            if (state.cameras.isNotEmpty()) {
                item {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ZONE_TELEMETRY
                        HudModule(
                            title       = "SQUAD_TELEMETRY",
                            borderColor = OrangePrimary,
                            modifier    = Modifier.weight(1f)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                state.cameras.take(3).forEach { cam ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cam.name.uppercase().take(14),
                                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                            fontStyle = FontStyle.Italic, color = TextSecondary)
                                        TacticalBadge(
                                            text  = if (cam.isOnline) "ACTIVE" else "OFFLINE",
                                            color = if (cam.isOnline) GreenOnline else ErrorRed
                                        )
                                    }
                                }
                            }
                        }

                        // SYSTEM_LOGS — last 2 events placeholder, real data from events repo
                        HudModule(
                            title       = "SYSTEM_LOGS",
                            borderColor = CyanTertiaryDim,
                            modifier    = Modifier.weight(1.2f)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.recentEvents.take(2).forEach { event ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .drawBehind {
                                                drawRect(SurfaceStroke.copy(alpha = 0.3f),
                                                    topLeft = Offset(0f, size.height - 1.dp.toPx()),
                                                    size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
                                            }
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            formatLogTime(event.timestampMs),
                                            fontSize = 8.sp, color = CyanTertiaryDim,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            modifier = Modifier.width(48.dp)
                                        )
                                        Text(
                                            "${event.cameraName}: ${event.eventType.displayName}",
                                            fontSize = 10.sp, color = TextSecondary,
                                            modifier = Modifier.weight(1f), maxLines = 2
                                        )
                                    }
                                }
                                if (state.recentEvents.isEmpty()) {
                                    Text("NO_EVENTS_LOGGED", fontSize = 10.sp,
                                        color = TextDisabled, fontStyle = FontStyle.Italic)
                                }
                            }
                        }

                        // SENSOR_ARRAYS — real power state
                        HudModule(
                            title       = "SENSOR_ARRAYS",
                            borderColor = GreenOnline,
                            modifier    = Modifier.weight(1f)
                        ) {
                            val power = state.powerState
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SensorReadout("FEEDS", "${state.cameras.count { it.isOnline }}", GreenOnline)
                                Box(Modifier.width(1.dp).height(36.dp).background(SurfaceStroke.copy(alpha = 0.3f)))
                                SensorReadout("SIGNAL",
                                    power?.let { if (!it.isOnMeteredNetwork) "LAN" else "WAN" } ?: "---",
                                    CyanTertiaryDim)
                                Box(Modifier.width(1.dp).height(36.dp).background(SurfaceStroke.copy(alpha = 0.3f)))
                                SensorReadout("BATTERY",
                                    power?.let { "${it.batteryPercent}%" } ?: "---%",
                                    if ((power?.batteryPercent ?: 100) < 20 && power?.isCharging == false)
                                        ErrorRed else GreenOnline)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
// ─────────────────────────────────────────────────────────────────────────────
// TacticalFeedTile — matches screen 2 camera tile
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
private fun TacticalFeedTile(
    camera: CameraDevice,
    playerState: PlayerState,
    isSelected: Boolean,
    exoPlayer: androidx.media3.exoplayer.ExoPlayer?,
    mjpegFrames: kotlinx.coroutines.flow.Flow<android.graphics.Bitmap>?,
    onTap: () -> Unit,
    onFullscreen: () -> Unit,
    onReconnect: () -> Unit
) {
    val isOffline = playerState is PlayerState.Error || playerState is PlayerState.Idle &&
                    camera.displayStatus == com.sentinel.app.domain.model.CameraStatus.OFFLINE

    Box(
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .border(4.dp,
                when {
                    isSelected -> OrangePrimary
                    isOffline  -> ErrorContainer.copy(alpha = 0.4f)
                    else       -> SurfaceHighest
                })
            .background(if (isOffline) Color.Black else SurfaceBase)
            .clickable(onClick = onTap)
    ) {
        // Corner fastener dots (decorative — 4 corners)
        FastenerDots(color = if (isOffline) ErrorDim else SurfaceStroke)

        if (isOffline) {
            // SIGNAL_LOST state
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.WifiOff, null, tint = ErrorRed,
                    modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(8.dp))
                Text("SIGNAL_LOST", fontSize = 18.sp, fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic, color = ErrorDim, letterSpacing = (-0.5).sp)
                Spacer(Modifier.height(4.dp))
                Text("ATTEMPTING RECONNECT...", fontSize = 8.sp,
                    color = ErrorRed.copy(alpha = 0.6f), letterSpacing = 2.sp)
            }
            // CRITICAL_FAILURE badge top-right
            TacticalBadge("CRITICAL_FAILURE", ErrorRed, filled = true,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        } else {
            // Live stream surface
            LiveStreamSurface(
                playerState = playerState,
                exoPlayer   = exoPlayer,
                mjpegFrames = mjpegFrames,
                modifier    = Modifier.fillMaxSize()
            )

            // LIVE badge top-left with left-border red pill style
            if (playerState is PlayerState.Playing) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(start = 4.dp, color = ErrorRed)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Pulsing dot
                    val alpha by rememberPulse()
                    Box(Modifier.size(6.dp).clip(CircleShape).background(ErrorRed.copy(alpha = alpha)))
                    Text("LIVE", fontSize = 9.sp, fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic, color = TextPrimary)
                }
            }

            // Status badge top-right (motion detection, secure link, etc.)
            val statusBadge = when {
                playerState is PlayerState.Playing &&
                camera.healthStatus?.latencyMs != null -> "SECURE_LINK_ESTABLISHED"
                else -> null
            }
            statusBadge?.let {
                TacticalBadge(it, GreenOnline, filled = true,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
        }

        // Bottom info bar — always present
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    "UNIT_ID: ${camera.id.take(8).uppercase()}",
                    fontSize = 8.sp, color = CyanTertiaryDim, letterSpacing = 1.5.sp
                )
                Text(
                    camera.name.uppercase(),
                    fontSize = 15.sp, fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic, color = TextPrimary, letterSpacing = (-0.3).sp
                )
            }
            Text(
                camera.preferredQuality.label.uppercase(),
                fontSize  = 9.sp,
                color     = TextSecondary,
                modifier  = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun rememberPulse(): androidx.compose.runtime.State<Float> {
    val t = rememberInfiniteTransition(label = "livePulse")
    return t.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// HudModule — the 3 bottom cards from screen 2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HudModule(
    title: String,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TacticalFramePanel(
        modifier = modifier,
        leftAccent = borderColor,
        fill = if (borderColor == GreenOnline) SurfaceElevated else SurfaceBase,
        contentPadding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic, color = borderColor, letterSpacing = 1.5.sp)
            content()
        }
    }
}

@Composable
private fun SensorReadout(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 8.sp, color = TextSecondary, letterSpacing = 0.5.sp)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic, color = color)
    }
}

@Composable
private fun Modifier.border(start: Dp, color: Color): Modifier = this.drawBehind {
    drawRect(color, topLeft = Offset(0f, 0f),
        size = androidx.compose.ui.geometry.Size(start.toPx(), size.height))
}

private fun formatLogTime(ms: Long): String {
    val h = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(ms) % 24
    val m = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    val now = System.currentTimeMillis()
    val diff = now - ms
    val actualH = (diff / 3600000).toInt()
    val actualM = ((diff % 3600000) / 60000).toInt()
    val actualS = ((diff % 60000) / 1000).toInt()
    return "%02d:%02d:%02d".format(actualH, actualM, actualS) + " ago".take(0)
        .let { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(ms)) }
}
