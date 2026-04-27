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
import com.sentinel.companion.R
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.companion.data.model.StreamQuality
import com.sentinel.companion.security.BiometricAvailability
import com.sentinel.companion.security.biometricAvailability
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
    val context = LocalContext.current
    val authAvailable = biometricAvailability(context) == BiometricAvailability.AVAILABLE

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
                        iconRes = R.drawable.net_config,
                        accent = if (conn.hostAddress.isNotBlank()) GreenOnline else TextDisabled,
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
                    // Push & motion alerts depend on a server-side push pipeline that has
                    // not shipped yet — surface them as disabled rather than as toggles
                    // that silently do nothing when flipped.
                    SettingsRow(
                        iconRes = R.drawable.alerts,
                        accent = TextDisabled,
                        title = "PUSH_NOTIFICATIONS",
                        subtitle = "Requires Sentinel Hub push pipeline (not yet available)",
                        trailing = {
                            Switch(
                                checked = false,
                                onCheckedChange = null,
                                enabled = false,
                                colors = switchColors(),
                            )
                        },
                    )
                    HorizontalDivider(color = SurfaceStroke, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
                    SettingsRow(
                        iconRes = R.drawable.devices,
                        accent = TextDisabled,
                        title = "MOTION_ALERTS",
                        subtitle = "Requires server-side motion events (not yet available)",
                        trailing = {
                            Switch(
                                checked = false,
                                onCheckedChange = null,
                                enabled = false,
                                colors = switchColors(),
                            )
                        },
                    )
                    HorizontalDivider(color = SurfaceStroke, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
                    // Connection alerts ARE wired — the sync service writes them to the
                    // alerts feed on every observed transition; the toggle controls
                    // whether they are surfaced. Persist the preference so the alerts
                    // feed reflects the user's choice even though we don't yet emit OS
                    // notifications.
                    SettingsRow(
                        icon = Icons.Filled.SignalCellularAlt,
                        iconTint = CyanTertiaryDim,
                        title = "CONNECTION_ALERTS",
                        subtitle = "Show camera connect/disconnect events in alerts feed",
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
                    // The stream player does not yet honor data-saver or quality
                    // selection. Disable the controls so they don't claim to work.
                    SettingsRow(
                        icon = Icons.Filled.DataSaverOn,
                        iconTint = TextDisabled,
                        title = "DATA_SAVER",
                        subtitle = "Stream player does not yet honor this preference",
                        trailing = {
                            Switch(
                                checked = false,
                                onCheckedChange = null,
                                enabled = false,
                                colors = switchColors(),
                            )
                        },
                    )
                    HorizontalDivider(color = SurfaceStroke, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))

                    // Stream quality selector — read-only, locked to AUTO until the
                    // player exposes quality switching.
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("STREAM_QUALITY", color = TextDisabled, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            "Locked to AUTO — manual quality selection ships with the player upgrade",
                            color = TextDisabled, fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StreamQuality.entries.forEach { quality ->
                                val selected = quality == StreamQuality.AUTO
                                Text(
                                    text = quality.label.uppercase(),
                                    color = if (selected) BackgroundDeep else TextDisabled,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (selected) TextDisabled else SurfaceBase,
                                            RoundedCornerShape(8.dp),
                                        )
                                        .border(1.dp, SurfaceStroke, RoundedCornerShape(8.dp))
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
                        iconTint = if (authAvailable || prefs.biometricLock) OrangePrimary else TextDisabled,
                        title = "BIOMETRIC_LOCK",
                        subtitle = if (authAvailable)
                            "Require biometric or device credential on app launch and resume"
                        else "Unavailable until a biometric or device credential is enrolled",
                        trailing = {
                            Switch(
                                checked = prefs.biometricLock,
                                onCheckedChange = viewModel::setBiometricLock,
                                enabled = authAvailable || prefs.biometricLock,
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
                        iconRes = R.drawable.uview_icon,
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
