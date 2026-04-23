package com.sentinel.app.features.cameras.detail

import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.ConnectionTestResult
import com.sentinel.app.domain.model.PlayerState
import com.sentinel.app.domain.model.RecordingState
import com.sentinel.app.ui.components.CornerBrackets
import com.sentinel.app.ui.components.EventRow
import com.sentinel.app.ui.components.HudStatRow
import com.sentinel.app.ui.components.LiveRecBadge
import com.sentinel.app.ui.components.LiveStreamSurface
import com.sentinel.app.ui.components.PrimaryButton
import com.sentinel.app.ui.components.TacticalActionButton
import com.sentinel.app.ui.components.TacticalBadge
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanAccent
import com.sentinel.app.ui.theme.CyanTertiaryDim
import com.sentinel.app.ui.theme.ErrorContainer
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
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun CameraDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateSettings: (String) -> Unit,
    viewModel: CameraDetailViewModel = hiltViewModel()
) {
    val state          by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState    by viewModel.playerState.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) { onDispose { viewModel.stopPlayback() } }

    if (state.isLoading || state.camera == null) {
        Box(Modifier.fillMaxSize().background(BackgroundDeep), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OrangePrimary)
        }
        return
    }

    val camera = state.camera!!
    var isMuted by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDeep)) {

        // ── TACTICAL navigation header ────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xE8111111))
                .drawBehind {
                    drawRect(OrangePrimary.copy(alpha = 0.1f),
                        topLeft = Offset(0f, size.height - 4.dp.toPx()),
                        size    = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()))
                }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = OrangePrimary, modifier = Modifier.size(22.dp))
            }
            Text(
                camera.name.uppercase().replace(" ", "_"),
                fontSize   = 16.sp,
                fontWeight = FontWeight.Black,
                fontStyle  = FontStyle.Italic,
                color      = OrangePrimary,
                letterSpacing = (-0.3).sp,
                modifier   = Modifier.weight(1f)
            )
            IconButton(onClick = { onNavigateSettings(camera.id) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Tune, "Settings", tint = OrangePrimary, modifier = Modifier.size(22.dp))
            }
        }

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier.weight(1f)
        ) {
            // ── Live feed — 16:9 with HUD overlays ───────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .border(4.dp, SurfaceHighest)
                        .background(Color.Black)
                ) {
                    // Stream renderer
                    LiveStreamSurface(
                        playerState = playerState,
                        exoPlayer   = viewModel.getExoPlayer(),
                        mjpegFrames = viewModel.getMjpegFrames(),
                        modifier    = Modifier.fillMaxSize()
                    )

                    // Corner brackets
                    CornerBrackets(color = OrangePrimary.copy(alpha = 0.5f))

                    // Error overlay
                    if (playerState is PlayerState.Error) {
                        Box(Modifier.fillMaxSize().background(ErrorContainer.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Error, null, tint = ErrorRed, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text((playerState as PlayerState.Error).message,
                                    fontSize = 11.sp, color = ErrorRed, fontStyle = FontStyle.Italic)
                                Spacer(Modifier.height(12.dp))
                                PrimaryButton("RECONNECT", onClick = viewModel::reconnect, icon = Icons.Default.Sync)
                            }
                        }
                    }

                    // Top-left: Cam_ID HUD — orange left-border pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(SurfaceLowest.copy(alpha = 0.8f))
                            .drawBehind {
                                drawRect(OrangePrimary,
                                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height))
                            }
                            .padding(start = 10.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Column {
                            Text("CAM_ID", fontSize = 8.sp, color = OrangePrimary, letterSpacing = 1.5.sp)
                            Text(
                                "${camera.name.uppercase()} — ${camera.room.uppercase()}",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontStyle  = FontStyle.Italic,
                                color      = TextPrimary
                            )
                        }
                    }

                    // Top-right: quality + encryption badges
                    Column(
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TacticalBadge("LIVE // ${camera.preferredQuality.label.uppercase()}", GreenOnline, filled = true,
                            modifier = Modifier.drawBehind {
                                drawRect(GreenOnline,
                                    topLeft = Offset(size.width - 2.dp.toPx(), 0f),
                                    size    = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
                            })
                        TacticalBadge(camera.sourceType.displayName.uppercase().take(16), CyanTertiaryDim,
                            modifier = Modifier.drawBehind {
                                drawRect(CyanTertiaryDim,
                                    topLeft = Offset(size.width - 2.dp.toPx(), 0f),
                                    size    = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
                            })
                    }

                    // Bottom-left: real telemetry readouts
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                            .background(SurfaceLowest.copy(alpha = 0.6f))
                            .padding(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column {
                                Text("BITRATE", fontSize = 7.sp, color = TextSecondary, letterSpacing = 0.5.sp)
                                Text(
                                    camera.healthStatus?.streamBitrateKbps?.let { "${it/1000.0f} MBPS" } ?: "-- MBPS",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic, color = CyanTertiaryDim
                                )
                            }
                            Column {
                                Text("LATENCY", fontSize = 7.sp, color = TextSecondary, letterSpacing = 0.5.sp)
                                Text(
                                    camera.healthStatus?.latencyMs?.let { "${it} MS" } ?: "-- MS",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic, color = CyanTertiaryDim
                                )
                            }
                        }
                    }

                    // Bottom-right: REC indicator (when recording)
                    if (recordingState == RecordingState.RECORDING) {
                        LiveRecBadge(modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp))
                    }
                }
            }

            // ── Tactical Action Strip — 3+3 grid ─────────────────────────
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    TacticalActionButton(
                        icon   = Icons.Default.CameraAlt,
                        label  = "SNAPSHOT",
                        onClick = viewModel::takeSnapshot,
                        iconColor = CyanTertiaryDim,
                        loading  = state.isTakingSnapshot,
                        modifier = Modifier.weight(1f)
                    )
                    TacticalActionButton(
                        icon       = Icons.Default.FiberManualRecord,
                        label      = if (recordingState == RecordingState.RECORDING) "RECORDING" else "RECORD",
                        onClick    = viewModel::toggleRecording,
                        iconColor  = ErrorRed,
                        isActive   = recordingState == RecordingState.RECORDING,
                        activePulse = recordingState == RecordingState.RECORDING,
                        modifier   = Modifier.weight(1f)
                    )
                    TacticalActionButton(
                        icon      = Icons.Default.Sync,
                        label     = "RECONNECT",
                        onClick   = viewModel::reconnect,
                        iconColor = GreenOnline,
                        modifier  = Modifier.weight(1f)
                    )
                    TacticalActionButton(
                        icon      = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        label     = if (isMuted) "UNMUTE" else "MUTE",
                        onClick   = { isMuted = viewModel.toggleMute() },
                        iconColor = TextPrimary,
                        modifier  = Modifier.weight(1f)
                    )
                    TacticalActionButton(
                        icon      = if (camera.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        label     = "FAVORITE",
                        onClick   = viewModel::toggleFavorite,
                        iconColor = OrangePrimary,
                        isActive  = camera.isFavorite,
                        modifier  = Modifier.weight(1f)
                    )
                    TacticalActionButton(
                        icon      = Icons.Default.Settings,
                        label     = "SETTINGS",
                        onClick   = { onNavigateSettings(camera.id) },
                        iconColor = TextPrimary,
                        modifier  = Modifier.weight(1f)
                    )
                }
            }

            // ── Tab system ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(SurfaceHighest,
                                topLeft = Offset(0f, size.height - 4.dp.toPx()),
                                size    = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()))
                        }
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    DetailTab.values().forEach { tab ->
                        val selected = tab == state.selectedTab
                        Box(
                            modifier = Modifier
                                .clickable { viewModel.selectTab(tab) }
                                .padding(bottom = 12.dp)
                                .drawBehind {
                                    if (selected) {
                                        drawRect(OrangePrimary,
                                            topLeft = Offset(0f, size.height - 4.dp.toPx()),
                                            size    = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()))
                                    }
                                }
                        ) {
                            Text(
                                tab.label,
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontStyle  = FontStyle.Italic,
                                color      = if (selected) OrangePrimary else TextSecondary,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    }
                }
            }

            // ── Tab content ───────────────────────────────────────────────
            when (state.selectedTab) {
                DetailTab.LIVE -> {
                    // 2-column grid: STREAM_PERFORMANCE + CAMERA_CONFIGURATION
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // STREAM_PERFORMANCE
                            DetailCard(title = "STREAM_PERFORMANCE", borderColor = CyanTertiaryDim,
                                modifier = Modifier.weight(1f)) {
                                HudStatRow("PROTOCOL", camera.sourceType.displayName.uppercase(),
                                    valueColor = TextPrimary)
                                HudStatRow("BITRATE",
                                    camera.healthStatus?.streamBitrateKbps?.let { "${it/1000.0f} MBPS" } ?: "--",
                                    valueColor = CyanTertiaryDim)
                                HudStatRow("LATENCY",
                                    camera.healthStatus?.latencyMs?.let { "${it}MS" } ?: "--",
                                    valueColor = GreenOnline)
                                HudStatRow("STATUS",
                                    when (playerState) {
                                        is PlayerState.Playing     -> "LIVE_STABLE"
                                        is PlayerState.Buffering   -> "BUFFERING"
                                        is PlayerState.Reconnecting -> "RECONNECTING"
                                        is PlayerState.Error       -> "FAULT_STATE"
                                        else                       -> "STANDBY"
                                    },
                                    valueColor = when (playerState) {
                                        is PlayerState.Playing -> GreenOnline
                                        is PlayerState.Error   -> ErrorRed
                                        else                   -> TextSecondary
                                    })
                            }
                            // CAMERA_CONFIGURATION
                            DetailCard(title = "CAMERA_CONFIG", borderColor = OrangePrimary,
                                modifier = Modifier.weight(1f)) {
                                HudStatRow("HOST", camera.connectionProfile.host.ifBlank { "--" })
                                HudStatRow("PORT", camera.connectionProfile.port.toString())
                                HudStatRow("QUALITY", camera.preferredQuality.label.uppercase(),
                                    valueColor = OrangePrimary)
                                HudStatRow("MOTION",
                                    state.motionDetectorState.javaClass.simpleName
                                        .replace("MotionDetectorState.", "")
                                        .uppercase(),
                                    valueColor = when {
                                        state.motionDetectorState is com.sentinel.app.domain.model.MotionDetectorState.Running -> GreenOnline
                                        else -> TextSecondary
                                    })
                            }
                        }
                    }
                }
                DetailTab.DETAILS -> {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DetailCard("CAMERA_INFO", OrangePrimary, Modifier.weight(1f)) {
                                HudStatRow("NAME", camera.name.uppercase())
                                HudStatRow("ROOM", camera.room.uppercase())
                                HudStatRow("TYPE", camera.sourceType.displayName.uppercase())
                                HudStatRow("STATUS", camera.displayStatus.name)
                            }
                            DetailCard("NETWORK", CyanTertiaryDim, Modifier.weight(1f)) {
                                HudStatRow("HOST", camera.connectionProfile.host.ifBlank { "--" })
                                HudStatRow("PORT", camera.connectionProfile.port.toString())
                                HudStatRow("PATH", camera.connectionProfile.path.ifBlank { "/" })
                                HudStatRow("AUTH",
                                    if (camera.connectionProfile.hasCredentials) "CONFIGURED" else "NONE")
                            }
                        }
                    }
                    camera.androidPhoneConfig?.let { cfg ->
                        item {
                            DetailCard("ANDROID_PHONE_NODE", GreenOnline, Modifier.fillMaxWidth()) {
                                HudStatRow("NICKNAME", cfg.phoneNickname.uppercase())
                                HudStatRow("APP", cfg.appMethod.displayName.uppercase())
                                HudStatRow("AUDIO", if (cfg.audioAvailable) "ACTIVE" else "NONE")
                                cfg.batteryLevelPercent?.let { bat ->
                                    HudStatRow("BATTERY",
                                        "$bat%${if (cfg.isCharging == true) " + CHARGING" else ""}",
                                        valueColor = if (bat < 20 && cfg.isCharging != true) ErrorRed else GreenOnline)
                                }
                            }
                        }
                    }
                }
                DetailTab.EVENTS -> {
                    if (state.events.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                Text("NO_EVENTS_LOGGED", fontSize = 12.sp, fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic, color = TextDisabled, letterSpacing = 1.sp)
                            }
                        }
                    } else {
                        items(state.events, key = { it.id }) { event ->
                            EventRow(event = event, onClick = {})
                            Box(Modifier.fillMaxWidth().padding(start = 68.dp).height(1.dp).background(SurfaceStroke))
                        }
                    }
                }
                DetailTab.DIAGNOSTICS -> {
                    item {
                        DetailCard("CONNECTION_DIAGNOSTIC", OrangePrimary, Modifier.fillMaxWidth()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "TCP reachability test — verifies the camera host is reachable on the LAN. " +
                                "Stream-level validation runs automatically via the playback engine.",
                                fontSize = 10.sp, color = TextSecondary
                            )
                            Spacer(Modifier.height(14.dp))
                            PrimaryButton(
                                text    = if (state.isTestingConnection) "TESTING..." else "RUN_DIAGNOSTIC",
                                onClick = viewModel::testConnection,
                                enabled = !state.isTestingConnection,
                                icon    = Icons.Default.Refresh,
                                modifier = Modifier.fillMaxWidth()
                            )
                            state.lastTestResult?.let { result ->
                                Spacer(Modifier.height(12.dp))
                                HudStatRow("RESULT",
                                    if (result.success) "HOST_REACHABLE" else "UNREACHABLE",
                                    valueColor = if (result.success) GreenOnline else ErrorRed)
                                result.latencyMs?.let { ms -> HudStatRow("LATENCY_TCP", "${ms}MS") }
                                result.errorMessage?.let { err -> HudStatRow("FAULT", err, valueColor = ErrorRed) }
                                HudStatRow("TESTED_AT",
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(result.testedAt)))
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
// DetailCard — fastener card with left border and section title
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DetailCard(
    title: String,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(SurfaceBase)
            .drawBehind {
                drawRect(borderColor, size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height))
                // Corner fastener dots
                val dotSize = 4.dp.toPx()
                val dotColor = SurfaceStroke
                drawRect(dotColor, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(dotSize, dotSize))
                drawRect(dotColor, topLeft = Offset(size.width - dotSize, 0f), size = androidx.compose.ui.geometry.Size(dotSize, dotSize))
                drawRect(dotColor, topLeft = Offset(0f, size.height - dotSize), size = androidx.compose.ui.geometry.Size(dotSize, dotSize))
                drawRect(dotColor, topLeft = Offset(size.width - dotSize, size.height - dotSize), size = androidx.compose.ui.geometry.Size(dotSize, dotSize))
            }
            .padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic, color = borderColor, letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp))
            content()
        }
    }
}

private fun TacticalActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    iconColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    loading: Boolean = false,
    isActive: Boolean = false,
    activePulse: Boolean = false
) = Unit  // Delegated to component file — keep signature consistent
