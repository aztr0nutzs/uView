package com.sentinel.app.features.cameras.detail

// UI AUTHORITY: This camera detail / live feed surface is governed by root
// `uview_screen3.html`. Preserve its live-feed shell, action strip, tabs,
// HUD metrics, corner framing, glow usage, edge treatment, and layout rhythm.
// Deviations require SCREEN_AUTHORITY_MAP.md review.

import androidx.annotation.OptIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.MotionDetectorState
import com.sentinel.app.domain.model.PlayerState
import com.sentinel.app.domain.model.RecordingState
import com.sentinel.app.ui.components.CornerBrackets
import com.sentinel.app.ui.components.EventRow
import com.sentinel.app.ui.components.LiveStreamSurface
import com.sentinel.app.ui.components.PrimaryButton
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanTertiaryDim
import com.sentinel.app.ui.theme.ErrorContainer
import com.sentinel.app.ui.theme.ErrorRed
import com.sentinel.app.ui.theme.GreenOnline
import com.sentinel.app.ui.theme.OrangeGlow
import com.sentinel.app.ui.theme.OrangePrimary
import com.sentinel.app.ui.theme.SurfaceBase
import com.sentinel.app.ui.theme.SurfaceHighest
import com.sentinel.app.ui.theme.SurfaceLowest
import com.sentinel.app.ui.theme.SurfaceStroke
import com.sentinel.app.ui.theme.TextDisabled
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Unavailable = MetricValue("UNAVAILABLE", true)

