package com.sentinel.app.features.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
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
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.app.data.preferences.AppPreferences
import com.sentinel.app.ui.components.SectionCard
import com.sentinel.app.ui.components.SettingsDivider
import com.sentinel.app.ui.components.SettingsNavRow
import com.sentinel.app.ui.components.SettingsToggleRow
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.SentinelTheme
import com.sentinel.app.ui.theme.StatusOffline
import com.sentinel.app.ui.theme.SurfaceStroke
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary
import com.sentinel.app.ui.theme.WarningAmber

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateDiagnostics: () -> Unit,
    onNavigatePrivacy: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsContent(
        settings              = settings,
        onNavigateBack        = onNavigateBack,
        onNavigateDiagnostics = onNavigateDiagnostics,
        onNavigatePrivacy     = onNavigatePrivacy,
        onSetDarkTheme        = viewModel::setDarkTheme,
        onSetAutoReconnect    = viewModel::setAutoReconnect,
        onSetNotifications    = viewModel::setNotifications,
        onSetLocalOnly        = viewModel::setLocalOnly,
        onSetDataSaver        = viewModel::setDataSaver,
        onSetDiagnosticsLogging = viewModel::setDiagnosticsLogging,
        onSetNetworkScan      = viewModel::setNetworkScan,
        isMonitoringRunning   = viewModel.isMonitoringServiceRunning,
        onStartMonitoring     = viewModel::startBackgroundMonitoring,
        onStopMonitoring      = viewModel::stopBackgroundMonitoring
    )
}

@Composable
private fun SettingsContent(
    settings: AppPreferences,
    onNavigateBack: () -> Unit,
    onNavigateDiagnostics: () -> Unit,
    onNavigatePrivacy: () -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetAutoReconnect: (Boolean) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetLocalOnly: (Boolean) -> Unit,
    onSetDataSaver: (Boolean) -> Unit,
    onSetDiagnosticsLogging: (Boolean) -> Unit,
    onSetNetworkScan: (Boolean) -> Unit,
    isMonitoringRunning: Boolean = false,
    onStartMonitoring: () -> Unit = {},
    onStopMonitoring: () -> Unit = {}
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
                    SettingsNavRow(
                        icon = Icons.Default.Videocam,
                        title = "Default Stream Quality",
                        subtitle = "Quality used when opening a new feed",
                        valueLabel = settings.defaultStreamQuality.label,
                        onClick = { /* future: quality picker bottom sheet */ }
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
                    SettingsNavRow(
                        icon = Icons.Default.Settings,
                        title = "Motion Sensitivity",
                        subtitle = "Pixels must change by this amount to count as motion",
                        valueLabel = settings.motionSensitivity.label,
                        onClick = { /* Phase 5: show bottom sheet with LOW/MEDIUM/HIGH/DISABLED options */ }
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
                    SettingsToggleRow(
                        icon = Icons.Default.Radar,
                        title = "Network Scan",
                        subtitle = "Allow LAN scanning for new camera discovery",
                        checked = settings.networkScanEnabled,
                        onCheckedChange = onSetNetworkScan
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon = Icons.Default.NetworkCheck,
                        title = "Scan Subnet",
                        subtitle = "Override subnet for discovery (empty = auto)",
                        valueLabel = settings.networkScanSubnet.ifBlank { "Auto" },
                        onClick = { /* future: input dialog */ }
                    )
                }
            }

            // ── Storage ───────────────────────────────────────────────────
            item {
                SectionCard(title = "Storage") {
                    SettingsNavRow(
                        icon = Icons.Default.FolderOpen,
                        title = "Recording Save Path",
                        subtitle = "Where local recordings are saved",
                        valueLabel = if (settings.localStoragePath.isBlank()) "Default" else "Custom",
                        onClick = { /* future: folder picker */ }
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon = Icons.Default.Settings,
                        title = "Event Retention",
                        subtitle = "How long to keep event history",
                        valueLabel = "${settings.eventRetentionDays} days",
                        onClick = { /* Phase 5: show bottom sheet with LOW/MEDIUM/HIGH/DISABLED options */ }
                    )
                }
            }

            // ── Security ──────────────────────────────────────────────────
            item {
                SectionCard(title = "Security") {
                    SettingsNavRow(
                        icon = Icons.Default.Lock,
                        title = "App Lock",
                        subtitle = "Require biometric or PIN to open (future)",
                        valueLabel = if (settings.appLockEnabled) "On" else "Off",
                        onClick = { /* Phase 5: show bottom sheet with LOW/MEDIUM/HIGH/DISABLED options */ },
                        iconTint = WarningAmber,
                        iconBg = WarningAmber.copy(alpha = 0.12f)
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy & Permissions",
                        subtitle = "Review app permissions and data usage",
                        onClick = onNavigatePrivacy,
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
                        subtitle = "Run connection tests and view debug info",
                        onClick = onNavigateDiagnostics
                    )
                }
            }

            // ── About ─────────────────────────────────────────────────────
            item {
                SectionCard(title = "About") {
                    SettingsNavRow(
                        icon = Icons.Default.Info,
                        title = "Sentinel Home",
                        subtitle = "Version 1.0.0 — Personal camera hub",
                        onClick = {}
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon = Icons.Default.Security,
                        title = "Privacy Statement",
                        subtitle = "This app monitors only user-added cameras on your network",
                        onClick = onNavigatePrivacy,
                        iconTint = com.sentinel.app.ui.theme.StatusOnline,
                        iconBg = com.sentinel.app.ui.theme.StatusOnline.copy(alpha = 0.12f)
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060B10)
@Composable
private fun SettingsPreview() {
    SentinelTheme {
        SettingsContent(
            settings = AppPreferences(),
            onNavigateBack = {}, onNavigateDiagnostics = {}, onNavigatePrivacy = {},
            onSetDarkTheme = {}, onSetAutoReconnect = {}, onSetNotifications = {},
            onSetLocalOnly = {}, onSetDataSaver = {}, onSetDiagnosticsLogging = {},
            onSetNetworkScan = {}
        )
    }
}
