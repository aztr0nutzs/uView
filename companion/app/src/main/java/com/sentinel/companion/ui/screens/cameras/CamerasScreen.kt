package com.sentinel.companion.ui.screens.cameras

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.companion.data.model.Camera
import com.sentinel.companion.data.model.CameraStatus
import com.sentinel.companion.ui.components.CompanionTopBar
import com.sentinel.companion.ui.components.SectionHeader
import com.sentinel.companion.ui.components.SourceTypeBadge
import com.sentinel.companion.ui.components.StatusBadge
import com.sentinel.companion.ui.components.chamferClip
import com.sentinel.companion.ui.theme.BackgroundDeep
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

@Composable
fun CamerasScreen(
    onCameraDetail: (String) -> Unit,
    viewModel: CamerasViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep),
    ) {
        CompanionTopBar(title = "SENTINEL_HUB", subtitle = "CAMERA_ROSTER // ${state.cameras.size} UNITS")

        // ── Search bar ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .background(SurfaceBase, RoundedCornerShape(10.dp))
                .border(1.dp, SurfaceStroke, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchChanged,
                singleLine = true,
                cursorBrush = SolidColor(GreenOnline),
                textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp, letterSpacing = 0.5.sp),
                decorationBox = { inner ->
                    if (state.searchQuery.isEmpty()) {
                        Text("SIGNAL_FILTER", color = TextDisabled, fontSize = 13.sp, letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.Medium)
                    }
                    inner()
                },
                modifier = Modifier.weight(1f),
            )
        }

        // ── Room filter chips ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.rooms.forEach { room ->
                val selected = room == state.selectedRoom
                Text(
                    text = room,
                    color = if (selected) BackgroundDeep else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .background(
                            if (selected) OrangePrimary else SurfaceBase,
                            RoundedCornerShape(2.dp),
                        )
                        .border(
                            1.dp,
                            if (selected) OrangePrimary else SurfaceStroke,
                            RoundedCornerShape(2.dp),
                        )
                        .clickable { viewModel.onRoomSelected(room) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        // ── Section header ─────────────────────────────────────────────────
        SectionHeader(
            title = "ACTIVE_RECON_FEEDS",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        // ── Camera list ────────────────────────────────────────────────────
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(state.filteredCameras, key = { it.id }) { camera ->
                TacticalCameraRow(
                    camera        = camera,
                    onOpen        = { onCameraDetail(camera.id) },
                    onFavorite    = { viewModel.toggleFavorite(camera.id) },
                    onToggleEnabled = { viewModel.toggleEnabled(camera.id) },
                    onReconnect   = { viewModel.reconnect(camera.id) },
                    onDelete      = { viewModel.deleteCamera(camera.id) },
                )
                Box(Modifier.fillMaxWidth().height(6.dp).background(SurfaceLowest))
            }
            if (state.filteredCameras.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("NO_UNITS_FOUND", color = TextDisabled, fontSize = 13.sp,
                            fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

// ─── Tactical camera row ──────────────────────────────────────────────────────

@Composable
private fun TacticalCameraRow(
    camera: Camera,
    onOpen: () -> Unit,
    onFavorite: () -> Unit,
    onToggleEnabled: () -> Unit,
    onReconnect: () -> Unit,
    onDelete: () -> Unit,
) {
    val isOnline = camera.statusEnum() == CameraStatus.ONLINE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceBase)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Chamfer thumbnail
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 48.dp)
                .chamferClip()
                .background(if (isOnline) SurfaceElevated else SurfaceLowest)
                .border(
                    1.dp,
                    if (isOnline) OrangePrimary.copy(alpha = 0.6f) else ErrorRed.copy(alpha = 0.3f),
                    RoundedCornerShape(0.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                contentDescription = null,
                tint = if (isOnline) OrangePrimary.copy(alpha = 0.6f) else ErrorRed.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp),
            )
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = camera.name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 3.dp),
            ) {
                SourceTypeBadge(camera.sourceTypeEnum().label)
                StatusBadge(camera.statusEnum())
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 3.dp),
            ) {
                Text(camera.room, color = TextSecondary, fontSize = 10.sp)
                if (isOnline && camera.latencyMs > 0) {
                    Text("${camera.latencyMs}ms", color = GreenOnline, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(camera.id.take(8).uppercase(), color = TextDisabled, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Action icons
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (camera.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "Favorite",
                tint = if (camera.isFavorite) OrangePrimary else TextDisabled,
                modifier = Modifier.size(18.dp).clickable(onClick = onFavorite),
            )
            if (camera.statusEnum() == CameraStatus.OFFLINE) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Reconnect",
                    tint = CyanTertiaryDim,
                    modifier = Modifier.size(18.dp).clickable(onClick = onReconnect),
                )
            }
        }
    }
}
