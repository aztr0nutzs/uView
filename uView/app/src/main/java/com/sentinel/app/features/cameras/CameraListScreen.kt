package com.sentinel.app.features.cameras

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraStatus
import com.sentinel.app.ui.components.ChamferThumbnail
import com.sentinel.app.ui.components.EmptyStateView
import com.sentinel.app.ui.components.PrimaryButton
import com.sentinel.app.ui.components.TacticalBadge
import com.sentinel.app.ui.components.TacticalSectionHeader
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanAccent
import com.sentinel.app.ui.theme.CyanSubtleBg
import com.sentinel.app.ui.theme.CyanTertiaryDim
import com.sentinel.app.ui.theme.ErrorRed
import com.sentinel.app.ui.theme.GreenOnline
import com.sentinel.app.ui.theme.OrangePrimary
import com.sentinel.app.ui.theme.OrangeSubtle
import com.sentinel.app.ui.theme.StatusOffline
import com.sentinel.app.ui.theme.SurfaceBase
import com.sentinel.app.ui.theme.SurfaceElevated
import com.sentinel.app.ui.theme.SurfaceHighest
import com.sentinel.app.ui.theme.SurfaceLowest
import com.sentinel.app.ui.theme.SurfaceStroke
import com.sentinel.app.ui.theme.TextDisabled
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun CameraListScreen(
    onNavigateBack: () -> Unit,
    onNavigateCameraDetail: (String) -> Unit,
    onNavigateAddCamera: () -> Unit,
    onNavigateEdit: (String) -> Unit,
    viewModel: CameraListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Real uptime counter — seconds elapsed since app start
    var uptimeSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); uptimeSeconds++ }
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDeep)) {

        // ── TACTICAL_HUB top bar ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xE6111111))  // zinc-900/90
                .border(
                    width = 6.dp,
                    color = SurfaceLowest,
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo + title
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceHighest)
                    .border(2.dp, OrangePrimary.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Videocam, null, tint = OrangePrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "SENTINEL_HUB",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Black,
                fontStyle  = FontStyle.Italic,
                color      = OrangePrimary,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.weight(1f))

            // Real uptime display
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "SYSTEM_UPTIME",
                    fontSize = 8.sp,
                    color    = CyanTertiaryDim,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
                val h  = uptimeSeconds / 3600
                val m  = (uptimeSeconds % 3600) / 60
                val s  = uptimeSeconds % 60
                Text(
                    "%02d:%02d:%02d".format(h, m, s),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.BatteryChargingFull, null, tint = OrangePrimary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onNavigateAddCamera, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, "Add camera", tint = CyanTertiaryDim)
            }
        }

        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier            = Modifier.weight(1f)
        ) {
            // ── SIGNAL_FILTER search ──────────────────────────────────────
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    Text(
                        "SIGNAL_FILTER",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle  = FontStyle.Italic,
                        color      = CyanTertiaryDim,
                        letterSpacing = 2.sp,
                        modifier   = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceLowest)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = CyanTertiaryDim.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(
                                value         = state.searchQuery,
                                onValueChange = viewModel::setSearchQuery,
                                singleLine    = true,
                                textStyle     = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextPrimary, fontStyle = FontStyle.Italic, letterSpacing = 1.sp
                                ),
                                cursorBrush = SolidColor(GreenOnline),
                                decorationBox = { inner ->
                                    Box(Modifier.height(52.dp), contentAlignment = Alignment.CenterStart) {
                                        if (state.searchQuery.isEmpty()) {
                                            Text("QUERY_FEED_IDENTIFIER...", color = TextDisabled,
                                                fontStyle = FontStyle.Italic, letterSpacing = 1.sp,
                                                fontSize = 13.sp)
                                        }
                                        inner()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Green animated bottom underline (always 3dp, simulating the focus animation)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (state.searchQuery.isNotEmpty()) 1f else 0f)
                                .height(3.dp)
                                .background(GreenOnline)
                                .align(Alignment.BottomStart)
                        )
                    }
                }
            }

            // ── Room filter chips ─────────────────────────────────────────
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    item {
                        TacticalChip(
                            label    = "ALL_NODES",
                            selected = state.selectedRoom == null,
                            onClick  = { viewModel.setRoomFilter(null) }
                        )
                    }
                    items(state.allRooms) { room ->
                        TacticalChip(
                            label    = room.uppercase().replace(" ", "_"),
                            selected = state.selectedRoom == room,
                            onClick  = { viewModel.setRoomFilter(room) }
                        )
                    }
                }
            }

            // ── ACTIVE_RECON_FEEDS section header ─────────────────────────
            item {
                TacticalSectionHeader(
                    title    = "ACTIVE_RECON_FEEDS",
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // ── Camera rows ───────────────────────────────────────────────
            if (state.filteredCameras.isEmpty() && !state.isLoading) {
                item {
                    EmptyStateView(
                        icon     = Icons.Default.Videocam,
                        title    = if (state.cameras.isEmpty()) "NO_FEEDS_CONFIGURED" else "NO_RESULTS",
                        subtitle = if (state.cameras.isEmpty())
                            "Deploy a camera source to begin monitoring"
                        else "Adjust SIGNAL_FILTER parameters",
                        action   = if (state.cameras.isEmpty()) {
                            { PrimaryButton("+ ADD_CAMERA", onClick = onNavigateAddCamera) }
                        } else null
                    )
                }
            } else {
                items(state.filteredCameras, key = { it.id }) { camera ->
                    TacticalCameraRow(
                        camera           = camera,
                        onClick          = { onNavigateCameraDetail(camera.id) },
                        onEdit           = { onNavigateEdit(camera.id) },
                        onToggleEnabled  = { viewModel.toggleEnabled(camera.id, !camera.isEnabled) },
                        onToggleFavorite = { viewModel.toggleFavorite(camera.id) },
                        onDelete         = { viewModel.deleteCamera(camera.id) }
                    )
                    // 6dp bottom border separator matching HTML border-b-[6px] border-zinc-950
                    Box(Modifier.fillMaxWidth().height(6.dp).background(SurfaceLowest))
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TacticalCameraRow — matches the camera list item from screen 1
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TacticalCameraRow(
    camera: CameraDevice,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleEnabled: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isOnline  = camera.displayStatus == CameraStatus.ONLINE
    val isOffline = !camera.isEnabled || camera.displayStatus == CameraStatus.OFFLINE ||
                    camera.displayStatus == CameraStatus.ERROR

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isOffline) SurfaceBase.copy(alpha = 0.5f) else SurfaceBase)
            .clickable(enabled = !isOffline, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Chamfer thumbnail
        ChamferThumbnail(
            status = camera.displayStatus,
            size   = 56.dp
        ) {
            Box(
                Modifier.fillMaxSize().background(if (isOffline) Color(0xFF111111) else Color(0xFF1C1C1C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isOnline) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    null,
                    tint     = if (isOnline) OrangePrimary.copy(alpha = 0.8f) else TextDisabled,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Camera info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                camera.name,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Black,
                fontStyle  = FontStyle.Italic,
                color      = if (isOffline) TextDisabled else TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TacticalBadge(
                    text  = camera.sourceType.displayName.uppercase().take(12),
                    color = if (isOffline) TextDisabled else CyanTertiaryDim
                )
                Text(
                    "LAT: ${camera.healthStatus?.latencyMs?.let { "${it}MS" } ?: "---"}",
                    fontSize  = 9.sp,
                    color     = TextDisabled,
                    letterSpacing = 0.5.sp
                )
            }

            // Right-side status row (UUID + online indicator)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (isOnline) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ONLINE", fontSize = 9.sp, fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic, color = GreenOnline, letterSpacing = 1.sp)
                        // Green indicator bar pill
                        Box(
                            modifier = Modifier
                                .width(32.dp).height(16.dp)
                                .background(SurfaceLowest)
                                .border(1.dp, GreenOnline.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.width(20.dp).height(6.dp).background(GreenOnline))
                        }
                    }
                } else {
                    Text(
                        if (!camera.isEnabled) "DISABLED" else "OFFLINE",
                        fontSize = 9.sp, fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = if (!camera.isEnabled) TextDisabled else ErrorRed.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    "UUID: ${camera.id.take(8).uppercase()}",
                    fontSize = 8.sp,
                    color    = TextDisabled,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        // Open/settings action button (settings_overscan → open detail)
        Box {
            IconButton(
                onClick  = { if (!isOffline) onClick() else menuOpen = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.OpenInFull,
                    "Open feed",
                    tint     = if (isOffline) TextDisabled else CyanTertiaryDim,
                    modifier = Modifier.size(22.dp)
                )
            }
            DropdownMenu(
                expanded         = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier         = Modifier.background(SurfaceHighest)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit", color = TextPrimary, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = CyanTertiaryDim) },
                    onClick     = { menuOpen = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text(if (camera.isFavorite) "Unfavorite" else "Favorite",
                        color = TextPrimary, fontSize = 13.sp) },
                    leadingIcon = { Icon(if (camera.isFavorite) Icons.Default.Favorite
                        else Icons.Default.FavoriteBorder, null, tint = OrangePrimary) },
                    onClick = { menuOpen = false; onToggleFavorite() }
                )
                DropdownMenuItem(
                    text = { Text(if (camera.isEnabled) "Disable" else "Enable",
                        color = TextPrimary, fontSize = 13.sp) },
                    leadingIcon = { Icon(if (camera.isEnabled) Icons.Default.VideocamOff
                        else Icons.Default.Videocam, null, tint = TextSecondary) },
                    onClick = { menuOpen = false; onToggleEnabled() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = ErrorRed, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = ErrorRed) },
                    onClick     = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TacticalChip — room filter chip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TacticalChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) OrangePrimary else SurfaceElevated)
            .border(1.dp, if (selected) Color.Transparent else CyanTertiaryDim.copy(alpha = 0.2f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Black,
            fontStyle  = FontStyle.Italic,
            color      = if (selected) Color(0xFF111111) else CyanTertiaryDim,
            letterSpacing = 1.5.sp
        )
    }
}