@OptIn(UnstableApi::class)
@Composable
fun CameraDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateSettings: (String) -> Unit,
    viewModel: CameraDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { viewModel.stopPlayback() }
    }

    if (state.isLoading || state.camera == null) {
        Box(
            Modifier
                .fillMaxSize()
                .background(BackgroundDeep),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = OrangePrimary)
        }
        return
    }

    val camera = state.camera!!
    var isMuted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .carbonBackground()
    ) {
        DetailTopNav(
            title = "TACTICAL_${camera.name}".hudToken(),
            onNavigateBack = onNavigateBack,
            onNavigateSettings = { onNavigateSettings(camera.id) }
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                LiveFeedShell(
                    camera = camera,
                    playerState = playerState,
                    recordingState = recordingState,
                    onReconnect = viewModel::reconnect,
                    exoPlayer = viewModel.getExoPlayer(),
                    mjpegFrames = viewModel.getMjpegFrames()
                )
            }

            item {
                TacticalActionStrip(
                    isTakingSnapshot = state.isTakingSnapshot,
                    isRecording = recordingState == RecordingState.RECORDING,
                    isFavorite = camera.isFavorite,
                    isMuted = isMuted,
                    onSnapshot = viewModel::takeSnapshot,
                    onRecording = viewModel::toggleRecording,
                    onReconnect = viewModel::reconnect,
                    onMute = { isMuted = viewModel.toggleMute() },
                    onFavorite = viewModel::toggleFavorite,
                    onSettings = { onNavigateSettings(camera.id) }
                )
            }

            item {
                DetailTabRail(
                    selectedTab = state.selectedTab,
                    onSelect = viewModel::selectTab
                )
            }

            when (state.selectedTab) {
                DetailTab.LIVE -> {
                    item {
                        LivePanelGrid(
                            camera = camera,
                            playerState = playerState,
                            motionDetectorState = state.motionDetectorState
                        )
                    }
                }

                DetailTab.DETAILS -> {
                    item {
                        ResponsivePanelGrid(
                            first = {
                                DetailPanel("CAMERA_INFO", OrangePrimary, Modifier.fillMaxWidth()) {
                                MetricRow("NAME", metric(camera.name.hudToken()))
                                MetricRow("ROOM", metric(camera.room.hudToken()))
                                MetricRow("SOURCE_TYPE", metric(camera.sourceType.displayName.hudToken()))
                                MetricRow("STATUS", metric(camera.displayStatus.name))
                                }
                            },
                            second = {
                                DetailPanel("NETWORK_ENDPOINT", CyanTertiaryDim, Modifier.fillMaxWidth()) {
                                MetricRow("HOST", metric(camera.connectionProfile.host.ifBlank { "UNAVAILABLE" }))
                                MetricRow("PORT", metric(camera.connectionProfile.port.toString()))
                                MetricRow("PATH", metric(camera.connectionProfile.path.ifBlank { "/" }))
                                MetricRow("AUTH", metric(if (camera.connectionProfile.hasCredentials) "CONFIGURED" else "NONE"))
                                }
                            }
                        )
                    }
                    camera.androidPhoneConfig?.let { cfg ->
                        item {
                            DetailPanel("ANDROID_PHONE_NODE", GreenOnline, Modifier.fillMaxWidth()) {
                                MetricRow("NICKNAME", metric(cfg.phoneNickname.hudToken()))
                                MetricRow("APP_METHOD", metric(cfg.appMethod.displayName.hudToken()))
                                MetricRow("AUDIO", metric(if (cfg.audioAvailable) "ACTIVE" else "UNAVAILABLE", !cfg.audioAvailable))
                                MetricRow("BATTERY", cfg.batteryLevelPercent?.let {
                                    metric("$it%${if (cfg.isCharging == true) "_CHARGING" else ""}")
                                } ?: Unavailable)
                            }
                        }
                    }
                }

                DetailTab.EVENTS -> {
                    if (state.events.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                HudText(
                                    text = "NO_EVENTS_LOGGED",
                                    fontSize = 12,
                                    color = TextDisabled,
                                    weight = FontWeight.Black
                                )
                            }
                        }
                    } else {
                        items(state.events, key = { it.id }) { event ->
                            EventRow(event = event, onClick = {})
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

                DetailTab.DIAGNOSTICS -> {
                    item {
                        DetailPanel("CONNECTION_DIAGNOSTIC", OrangePrimary, Modifier.fillMaxWidth()) {
                            MetricRow("TEST_SCOPE", metric("TCP_HOST_REACHABILITY"))
                            MetricRow("STREAM_VALIDATION", metric("PLAYBACK_ENGINE"))
                            Spacer(Modifier.height(10.dp))
                            PrimaryButton(
                                text = if (state.isTestingConnection) "TESTING..." else "RUN_DIAGNOSTIC",
                                onClick = viewModel::testConnection,
                                enabled = !state.isTestingConnection,
                                icon = Icons.Default.Refresh,
                                modifier = Modifier.fillMaxWidth()
                            )
                            state.lastTestResult?.let { result ->
                                Spacer(Modifier.height(10.dp))
                                MetricRow(
                                    "RESULT",
                                    metric(if (result.success) "HOST_REACHABLE" else "UNREACHABLE"),
                                    valueColor = if (result.success) GreenOnline else ErrorRed
                                )
                                MetricRow("LATENCY_TCP", result.latencyMs?.let { metric("${it}MS") } ?: Unavailable)
                                MetricRow("CREDENTIALS", result.credentialsAccepted?.let {
                                    metric(if (it) "ACCEPTED" else "REJECTED")
                                } ?: Unavailable)
                                result.errorMessage?.let { MetricRow("FAULT", metric(it.hudToken()), valueColor = ErrorRed) }
                                MetricRow(
                                    "TESTED_AT",
                                    metric(SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(result.testedAt)))
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(78.dp)) }
        }
    }
}

@Composable
private fun DetailTopNav(
    title: String,
    onNavigateBack: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xE60E0E0E))
            .drawBehind {
                drawRect(
                    color = SurfaceBase,
                    topLeft = Offset(0f, size.height - 4.dp.toPx()),
                    size = Size(size.width, 4.dp.toPx())
                )
                drawRect(
                    color = OrangeGlow.copy(alpha = 0.25f),
                    topLeft = Offset(0f, size.height - 7.dp.toPx()),
                    size = Size(size.width, 3.dp.toPx())
                )
            }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.ArrowBack, "Back", tint = OrangePrimary, modifier = Modifier.size(23.dp))
        }
        HudText(
            text = title,
            fontSize = 18,
            color = OrangePrimary,
            weight = FontWeight.Black,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNavigateSettings, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Tune, "Settings", tint = OrangePrimary, modifier = Modifier.size(23.dp))
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun LiveFeedShell(
    camera: CameraDevice,
    playerState: PlayerState,
    recordingState: RecordingState,
    onReconnect: () -> Unit,
    exoPlayer: androidx.media3.exoplayer.ExoPlayer?,
    mjpegFrames: kotlinx.coroutines.flow.Flow<android.graphics.Bitmap>?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .mechanicalFeedFrame()
            .background(Color.Black)
    ) {
        LiveStreamSurface(
            playerState = playerState,
            exoPlayer = exoPlayer,
            mjpegFrames = mjpegFrames,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(Color.Black.copy(alpha = 0.16f))
                }
        )

        CornerBrackets(
            color = OrangePrimary.copy(alpha = 0.42f),
            size = 32.dp,
            stroke = 4.dp
        )

        if (playerState is PlayerState.Error) {
            ErrorFeedOverlay(message = playerState.message, onReconnect = onReconnect)
        }

        FeedIdentityOverlay(
            camera = camera,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FeedStatusSlab(
                text = "LIVE FEED // ${camera.preferredQuality.label.hudToken()}",
                color = GreenOnline
            )
            FeedStatusSlab(
                text = if (camera.connectionProfile.useTls) "TLS ENCRYPTED" else "ENCRYPTION UNAVAILABLE",
                color = CyanTertiaryDim
            )
        }

        FeedTelemetryOverlay(
            bitrate = camera.healthStatus?.streamBitrateKbps?.let { metric("${formatMbps(it)} MBPS") } ?: Unavailable,
            latency = camera.healthStatus?.latencyMs?.let { metric("${it} MS") } ?: Unavailable,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        )

        if (recordingState == RecordingState.RECORDING) {
            RecordingBadge(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun FeedIdentityOverlay(camera: CameraDevice, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(SurfaceLowest.copy(alpha = 0.82f))
            .drawBehind {
                drawRect(OrangePrimary, size = Size(4.dp.toPx(), size.height))
            }
            .padding(start = 12.dp, end = 10.dp, top = 5.dp, bottom = 6.dp)
    ) {
        Column {
            HudText("CAM_ID", 9, OrangePrimary, FontWeight.Black)
            HudText(
                text = "${camera.name} - ${camera.room}".uppercase(Locale.getDefault()),
                fontSize = 16,
                color = TextPrimary,
                weight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FeedStatusSlab(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(SurfaceLowest.copy(alpha = 0.82f))
            .drawBehind {
                drawRect(
                    color = color,
                    topLeft = Offset(size.width - 2.dp.toPx(), 0f),
                    size = Size(2.dp.toPx(), size.height)
                )
            }
            .padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp)
    ) {
        HudText(text.uppercase(Locale.getDefault()), 10, color, FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun FeedTelemetryOverlay(
    bitrate: MetricValue,
    latency: MetricValue,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(SurfaceLowest.copy(alpha = 0.64f))
            .drawBehind {
                drawRect(
                    color = SurfaceStroke.copy(alpha = 0.5f),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 1.dp.toPx())
                )
            }
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FeedMiniMetric("BITRATE", bitrate)
        FeedMiniMetric("LATENCY", latency)
    }
}

@Composable
private fun FeedMiniMetric(label: String, value: MetricValue) {
    Column {
        HudText(label, 8, TextSecondary, FontWeight.Black)
        HudText(
            text = value.text,
            fontSize = if (value.isUnavailable) 9 else 13,
            color = if (value.isUnavailable) TextDisabled else CyanTertiaryDim,
            weight = FontWeight.Bold
        )
    }
}

@Composable
private fun RecordingBadge(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "detail_rec").animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "detail_rec_alpha"
    )
    Row(
        modifier = modifier
            .background(ErrorRed.copy(alpha = 0.16f))
            .drawBehind {
                drawRect(ErrorRed.copy(alpha = alpha), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(ErrorRed.copy(alpha = alpha), CircleShape)
        )
        HudText("REC", 10, ErrorRed, FontWeight.Black)
    }
}

@Composable
private fun ErrorFeedOverlay(message: String, onReconnect: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ErrorContainer.copy(alpha = 0.62f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Error, null, tint = ErrorRed, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(8.dp))
            HudText(message.hudToken(), 11, ErrorRed, FontWeight.Black, maxLines = 2)
            Spacer(Modifier.height(12.dp))
            PrimaryButton("RECONNECT", onClick = onReconnect, icon = Icons.Default.Sync)
        }
    }
}

@Composable
private fun TacticalActionStrip(
    isTakingSnapshot: Boolean,
    isRecording: Boolean,
    isFavorite: Boolean,
    isMuted: Boolean,
    onSnapshot: () -> Unit,
    onRecording: () -> Unit,
    onReconnect: () -> Unit,
    onMute: () -> Unit,
    onFavorite: () -> Unit,
    onSettings: () -> Unit
) {
    val actions = listOf(
        ActionSpec(Icons.Default.CameraAlt, if (isTakingSnapshot) "CAPTURING" else "SNAPSHOT", CyanTertiaryDim, false, false, onSnapshot),
        ActionSpec(Icons.Default.FiberManualRecord, if (isRecording) "RECORDING" else "RECORD", ErrorRed, isRecording, isRecording, onRecording),
        ActionSpec(Icons.Default.Sync, "RECONNECT", GreenOnline, false, false, onReconnect),
        ActionSpec(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, if (isMuted) "UNMUTE" else "MUTE", TextPrimary, false, false, onMute),
        ActionSpec(if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, "FAVORITE", OrangePrimary, isFavorite, false, onFavorite),
        ActionSpec(Icons.Default.Settings, "SETTINGS", TextPrimary, false, false, onSettings)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 720.dp) 3 else 6
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            actions.chunked(columns).forEach { rowActions ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                    rowActions.forEach { action ->
                        TacticalStripButton(action, Modifier.weight(1f))
                    }
                    repeat(columns - rowActions.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticalStripButton(action: ActionSpec, modifier: Modifier = Modifier) {
    val pulseAlpha by if (action.pulse) {
        rememberInfiniteTransition(label = "action_pulse").animateFloat(
            initialValue = 0.10f,
            targetValue = 0.24f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "action_pulse_alpha"
        )
    } else {
        remember { mutableStateOf(if (action.active) 0.10f else 0f) }
    }

    Column(
        modifier = modifier
            .height(72.dp)
            .background(if (action.active || action.pulse) action.color.copy(alpha = pulseAlpha) else SurfaceBase)
            .drawBehind {
                drawRect(
                    color = if (action.active || action.pulse) action.color else SurfaceStroke,
                    topLeft = Offset(0f, size.height - 4.dp.toPx()),
                    size = Size(size.width, 4.dp.toPx())
                )
            }
            .clickable(onClick = action.onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(action.icon, action.label, tint = action.color, modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(6.dp))
        HudText(
            text = action.label,
            fontSize = 10,
            color = if (action.active || action.pulse) action.color else TextSecondary,
            weight = FontWeight.Black,
            maxLines = 1
        )
    }
}

@Composable
private fun DetailTabRail(selectedTab: DetailTab, onSelect: (DetailTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    SurfaceHighest,
                    topLeft = Offset(0f, size.height - 4.dp.toPx()),
                    size = Size(size.width, 4.dp.toPx())
                )
            },
        horizontalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        DetailTab.values().forEach { tab ->
            val selected = tab == selectedTab
            Box(
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .padding(bottom = 4.dp)
                    .drawBehind {
                        if (selected) {
                            drawRect(
                                OrangeGlow.copy(alpha = 0.42f),
                                topLeft = Offset(0f, size.height - 7.dp.toPx()),
                                size = Size(size.width, 7.dp.toPx())
                            )
                            drawRect(
                                OrangePrimary,
                                topLeft = Offset(0f, size.height - 4.dp.toPx()),
                                size = Size(size.width, 4.dp.toPx())
                            )
                        }
                    }
                    .padding(bottom = 13.dp)
            ) {
                HudText(
                    text = tab.label.uppercase(),
                    fontSize = 17,
                    color = if (selected) OrangePrimary else TextSecondary,
                    weight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun LivePanelGrid(
    camera: CameraDevice,
    playerState: PlayerState,
    motionDetectorState: MotionDetectorState
) {
    ResponsivePanelGrid(
        first = {
            DetailPanel("STREAM_PERFORMANCE", CyanTertiaryDim, Modifier.fillMaxWidth()) {
            MetricRow("PROTOCOL_TYPE", metric(protocolLabel(camera)))
            MetricRow(
                "BITRATE_ACTUAL",
                camera.healthStatus?.streamBitrateKbps?.let { metric("${formatMbps(it)} MBPS") } ?: Unavailable,
                valueColor = CyanTertiaryDim,
                barLevel = camera.healthStatus?.streamBitrateKbps?.let { bitrateBars(it) }
            )
            MetricRow(
                "FRAME_LATENCY",
                camera.healthStatus?.latencyMs?.let { metric("${it}MS_${latencyLabel(it)}") } ?: Unavailable,
                valueColor = GreenOnline
            )
            MetricRow("BUFFER_STATE", metric(bufferStateLabel(playerState)), valueColor = playerStateColor(playerState))
            }
        },
        second = {
            DetailPanel("CAMERA_CONFIGURATION", OrangePrimary, Modifier.fillMaxWidth()) {
            MetricRow("RESOLUTION_SET", Unavailable)
            MetricRow("ENCODING_HW", Unavailable)
            MetricRow("NIGHT_VISION", Unavailable, valueColor = TextDisabled)
            MetricRow("FIRMWARE_VER", Unavailable)
            MetricRow(
                "MOTION_DETECTOR",
                metric(
                    when (motionDetectorState) {
                        is MotionDetectorState.Running -> "AUTO_ACTIVE"
                        is MotionDetectorState.Error -> "FAULT_STATE"
                        else -> "INACTIVE"
                    }
                ),
                valueColor = if (motionDetectorState is MotionDetectorState.Running) OrangePrimary else TextSecondary
            )
            }
        }
    )
}

@Composable
private fun ResponsivePanelGrid(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 720.dp) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(1f)) { first() }
                Box(Modifier.weight(1f)) { second() }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                first()
                second()
            }
        }
    }
}

@Composable
private fun DetailPanel(
    title: String,
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceBase)
            .drawBehind {
                drawRect(borderColor, size = Size(4.dp.toPx(), size.height))
                val dotSize = 5.dp.toPx()
                drawRect(SurfaceStroke, topLeft = Offset(2.dp.toPx(), 2.dp.toPx()), size = Size(dotSize, dotSize))
                drawRect(SurfaceStroke, topLeft = Offset(size.width - dotSize - 2.dp.toPx(), 2.dp.toPx()), size = Size(dotSize, dotSize))
                drawRect(SurfaceStroke, topLeft = Offset(2.dp.toPx(), size.height - dotSize - 2.dp.toPx()), size = Size(dotSize, dotSize))
                drawRect(SurfaceStroke, topLeft = Offset(size.width - dotSize - 2.dp.toPx(), size.height - dotSize - 2.dp.toPx()), size = Size(dotSize, dotSize))
            }
            .padding(start = 18.dp, end = 14.dp, top = 18.dp, bottom = 14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HudText(title, 13, borderColor, FontWeight.Black)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(5.dp).background(SurfaceStroke.copy(alpha = 0.55f)))
                    Box(Modifier.size(5.dp).background(SurfaceStroke.copy(alpha = 0.55f)))
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: MetricValue,
    valueColor: Color = TextPrimary,
    barLevel: Int? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = SurfaceStroke.copy(alpha = 0.42f),
                    topLeft = Offset(0f, size.height - 1.dp.toPx()),
                    size = Size(size.width, 1.dp.toPx())
                )
            }
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HudText(label, 11, TextSecondary, FontWeight.Bold, modifier = Modifier.weight(0.9f))
        Row(
            modifier = Modifier.weight(1.1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (barLevel != null) {
                SegmentedBars(level = barLevel, color = if (value.isUnavailable) TextDisabled else CyanTertiaryDim)
                Spacer(Modifier.width(10.dp))
            }
            HudText(
                text = value.text,
                fontSize = if (value.isUnavailable) 10 else 12,
                color = if (value.isUnavailable) TextDisabled else valueColor,
                weight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SegmentedBars(level: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) { index ->
            Box(
                Modifier
                    .width(7.dp)
                    .height(16.dp)
                    .background(if (index < level) color else SurfaceHighest)
            )
        }
    }
}

@Composable
private fun HudText(
    text: String,
    fontSize: Int,
    color: Color,
    weight: FontWeight,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    Text(
        text = text,
        fontSize = fontSize.sp,
        fontWeight = weight,
        fontStyle = FontStyle.Italic,
        color = color,
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        modifier = modifier
    )
}

private fun Modifier.carbonBackground(): Modifier = this
    .background(BackgroundDeep)
    .drawBehind {
        val step = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(SurfaceBase.copy(alpha = 0.62f), radius = 0.45.dp.toPx(), center = Offset(x, y))
                y += step
            }
            x += step
        }
    }

