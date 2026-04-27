package com.sentinel.companion.ui.screens.devicelist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.painterResource
import com.sentinel.companion.R
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sentinel.companion.data.model.DeviceProfile
import com.sentinel.companion.data.model.DeviceState
import com.sentinel.companion.ui.components.CompanionTopBar
import com.sentinel.companion.ui.components.PulseDot
import com.sentinel.companion.ui.components.SectionHeader
import com.sentinel.companion.ui.components.SourceTypeBadge
import com.sentinel.companion.ui.components.carbonBackground
import com.sentinel.companion.ui.components.chamferClip
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.CyanTertiaryDim
import com.sentinel.companion.ui.theme.ErrorContainer
import com.sentinel.companion.ui.theme.ErrorRed
import com.sentinel.companion.ui.theme.GreenContainer
import com.sentinel.companion.ui.theme.GreenOnline
import com.sentinel.companion.ui.theme.OrangePrimary
import com.sentinel.companion.ui.theme.StatusConnecting
import com.sentinel.companion.ui.theme.StatusDisabled
import com.sentinel.companion.ui.theme.SurfaceBase
import com.sentinel.companion.ui.theme.SurfaceElevated
import com.sentinel.companion.ui.theme.SurfaceHighest
import com.sentinel.companion.ui.theme.SurfaceStroke
import com.sentinel.companion.ui.theme.TextDisabled
import com.sentinel.companion.ui.theme.TextPrimary
import com.sentinel.companion.ui.theme.TextSecondary

@Composable
fun DeviceListScreen(
    onAddDevice: () -> Unit,
    onViewStream: (deviceId: String) -> Unit,
    onDeviceSettings: (deviceId: String) -> Unit,
    viewModel: DeviceListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
            .carbonBackground(),
    ) {
        CompanionTopBar(
            title      = "DEVICE_ROSTER",
            subtitle   = "${state.devices.size} UNIT(S) REGISTERED",
            isConnected = state.devices.any { it.state == DeviceState.ONLINE.name },
            trailing   = {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(OrangePrimary, RoundedCornerShape(8.dp))
                        .clickable(onClick = onAddDevice),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.add_cam),
                        contentDescription = "Add Device",
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
        )

        // ── Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .background(SurfaceElevated, RoundedCornerShape(8.dp))
                .border(1.dp, SurfaceStroke, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value         = state.filter,
                onValueChange = viewModel::onFilterChanged,
                textStyle     = TextStyle(color = TextPrimary, fontSize = 13.sp),
                cursorBrush   = SolidColor(CyanTertiaryDim),
                singleLine    = true,
                modifier      = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (state.filter.isEmpty()) {
                        Text("SIGNAL_FILTER...", color = TextDisabled, fontSize = 13.sp, letterSpacing = 0.5.sp)
                    }
                    inner()
                },
            )
        }

        // ── Location chips
        if (state.availableLocations.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LocationChip(
                    label    = "ALL",
                    selected = state.locationFilter.isBlank(),
                    onClick  = { viewModel.onLocationFilterChanged("") },
                )
                state.availableLocations.forEach { loc ->
                    LocationChip(
                        label    = loc.uppercase(),
                        selected = state.locationFilter == loc,
                        onClick  = { viewModel.onLocationFilterChanged(loc) },
                    )
                }
            }
        }

        // ── Device list
        if (state.devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "NO_DEVICES_FOUND",
                        color      = TextDisabled,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle  = FontStyle.Italic,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        "Tap + to add a camera",
                        color    = TextDisabled,
                        fontSize = 12.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                val favorites = state.devices.filter { it.isFavorite }
                val others    = state.devices.filter { !it.isFavorite }

                if (favorites.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title    = "FAVORITES",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(favorites, key = { it.id }) { device ->
                        DeviceRow(
                            device          = device,
                            onStream        = { onViewStream(device.id) },
                            onSettings      = { onDeviceSettings(device.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(device) },
                            onReconnect     = { viewModel.reconnect(device) },
                            onDelete        = { viewModel.delete(device) },
                        )
                        HorizontalDivider(color = SurfaceStroke, thickness = 0.5.dp)
                    }
                }

                if (others.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title    = "ALL_DEVICES",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(others, key = { it.id }) { device ->
                        DeviceRow(
                            device          = device,
                            onStream        = { onViewStream(device.id) },
                            onSettings      = { onDeviceSettings(device.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(device) },
                            onReconnect     = { viewModel.reconnect(device) },
                            onDelete        = { viewModel.delete(device) },
                        )
                        HorizontalDivider(color = SurfaceStroke, thickness = 0.5.dp)
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: DeviceProfile,
    onStream: () -> Unit,
    onSettings: () -> Unit,
    onToggleFavorite: () -> Unit,
    onReconnect: () -> Unit,
    onDelete: () -> Unit,
) {
    val (statusColor, statusLabel) = deviceStatusDisplay(device.state)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDeep)
            .clickable(onClick = onStream)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // thumbnail placeholder
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 52.dp)
                .chamferClip()
                .background(SurfaceHighest),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.live_view),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = device.name,
                    color      = if (device.isEnabled) TextPrimary else TextDisabled,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text     = "${device.host}:${device.port}",
                color    = TextDisabled,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // status pill
                Row(
                    modifier = Modifier
                        .background(
                            statusColor.copy(alpha = 0.12f),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PulseDot(
                        color   = statusColor,
                        size    = 5.dp,
                        animate = device.state == DeviceState.ONLINE.name,
                    )
                    Text(
                        text     = statusLabel,
                        color    = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                    )
                }
                SourceTypeBadge(device.protocol)
                if (device.location.isNotBlank()) {
                    Text(
                        text     = device.location.uppercase(),
                        color    = TextDisabled,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }

        // action icons
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Icon(
                imageVector = if (device.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint     = if (device.isFavorite) OrangePrimary else TextDisabled,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onToggleFavorite),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Reconnect",
                    tint     = CyanTertiaryDim,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onReconnect),
                )
                Image(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = "Settings",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onSettings),
                )
                Image(
                    painter = painterResource(R.drawable.delete_cam),
                    contentDescription = "Delete",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onDelete),
                )
            }
        }
    }
}

@Composable
private fun LocationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text       = label,
        color      = if (selected) OrangePrimary else TextSecondary,
        fontSize   = 10.sp,
        fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
        letterSpacing = 0.5.sp,
        modifier   = Modifier
            .background(
                if (selected) OrangePrimary.copy(alpha = 0.12f) else SurfaceElevated,
                RoundedCornerShape(2.dp),
            )
            .border(
                1.dp,
                if (selected) OrangePrimary else SurfaceStroke,
                RoundedCornerShape(2.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

private fun deviceStatusDisplay(state: String): Pair<androidx.compose.ui.graphics.Color, String> = when (state) {
    DeviceState.ONLINE.name      -> Pair(GreenOnline,        "ONLINE")
    DeviceState.OFFLINE.name     -> Pair(ErrorRed,           "OFFLINE")
    DeviceState.CONNECTING.name  -> Pair(StatusConnecting,   "LINKING")
    DeviceState.DISABLED.name    -> Pair(StatusDisabled,     "DISABLED")
    else                         -> Pair(StatusDisabled,     "UNKNOWN")
}
