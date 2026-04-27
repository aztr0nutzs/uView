package com.sentinel.companion.ui.screens.alerts

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.res.painterResource
import com.sentinel.companion.R
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.sentinel.companion.ui.components.CompanionTopBar
import com.sentinel.companion.ui.components.alertColor
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.CyanSubtleBg
import com.sentinel.companion.ui.theme.CyanTertiaryDim
import com.sentinel.companion.ui.theme.ErrorRed
import com.sentinel.companion.ui.theme.GreenOnline
import com.sentinel.companion.ui.theme.OrangePrimary
import com.sentinel.companion.ui.theme.OrangeSubtle
import com.sentinel.companion.ui.theme.SurfaceBase
import com.sentinel.companion.ui.theme.SurfaceElevated
import com.sentinel.companion.ui.theme.SurfaceHighest
import com.sentinel.companion.ui.theme.SurfaceLowest
import com.sentinel.companion.ui.theme.SurfaceStroke
import com.sentinel.companion.ui.theme.TextDisabled
import com.sentinel.companion.ui.theme.TextPrimary
import com.sentinel.companion.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val alertTypeFilters = listOf("ALL") + AlertType.entries.map { it.name }

@Composable
fun AlertsScreen(viewModel: AlertsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep),
    ) {
        CompanionTopBar(
            title    = "EVENT_LOG",
            subtitle = "${state.filteredAlerts.size} EVENTS // ${state.unreadCount} UNREAD",
            trailing = {
                if (state.unreadCount > 0) {
                    Icon(
                        imageVector = Icons.Filled.DoneAll,
                        contentDescription = "Mark all read",
                        tint = CyanTertiaryDim,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { viewModel.markAllRead() },
                    )
                }
            },
        )

        // ── Event type filter ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            alertTypeFilters.forEach { type ->
                val selected = type == state.selectedType
                val label = if (type == "ALL") "ALL" else AlertType.valueOf(type).label.uppercase()
                Text(
                    text = label,
                    color = if (selected) BackgroundDeep else TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .background(
                            if (selected) OrangePrimary else SurfaceBase,
                            RoundedCornerShape(20.dp),
                        )
                        .border(1.dp, if (selected) OrangePrimary else SurfaceStroke, RoundedCornerShape(20.dp))
                        .clickable { viewModel.onTypeSelected(type) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        // ── Camera filter ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.cameraNames.forEach { cam ->
                val selected = cam == state.selectedCamera
                Text(
                    text = cam,
                    color = if (selected) BackgroundDeep else CyanTertiaryDim,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(
                            if (selected) CyanTertiaryDim else CyanSubtleBg,
                            RoundedCornerShape(4.dp),
                        )
                        .border(1.dp, CyanTertiaryDim.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .clickable { viewModel.onCameraSelected(cam) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Event list ─────────────────────────────────────────────────────
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(state.filteredAlerts, key = { it.id }) { alert ->
                AlertRow(
                    alert   = alert,
                    onClick = { viewModel.markRead(alert.id) },
                )
                HorizontalDivider(color = SurfaceLowest, thickness = 1.dp)
            }
            if (state.filteredAlerts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CheckCircle, null, tint = GreenOnline.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("NO_EVENTS", color = TextDisabled, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Single alert row ─────────────────────────────────────────────────────────

@Composable
private fun AlertRow(alert: Alert, onClick: () -> Unit) {
    val type       = alert.typeEnum()
    val accentColor = alertColor(type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (!alert.isRead) SurfaceBase else BackgroundDeep)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AlertGlyph(type = type, accentColor = accentColor)
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = alert.cameraName,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = type.label.uppercase(),
                    color = accentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            }
            Text(
                text = alert.message,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                text = formatTimestamp(alert.timestampMs),
                color = TextDisabled,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        if (!alert.isRead) {
            Box(Modifier.size(8.dp).background(OrangePrimary, RoundedCornerShape(50)))
        }
    }
}

@Composable
private fun AlertGlyph(type: AlertType, accentColor: Color) {
    val customRes: Int? = when (type) {
        AlertType.MOTION            -> R.drawable.alerts
        AlertType.RECORDING_STARTED -> R.drawable.record
        AlertType.SNAPSHOT          -> R.drawable.snapshot
        else                        -> null
    }
    if (customRes != null) {
        Image(
            painter = painterResource(customRes),
            contentDescription = null,
            modifier = Modifier.size(26.dp),
        )
    } else {
        val vector: ImageVector = when (type) {
            AlertType.CONNECTION_LOST     -> Icons.Filled.WifiOff
            AlertType.CONNECTION_RESTORED -> Icons.Filled.Wifi
            AlertType.RECORDING_STOPPED   -> Icons.Filled.Stop
            AlertType.SYSTEM              -> Icons.Filled.Info
            else                          -> Icons.Filled.Info
        }
        Icon(
            imageVector = vector,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun formatTimestamp(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000     -> "${diff / 1000}s ago"
        diff < 3_600_000  -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else              -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(ms))
    }
}