private fun Modifier.mechanicalFeedFrame(): Modifier = this
    .drawBehind {
        drawRect(SurfaceHighest)
        drawRect(
            color = Color.White.copy(alpha = 0.06f),
            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
            size = Size(size.width - 8.dp.toPx(), 2.dp.toPx())
        )
        drawRect(
            color = Color.White.copy(alpha = 0.05f),
            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
            size = Size(2.dp.toPx(), size.height - 8.dp.toPx())
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.55f),
            topLeft = Offset(4.dp.toPx(), size.height - 6.dp.toPx()),
            size = Size(size.width - 8.dp.toPx(), 2.dp.toPx())
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.55f),
            topLeft = Offset(size.width - 6.dp.toPx(), 4.dp.toPx()),
            size = Size(2.dp.toPx(), size.height - 8.dp.toPx())
        )
    }
    .padding(4.dp)

private data class MetricValue(val text: String, val isUnavailable: Boolean = false)

private data class ActionSpec(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val active: Boolean,
    val pulse: Boolean,
    val onClick: () -> Unit
)

private fun metric(text: String, unavailable: Boolean = text == "UNAVAILABLE"): MetricValue =
    MetricValue(text.hudToken(), unavailable)

private fun String.hudToken(): String =
    trim()
        .uppercase(Locale.getDefault())
        .replace(Regex("[^A-Z0-9]+"), "_")
        .trim('_')
        .ifBlank { "UNAVAILABLE" }

