package com.sentinel.companion.ui.screens.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.companion.data.model.StreamQuality
import com.sentinel.companion.ui.components.CompanionTopBar
import com.sentinel.companion.ui.components.SectionCard
import com.sentinel.companion.ui.components.SectionHeader
import com.sentinel.companion.ui.components.SettingsRow
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.CyanTertiaryDim
import com.sentinel.companion.ui.theme.ErrorContainer
import com.sentinel.companion.ui.theme.ErrorRed
import com.sentinel.companion.ui.theme.GreenOnline
import com.sentinel.companion.ui.theme.OrangePrimary
import com.sentinel.companion.ui.theme.OrangeSubtle
import com.sentinel.companion.ui.theme.SurfaceBase
import com.sentinel.companion.ui.theme.SurfaceStroke
import com.sentinel.companion.ui.theme.TextDisabled
import com.sentinel.companion.ui.theme.TextPrimary
import com.sentinel.companion.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onDisconnect: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.appPrefs
    val conn  = state.connectionPrefs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep),
    ) {
        CompanionTopBar(title = "SYSTEM_CONFIG", subtitle = "SENTINEL_COMPANION // v1.0")

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Connection ──────────────────────────────────────────────────
            item { SectionHeader(title = "CONNECTION") }
            item {
                SectionCard {
                    SettingsRow(
                        icon = Icons.Filled.Wifi,
                        iconTint = if (conn.hostAddress.isNotBlank()) GreenOnline else TextDisabled,
                        title = "HOST_ADDRESS",
                        subtitle = if (conn.hostAddress.isNotBlank())
                            "${if (conn.useHttps) "https" else "http"}://${conn.hostAddress}:${conn.port}"
                        else "NOT_CONFIGURED",
                    )
                    HorizontalDivider(color = SurfaceStroke, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
                    SettingsRow(
                        icon = Icons.Filled.Shield,
                        iconTint = if (conn.useHttps) CyanTertiaryDim else TextDisabled,
                        title = "TLS_ENCRYPTION",
                        subtitle = if (conn.useHttps) "HTTPS enabled" else "HTTP (unencrypted)",
                    )
                    HorizontalDivider(color = SurfaceStroke, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
                    SettingsRow(
                        icon = Icons.Filled.WifiOff,
                        iconTint = ErrorRed,
                        title = "DISCONNECT",
                        subtitle = "Remove host and return to connect screen",
                        onClick = { viewModel.disconnect(onDisconnect) },
                        trailing = {
                            Icon(Icons.Filled.ChevronRight, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                        },
                    )
                }
            }

            // ── Notifications ───────────────────────────────────────────────
            item { SectionHeader(title = "NOTIFICATIONS") }
            item {
                SectionCard {
                    SettingsRow(
                        icon = Icons.Filled.Notifications,
                        iconTint = OrangePrimary,
                        title = "PUSH_NOTIFICATIONS",
                        subtitle = "Enable all push alerts",
                        trailing = {
                            Switch(
                                checked = prefs.notificationsEnabled,
                                onCheckedChange = viewModel::setNotifications,
                                colors = switchColors(),
                            )
                        },
                    )
                    HorizontalDivider(color = SurfaceStroke, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
                    SettingsRow(
                        icon = Icons.Filled.Videocam,
                        iconTint = OrangePrimary,
                        title = "MOTION_ALERTS",
                        subtitle = "Notify on motion detection",
                        trailing = {
                            Switch(
                                checked = prefs.motionAlertsEnabled,
                                onCheckedChange = viewModel::setMotionAlerts,
                                colors = switchColors(),
                            )
                        },
                    )
                    HorizontalDivider(color = SurfaceStroke, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
                    SettingsRow(
                        icon = Icons.Filled.SignalCellularAlt,
                        iconTint = CyanTertiaryDim,
                        title = "CONNECTION_ALERTS",
                        subtitle = "Notify on camera connect/disconnect",
                        trailing = {
                            Switch(
                                checked = prefs.connectionAlertsEnabled,
                                onCheckedChange = viewModel::setConnectionAlerts,
                                colors = switchColors(),
                            )
                        },
                    )
                }
            }

            // ── Stream & Playback ───────────────────────────────────────────
            item { SectionHeader(title = "STREAM_PLAYBACK") }
            item {
                SectionCard {
                    SettingsRow(
                        icon = Icons.Filled.DataSaverOn,
                        iconTint = GreenOnline,
                        title = "DATA_SAVER",
                        subtitle = "Reduce stream quality to save bandwidth",
                        trailing = {
                            Switch(
                                checked = prefs.dataSaverMode,
                                onCheckedChange = viewModel::setDataSaver,
                                colors = switchColors(),
                            )
                        },
                    )
                    HorizontalDivider(color = SurfaceStroke, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))

                    // Stream quality selector
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("STREAM_QUALITY", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Text("Select preferred stream quality level", color = TextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StreamQuality.entries.forEach { quality ->
                                val selected = prefs.streamQuality == quality
                                Text(
                                    text = quality.label.uppercase(),
                                    color = if (selected) BackgroundDeep else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (selected) OrangePrimary else SurfaceBase,
                                            RoundedCornerShape(8.dp),
                                        )
                                        .border(1.dp, if (selected) OrangePrimary else SurfaceStroke, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setStreamQuality(quality) }
                                        .padding(vertical = 8.dp)
                                        .then(Modifier),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }

            // ── Security ────────────────────────────────────────────────────
            item { SectionHeader(title = "SECURITY") }
            item {
                SectionCard {
                    SettingsRow(
                        icon = Icons.Filled.Lock,
                        iconTint = OrangePrimary,
                        title = "BIOMETRIC_LOCK",
                        subtitle = "Require biometric auth to open app",
                        trailing = {
                            Switch(
                                checked = prefs.biometricLock,
                                onCheckedChange = viewModel::setBiometricLock,
                                colors = switchColors(),
                            )
                        },
                    )
                    HorizontalDivider(color = SurfaceStroke, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
                    SettingsRow(
                        icon = Icons.Filled.Security,
                        iconTint = CyanTertiaryDim,
                        title = "LOCAL_ONLY_MODE",
                        subtitle = "Block all external network access",
                        trailing = {
                            Switch(
                                checked = prefs.localOnlyMode,
                                onCheckedChange = viewModel::setLocalOnlyMode,
                                colors = switchColors(),
                            )
                        },
                    )
                }
            }

            // ── About ────────────────────────────────────────────────────────
            item { SectionHeader(title = "ABOUT") }
            item {
                SectionCard {
                    SettingsRow(
                        icon = Icons.Filled.PhoneAndroid,
                        iconTint = OrangePrimary,
                        title = "SENTINEL_COMPANION",
                        subtitle = "v1.0.0 // Companion viewer for Sentinel Home",
                    )
                    HorizontalDivider(color = SurfaceStroke, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        iconTint = TextDisabled,
                        title = "BUILD_INFO",
                        subtitle = "Jetpack Compose // Material3 // Hilt // Room",
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = OrangePrimary,
    checkedTrackColor = OrangeSubtle,
    uncheckedThumbColor = TextDisabled,
    uncheckedTrackColor = SurfaceBase,
)
