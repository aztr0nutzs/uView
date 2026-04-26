package com.sentinel.app.features.settings

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.app.data.preferences.AppPreferences
import com.sentinel.app.ui.components.SectionCard
import com.sentinel.app.ui.components.SettingsDivider
import com.sentinel.app.ui.components.SettingsNavRow
import com.sentinel.app.ui.components.SettingsToggleRow
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanSubtle
import com.sentinel.app.ui.theme.SentinelTheme
import com.sentinel.app.ui.theme.TextDisabled
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary
import com.sentinel.app.ui.theme.WarningAmber

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateDiagnostics: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val appLockError by viewModel.appLockError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Show error toast when app lock cannot be enabled
    LaunchedEffect(appLockError) {
        appLockError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearAppLockError()
        }
    }

    SettingsContent(
        settings              = settings,
        onNavigateBack        = onNavigateBack,
        onNavigateDiagnostics = onNavigateDiagnostics,
        onSetDarkTheme        = viewModel::setDarkTheme,
        onSetAutoReconnect    = viewModel::setAutoReconnect,
        onSetNotifications    = viewModel::setNotifications,
        onSetLocalOnly        = viewModel::setLocalOnly,
        onSetDataSaver        = viewModel::setDataSaver,
        onSetDiagnosticsLogging = viewModel::setDiagnosticsLogging,
        onSetNetworkScan      = viewModel::setNetworkScan,
        isMonitoringRunning   = viewModel.isMonitoringServiceRunning,
        onStartMonitoring     = viewModel::startBackgroundMonitoring,
        onStopMonitoring      = viewModel::stopBackgroundMonitoring,
        onSetAppLock          = { enabled ->
            activity?.let { viewModel.setAppLock(enabled, it) }
        }
    )
}