private fun formatMbps(kbps: Int): String =
    String.format(Locale.US, "%.1f", kbps / 1000f)

private fun bitrateBars(kbps: Int): Int = when {
    kbps <= 0 -> 0
    kbps < 1000 -> 1
    kbps < 2500 -> 2
    kbps < 4500 -> 3
    kbps < 7000 -> 4
    else -> 5
}

private fun latencyLabel(ms: Long): String = when {
    ms <= 80 -> "OPTIMAL"
    ms <= 180 -> "STABLE"
    else -> "DEGRADED"
}

private fun protocolLabel(camera: CameraDevice): String {
    val secure = if (camera.connectionProfile.useTls) "_SECURE" else ""
    return "${camera.sourceType.name}_${camera.connectionProfile.transport.name}$secure"
}

private fun bufferStateLabel(playerState: PlayerState): String = when (playerState) {
    is PlayerState.Playing -> "HEALTHY_STABLE"
    is PlayerState.Buffering -> "BUFFERING_${playerState.progressPercent}PCT"
    is PlayerState.Reconnecting -> "RECONNECTING"
    is PlayerState.Loading -> "INITIALIZING"
    is PlayerState.Error -> "FAULT_STATE"
    is PlayerState.Paused -> "PAUSED"
    is PlayerState.StreamUnsupported -> "UNSUPPORTED"
    is PlayerState.Idle -> "STANDBY"
}

private fun playerStateColor(playerState: PlayerState): Color = when (playerState) {
    is PlayerState.Playing -> TextPrimary
    is PlayerState.Error -> ErrorRed
    is PlayerState.Reconnecting,
    is PlayerState.Buffering -> GreenOnline
    else -> TextSecondary
}
