package com.sentinel.companion.ui.screens.devicesettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sentinel.companion.data.model.AuthType
import com.sentinel.companion.data.model.StreamProtocol
import com.sentinel.companion.ui.components.CompanionTopBar
import com.sentinel.companion.ui.components.GhostButton
import com.sentinel.companion.ui.components.MetricRow
import com.sentinel.companion.ui.components.PrimaryButton
import com.sentinel.companion.ui.components.SectionCard
import com.sentinel.companion.ui.components.SectionHeader
import com.sentinel.companion.ui.components.SettingsRow
import com.sentinel.companion.ui.components.carbonBackground
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.CyanTertiaryDim
import com.sentinel.companion.ui.theme.ErrorRed
import com.sentinel.companion.ui.theme.GreenOnline
import com.sentinel.companion.ui.theme.OrangePrimary
import com.sentinel.companion.ui.theme.SurfaceElevated
import com.sentinel.companion.ui.theme.SurfaceHighest
import com.sentinel.companion.ui.theme.SurfaceStroke
import com.sentinel.companion.ui.theme.TextDisabled
import com.sentinel.companion.ui.theme.TextPrimary
import com.sentinel.companion.ui.theme.TextSecondary

@Composable
fun DeviceSettingsScreen(
    deviceId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: DeviceSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(deviceId) { viewModel.load(deviceId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
            .carbonBackground(),
    ) {
        CompanionTopBar(
            title      = state.name.ifBlank { "DEVICE_CONFIG" }.uppercase(),
            subtitle   = state.device?.host ?: "Loading...",
            isConnected = state.isEnabled,
            onBack     = onBack,
        )

        if (!state.isLoaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Quick toggles
            SectionCard {
                SectionHeader(title = "DEVICE_CONTROL")
                Spacer(Modifier.height(8.dp))
                SettingsRow(
                    icon     = Icons.Filled.PowerSettingsNew,
                    iconTint = if (state.isEnabled) GreenOnline else TextDisabled,
                    title    = "ENABLED",
                    subtitle = if (state.isEnabled) "Monitoring active" else "Device disabled",
                    trailing = {
                        Switch(
                            checked  = state.isEnabled,
                            onCheckedChange = { viewModel.toggleEnabled() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor  = GreenOnline,
                                checkedTrackColor  = GreenOnline.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextDisabled,
                                uncheckedTrackColor = SurfaceHighest,
                            ),
                        )
                    },
                )
                HorizontalDivider(color = SurfaceStroke, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                SettingsRow(
                    icon     = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    iconTint = if (state.isFavorite) OrangePrimary else TextDisabled,
                    title    = "FAVORITE",
                    subtitle = if (state.isFavorite) "Pinned to top of roster" else "Not favorited",
                    trailing = {
                        Switch(
                            checked  = state.isFavorite,
                            onCheckedChange = { viewModel.toggleFavorite() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor  = OrangePrimary,
                                checkedTrackColor  = OrangePrimary.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextDisabled,
                                uncheckedTrackColor = SurfaceHighest,
                            ),
                        )
                    },
                )
            }

            // ── Identity
            SectionCard {
                SectionHeader(title = "IDENTITY")
                Spacer(Modifier.height(12.dp))
                SettingsTextField(
                    value         = state.name,
                    onValueChange = viewModel::onNameChanged,
                    label         = "DEVICE NAME",
                    modifier      = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                SettingsTextField(
                    value         = state.location,
                    onValueChange = viewModel::onLocationChanged,
                    label         = "LOCATION / ZONE",
                    modifier      = Modifier.fillMaxWidth(),
                )
            }

            // ── Connection
            SectionCard {
                SectionHeader(title = "CONNECTION")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsTextField(
                        value         = state.host,
                        onValueChange = viewModel::onHostChanged,
                        label         = "HOST / IP",
                        modifier      = Modifier.weight(2f),
                    )
                    SettingsTextField(
                        value         = state.port,
                        onValueChange = viewModel::onPortChanged,
                        label         = "PORT",
                        keyboardType  = KeyboardType.Number,
                        modifier      = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                SettingsTextField(
                    value         = state.path,
                    onValueChange = viewModel::onPathChanged,
                    label         = "STREAM PATH",
                    modifier      = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("PROTOCOL", color = TextDisabled, fontSize = 10.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                ProtocolGrid(
                    selected = state.protocol,
                    onSelect = viewModel::onProtocolChanged,
                )
            }

            // ── Credentials
            SectionCard {
                SectionHeader(title = "CREDENTIALS")
                Spacer(Modifier.height(8.dp))
                Text("AUTH TYPE", color = TextDisabled, fontSize = 10.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                AuthTypeGrid(
                    selected = state.authType,
                    onSelect = viewModel::onAuthTypeChanged,
                )
                if (state.authType != AuthType.NONE.name) {
                    Spacer(Modifier.height(12.dp))
                    SettingsTextField(
                        value         = state.username,
                        onValueChange = viewModel::onUsernameChanged,
                        label         = "USERNAME",
                        modifier      = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingsTextField(
                        value         = state.password,
                        onValueChange = viewModel::onPasswordChanged,
                        label         = "PASSWORD",
                        isPassword    = true,
                        modifier      = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Stream preview
            state.device?.let { device ->
                SectionCard {
                    SectionHeader(title = "STREAM_URL")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = buildPreview(state),
                        color = CyanTertiaryDim,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            state.error?.let { err ->
                Text(
                    "ERROR: $err",
                    color    = ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ErrorRed.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                )
            }

            // ── Save
            PrimaryButton(
                text    = if (state.isSaving) "SAVING..." else "SAVE_CHANGES",
                onClick = { viewModel.save(onBack) },
                enabled = !state.isSaving,
                icon    = Icons.Filled.Save,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Delete
            GhostButton(
                text    = "DELETE_DEVICE",
                onClick = { viewModel.delete(onDeleted) },
                color   = ErrorRed,
                icon    = Icons.Filled.Delete,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProtocolGrid(selected: String, onSelect: (String) -> Unit) {
    val protocols = StreamProtocol.entries
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        protocols.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { proto ->
                    SettingsChip(
                        label    = proto.label,
                        selected = selected == proto.name,
                        onClick  = { onSelect(proto.name) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AuthTypeGrid(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AuthType.entries.forEach { type ->
            SettingsChip(
                label    = type.name,
                selected = selected == type.name,
                onClick  = { onSelect(type.name) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SettingsChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                if (selected) OrangePrimary.copy(alpha = 0.15f) else SurfaceElevated,
                RoundedCornerShape(6.dp),
            )
            .border(1.dp, if (selected) OrangePrimary else SurfaceStroke, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = label,
            color      = if (selected) OrangePrimary else TextSecondary,
            fontSize   = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 0.3.sp,
        )
    }
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label = {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine    = true,
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = CyanTertiaryDim,
            unfocusedBorderColor = SurfaceStroke,
            focusedLabelColor    = CyanTertiaryDim,
            unfocusedLabelColor  = TextDisabled,
            cursorColor          = CyanTertiaryDim,
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary,
        ),
        modifier = modifier,
    )
}

private fun buildPreview(state: DeviceSettingsUiState): String {
    val proto  = StreamProtocol.entries.firstOrNull { it.name == state.protocol } ?: StreamProtocol.RTSP
    val scheme = when (proto) {
        StreamProtocol.RTSP, StreamProtocol.ONVIF -> "rtsp"
        else -> "http"
    }
    val auth = if (state.username.isNotBlank()) "${state.username}:****@" else ""
    val path = state.path.ifBlank { "/" }.let { if (it.startsWith("/")) it else "/$it" }
    return "$scheme://$auth${state.host}:${state.port}$path"
}
