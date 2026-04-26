package com.sentinel.app.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentinel.app.R
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.CameraEventType
import com.sentinel.app.domain.model.CameraStatus
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanPrimary
import com.sentinel.app.ui.theme.CyanSubtle
import com.sentinel.app.ui.theme.StatusConnecting
import com.sentinel.app.ui.theme.StatusDisabled
import com.sentinel.app.ui.theme.StatusOffline
import com.sentinel.app.ui.theme.StatusOnline
import com.sentinel.app.ui.theme.StatusUnknown
import com.sentinel.app.ui.theme.SurfaceElevated
import com.sentinel.app.ui.theme.SurfaceHighest
import com.sentinel.app.ui.theme.SurfaceStroke
import com.sentinel.app.ui.theme.TextDisabled
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary
import com.sentinel.app.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// StatusChip
// Colored pill showing camera connection state with an animated dot for ONLINE.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatusChip(
    status: CameraStatus,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val (dotColor, label) = when (status) {
        CameraStatus.ONLINE      -> StatusOnline     to "Online"
        CameraStatus.OFFLINE     -> StatusOffline    to "Offline"
        CameraStatus.CONNECTING  -> StatusConnecting to "Connecting"
        CameraStatus.ERROR       -> StatusOffline    to "Error"
        CameraStatus.DISABLED    -> StatusDisabled   to "Disabled"
        CameraStatus.UNKNOWN     -> StatusUnknown    to "Unknown"
    }

    val chipBg = dotColor.copy(alpha = 0.12f)
    val animatedDot by animateColorAsState(
        targetValue = dotColor,
        animationSpec = tween(600),
        label = "statusDot"
    )

    // Pulse animation for CONNECTING state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == CameraStatus.CONNECTING) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(chipBg)
            .padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 3.dp else 5.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 6.dp else 7.dp)
                .clip(CircleShape)
                .background(animatedDot.copy(alpha = pulseAlpha))
        )
        if (!compact) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = dotColor,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SectionCard
// Deep-surface card container for grouping related content.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceStroke, RoundedCornerShape(16.dp))
    ) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 4.dp)
            )
        }
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SettingsRow — toggle variant
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsToggleRow(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = CyanPrimary,
    iconBg: Color = CyanSubtle
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BackgroundDeep,
                checkedTrackColor = CyanPrimary,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceHighest
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SettingsRow — chevron/navigation variant
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsNavRow(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    valueLabel: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = CyanPrimary,
    iconBg: Color = CyanSubtle
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        if (valueLabel != null) {
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextDisabled,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SettingsRow divider
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 68.dp)
            .height(1.dp)
            .background(SurfaceStroke)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// EventRow
// A single event entry in the events feed list.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EventRow(
    event: CameraEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (iconPainter, iconColor) = eventPainterAndColor(event.eventType)
    val timeStr = formatEventTime(event.timestampMs)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (!event.isRead) SurfaceHighest.copy(alpha = 0.5f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = event.cameraName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = if (!event.isRead) FontWeight.SemiBold else FontWeight.Normal
                )
                if (!event.isRead) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary)
                    )
                }
            }
            Text(
                text = event.eventType.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = iconColor,
                fontWeight = FontWeight.Medium
            )
            if (event.description.isNotBlank()) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            // Motion event confidence indicator
            if (event.eventType == com.sentinel.app.domain.model.CameraEventType.MOTION_DETECTED) {
                Box(
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .background(WarningAmber.copy(alpha = 0.12f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        "MOTION",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        Text(
            text = timeStr,
            style = MaterialTheme.typography.labelSmall,
            color = TextDisabled
        )
    }
}

@Composable
private fun eventPainterAndColor(type: CameraEventType): Pair<Painter, Color> {
    return when (type) {
        CameraEventType.MOTION_DETECTED     -> rememberVectorPainter(Icons.Default.DirectionsRun) to WarningAmber
        CameraEventType.CONNECTION_LOST     -> rememberVectorPainter(Icons.Default.WifiOff)        to StatusOffline
        CameraEventType.CONNECTION_RESTORED -> rememberVectorPainter(Icons.Default.Wifi)           to StatusOnline
        CameraEventType.RECORDING_STARTED   -> painterResource(R.drawable.ic_record)              to Color(0xFFFF1744)
        CameraEventType.RECORDING_STOPPED   -> rememberVectorPainter(Icons.Default.Stop)           to TextSecondary
        CameraEventType.SNAPSHOT_TAKEN      -> painterResource(R.drawable.ic_snapshot)            to CyanPrimary
        CameraEventType.CAMERA_ADDED        -> painterResource(R.drawable.ic_add_cam)             to StatusOnline
        CameraEventType.CAMERA_REMOVED      -> painterResource(R.drawable.ic_delete_cam)          to TextSecondary
        CameraEventType.SETTINGS_CHANGED    -> painterResource(R.drawable.ic_settings)            to TextSecondary
        CameraEventType.STREAM_ERROR        -> rememberVectorPainter(Icons.Default.ErrorOutline)   to StatusOffline
    }
}

private fun formatEventTime(timestampMs: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMs
    return when {
        diff < 60_000        -> "just now"
        diff < 3_600_000     -> "${diff / 60_000}m ago"
        diff < 86_400_000    -> "${diff / 3_600_000}h ago"
        else                 -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMs))
    }
}