@Composable
private fun SettingsContent(
    settings: AppPreferences,
    onNavigateBack: () -> Unit,
    onNavigateDiagnostics: () -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetAutoReconnect: (Boolean) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetLocalOnly: (Boolean) -> Unit,
    onSetDataSaver: (Boolean) -> Unit,
    onSetDiagnosticsLogging: (Boolean) -> Unit,
    onSetNetworkScan: (Boolean) -> Unit,
    isMonitoringRunning: Boolean = false,
    onStartMonitoring: () -> Unit = {},
    onStopMonitoring: () -> Unit = {},
    onSetAppLock: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
            }
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Appearance ────────────────────────────────────────────────
            item {
                SectionCard(title = "Appearance") {
                    SettingsToggleRow(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Theme",
                        subtitle = "Use dark interface (recommended)",
                        checked = settings.darkThemeEnabled,
                        onCheckedChange = onSetDarkTheme
                    )
                }
            }

            // ── Stream & Playback ─────────────────────────────────────────
            item {
                SectionCard(title = "Stream & Playback") {
                    SettingsInfoRow(
                        icon = Icons.Default.Videocam,
                        title = "Default Stream Quality",
                        subtitle = "Per-camera quality is active; global default override is not wired in this build",
                        valueLabel = settings.defaultStreamQuality.label,
                        availability = "UNAVAILABLE"
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon = Icons.Default.SignalCellularAlt,
                        title = "Data Saver Mode",
                        subtitle = "Force lowest quality to reduce bandwidth",
                        checked = settings.dataSaverMode,
                        onCheckedChange = onSetDataSaver
                    )
                }
            }

            // ── Notifications ─────────────────────────────────────────────
            item {
                SectionCard(title = "Notifications") {
                    SettingsToggleRow(
                        icon = Icons.Default.Notifications,
                        title = "Push Notifications",
                        subtitle = "Alerts for motion, connection changes",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = onSetNotifications
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        icon = Icons.Default.Settings,
                        title = "Motion Sensitivity",
                        subtitle = "Global sensitivity control is not connected; live motion uses per-session MEDIUM profile",
                        valueLabel = settings.motionSensitivity.label,
                        availability = "UNAVAILABLE"
                    )
                }
            }

            // ── Network ───────────────────────────────────────────────────
            item {
                SectionCard(title = "Network") {
                    SettingsToggleRow(
                        icon = Icons.Default.WifiOff,
                        title = "Local-Only Mode",
                        subtitle = "Block all external network access for cameras",
                        checked = settings.localOnlyMode,
                        onCheckedChange = onSetLocalOnly
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        icon = Icons.Default.Radar,
                        title = "Network Scan",
                        subtitle = "LAN discovery runs only when launched from the scan screen; this preference toggle is not wired",
                        valueLabel = if (settings.networkScanEnabled) "ON" else "OFF",
                        availability = "ON_DEMAND_ONLY"
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        icon = Icons.Default.NetworkCheck,
                        title = "Scan Subnet",
                        subtitle = "Manual subnet override UI is not implemented yet",
                        valueLabel = settings.networkScanSubnet.ifBlank { "Auto" },
                        availability = "UNAVAILABLE"
                    )
                }
            }

            // ── Storage ───────────────────────────────────────────────────
            item {
                SectionCard(title = "Storage") {
                    SettingsInfoRow(
                        icon = Icons.Default.FolderOpen,
                        title = "Recording Save Path",
                        subtitle = "Custom folder picker is not wired; recordings use app-managed Movies/SentinelRecordings",
                        valueLabel = "APP MANAGED",
                        availability = "LOCKED"
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        icon = Icons.Default.Settings,
                        title = "Event Retention",
                        subtitle = "Retention pruning pipeline is not wired to this preference yet",
                        valueLabel = "${settings.eventRetentionDays} days",
                        availability = "UNAVAILABLE"
                    )
                }
            }

            // ── Security ──────────────────────────────────────────────────
            item {
                SectionCard(title = "Security") {
                    SettingsToggleRow(
                        icon = Icons.Default.Lock,
                        title = "App Lock",
                        subtitle = if (settings.appLockEnabled)
                            "Biometric or PIN required to open"
                        else
                            "Require authentication to access cameras",
                        checked = settings.appLockEnabled,
                        onCheckedChange = onSetAppLock,
                        iconTint = WarningAmber,
                        iconBg = WarningAmber.copy(alpha = 0.12f)
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy & Permissions",
                        subtitle = "Permission center is not implemented in this build",
                        valueLabel = "PENDING",
                        availability = "UNAVAILABLE",
                        iconTint = WarningAmber,
                        iconBg = WarningAmber.copy(alpha = 0.12f)
                    )
                }
            }

            // ── Background Monitoring ────────────────────────────────────
            item {
                SectionCard(title = "Background Monitoring") {
                    SettingsToggleRow(
                        icon    = Icons.Default.Shield,
                        title   = "Background Monitoring",
                        subtitle = if (isMonitoringRunning)
                            "Service running — motion detection active in background"
                        else
                            "Keep cameras monitored when app is closed",
                        checked = isMonitoringRunning,
                        onCheckedChange = { enabled ->
                            if (enabled) onStartMonitoring() else onStopMonitoring()
                        }
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon    = Icons.Default.Refresh,
                        title   = "Auto Reconnect",
                        subtitle = "Automatically retry disconnected streams",
                        checked = settings.autoReconnectEnabled,
                        onCheckedChange = onSetAutoReconnect
                    )
                }
            }

            // ── Developer / Diagnostics ───────────────────────────────────
            item {
                SectionCard(title = "Developer") {
                    SettingsToggleRow(
                        icon = Icons.Default.Science,
                        title = "Diagnostics Logging",
                        subtitle = "Log stream events and connection details",
                        checked = settings.diagnosticsLoggingEnabled,
                        onCheckedChange = onSetDiagnosticsLogging
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon = Icons.Default.NetworkCheck,
                        title = "Diagnostics",
                        subtitle = "Run TCP reachability checks; stream decode and credentials are not tested",
                        onClick = onNavigateDiagnostics
                    )
                }
            }

            // ── About ─────────────────────────────────────────────────────
            item {
                SectionCard(title = "About") {
                    SettingsInfoRow(
                        icon = Icons.Default.Info,
                        title = "Sentinel Home",
                        subtitle = "Version 1.0.0 — Personal camera hub",
                        valueLabel = "INFO",
                        availability = null
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        icon = Icons.Default.Security,
                        title = "Privacy Statement",
                        subtitle = "This app monitors only user-added cameras on your network",
                        valueLabel = "LOCAL",
                        availability = null,
                        iconTint = com.sentinel.app.ui.theme.StatusOnline,
                        iconBg = com.sentinel.app.ui.theme.StatusOnline.copy(alpha = 0.12f)
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueLabel: String? = null,
    availability: String? = null,
    iconTint: androidx.compose.ui.graphics.Color = TextDisabled,
    iconBg: androidx.compose.ui.graphics.Color = CyanSubtle.copy(alpha = 0.55f)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextDisabled
            )
            availability?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningAmber,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        valueLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = TextDisabled
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060B10)
@Composable
private fun SettingsPreview() {
    SentinelTheme {
        SettingsContent(
            settings = AppPreferences(),
            onNavigateBack = {}, onNavigateDiagnostics = {},
            onSetDarkTheme = {}, onSetAutoReconnect = {}, onSetNotifications = {},
            onSetLocalOnly = {}, onSetDataSaver = {}, onSetDiagnosticsLogging = {},
            onSetNetworkScan = {}, onSetAppLock = {}
        )
    }
}
