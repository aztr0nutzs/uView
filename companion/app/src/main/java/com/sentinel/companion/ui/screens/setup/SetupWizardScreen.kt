package com.sentinel.companion.ui.screens.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sentinel.companion.data.model.AuthType
import com.sentinel.companion.data.model.DiscoveredDevice
import com.sentinel.companion.data.model.StreamProtocol
import com.sentinel.companion.ui.components.CompanionTopBar
import com.sentinel.companion.ui.components.GhostButton
import com.sentinel.companion.ui.components.MetricRow
import com.sentinel.companion.ui.components.PrimaryButton
import com.sentinel.companion.ui.components.PulseDot
import com.sentinel.companion.ui.components.SectionCard
import com.sentinel.companion.ui.components.SectionHeader
import com.sentinel.companion.ui.components.SourceTypeBadge
import com.sentinel.companion.ui.components.carbonBackground
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.CyanTertiaryDim
import com.sentinel.companion.ui.theme.ErrorRed
import com.sentinel.companion.ui.theme.GreenOnline
import com.sentinel.companion.ui.theme.OrangePrimary
import com.sentinel.companion.ui.theme.StatusConnecting
import com.sentinel.companion.ui.theme.SurfaceBase
import com.sentinel.companion.ui.theme.SurfaceElevated
import com.sentinel.companion.ui.theme.SurfaceHighest
import com.sentinel.companion.ui.theme.SurfaceStroke
import com.sentinel.companion.ui.theme.TextDisabled
import com.sentinel.companion.ui.theme.TextPrimary
import com.sentinel.companion.ui.theme.TextSecondary

@Composable
fun SetupWizardScreen(
    onDeviceSaved: (deviceId: String) -> Unit,
    onCancel: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.savedDeviceId) {
        state.savedDeviceId?.let { onDeviceSaved(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
            .carbonBackground(),
    ) {
        CompanionTopBar(
            title     = when (state.step) {
                SetupStep.DISCOVER -> "ADD_DEVICE"
                SetupStep.AUTH     -> "AUTH_CONFIG"
                SetupStep.CONFIRM  -> "CONFIRM_SAVE"
            },
            subtitle  = when (state.step) {
                SetupStep.DISCOVER -> "STEP 1 OF 3 — DISCOVERY"
                SetupStep.AUTH     -> "STEP 2 OF 3 — CREDENTIALS"
                SetupStep.CONFIRM  -> "STEP 3 OF 3 — PROFILE"
            },
            isConnected = false,
            onBack    = if (state.step == SetupStep.DISCOVER) onCancel else viewModel::goBack,
        )

        StepIndicator(current = state.step)

        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()) togetherWith
                (slideOutHorizontally { -it } + fadeOut())
            },
            label = "stepContent",
        ) { step ->
            when (step) {
                SetupStep.DISCOVER -> DiscoverStep(state, viewModel)
                SetupStep.AUTH     -> AuthStep(state, viewModel)
                SetupStep.CONFIRM  -> ConfirmStep(state, viewModel)
            }
        }
    }
}

@Composable
private fun StepIndicator(current: SetupStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SetupStep.entries.forEach { step ->
            val active  = step == current
            val done    = step.ordinal < current.ordinal
            val color   = when {
                active -> OrangePrimary
                done   -> GreenOnline
                else   -> SurfaceHighest
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
    }
}

// ── Step 1: Discover ──────────────────────────────────────────────────────────

@Composable
private fun DiscoverStep(state: SetupUiState, vm: SetupViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Scan controls
        SectionCard {
            SectionHeader(title = "NETWORK_SCAN")
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PrimaryButton(
                    text    = if (state.isScanning) "SCANNING..." else "START_SCAN",
                    onClick = if (state.isScanning) vm::stopScan else vm::startScan,
                    icon    = Icons.Filled.Wifi,
                    enabled = true,
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.isScanning) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(
                        color     = OrangePrimary,
                        modifier  = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        "SWEEPING LOCAL NETWORK...",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
            state.scanError?.let { err ->
                Spacer(Modifier.height(6.dp))
                Text(err, color = ErrorRed, fontSize = 11.sp)
            }
        }

        // ── Discovered devices
        if (state.discoveredDevices.isNotEmpty()) {
            SectionCard {
                SectionHeader(
                    title   = "DISCOVERED_DEVICES",
                    trailing = {
                        Text(
                            "${state.discoveredDevices.size}",
                            color = OrangePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
                state.discoveredDevices.forEach { device ->
                    DiscoveredDeviceRow(
                        device   = device,
                        selected = state.selectedDevice == device,
                        onClick  = { vm.selectDiscoveredDevice(device) },
                    )
                    HorizontalDivider(color = SurfaceStroke, thickness = 0.5.dp)
                }
            }
        }

        // ── Manual entry
        SectionCard {
            SectionHeader(title = "MANUAL_ENTRY")
            Spacer(Modifier.height(12.dp))
            TacticalTextField(
                value       = state.manualHost,
                onValueChange = vm::onManualHostChanged,
                label       = "HOST / IP ADDRESS",
                placeholder = "192.168.1.100",
                modifier    = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            TacticalTextField(
                value       = state.manualPort,
                onValueChange = vm::onManualPortChanged,
                label       = "PORT",
                placeholder = "554",
                keyboardType = KeyboardType.Number,
                modifier    = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            GhostButton(
                text    = "PROBE_HOST",
                onClick = vm::probeManual,
                icon    = Icons.Filled.Search,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── Proceed
        val canProceed = state.manualHost.isNotBlank() || state.selectedDevice != null
        PrimaryButton(
            text    = "NEXT — AUTH_CONFIG",
            onClick = vm::goToAuth,
            enabled = canProceed,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DiscoveredDeviceRow(
    device: DiscoveredDevice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) OrangePrimary.copy(alpha = 0.08f) else Color.Transparent)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Router,
            contentDescription = null,
            tint     = if (selected) OrangePrimary else TextDisabled,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = device.name.ifBlank { device.host },
                color      = if (selected) TextPrimary else TextSecondary,
                fontSize   = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                text     = "${device.host}:${device.port}",
                color    = TextDisabled,
                fontSize = 11.sp,
            )
        }
        SourceTypeBadge(device.suggestedProtocol.label)
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Check, contentDescription = null, tint = GreenOnline, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Step 2: Auth ──────────────────────────────────────────────────────────────

@Composable
private fun AuthStep(state: SetupUiState, vm: SetupViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Device info
        SectionCard {
            SectionHeader(title = "DEVICE_INFO")
            Spacer(Modifier.height(12.dp))
            TacticalTextField(
                value         = state.deviceName,
                onValueChange = vm::onDeviceNameChanged,
                label         = "DEVICE NAME",
                placeholder   = "Front Door Camera",
                modifier      = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            TacticalTextField(
                value         = state.location,
                onValueChange = vm::onLocationChanged,
                label         = "LOCATION / ZONE",
                placeholder   = "Entry",
                modifier      = Modifier.fillMaxWidth(),
            )
        }

        // ── Protocol + stream path
        SectionCard {
            SectionHeader(title = "STREAM_CONFIG")
            Spacer(Modifier.height(12.dp))
            Text("PROTOCOL", color = TextDisabled, fontSize = 10.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            ProtocolSelector(selected = state.protocol, onSelect = vm::onProtocolChanged)
            Spacer(Modifier.height(12.dp))
            TacticalTextField(
                value         = state.streamPath,
                onValueChange = vm::onPathChanged,
                label         = "STREAM PATH",
                placeholder   = "/stream",
                modifier      = Modifier.fillMaxWidth(),
            )
        }

        // ── Credentials
        SectionCard {
            SectionHeader(title = "CREDENTIALS")
            Spacer(Modifier.height(8.dp))
            Text("AUTH TYPE", color = TextDisabled, fontSize = 10.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            AuthTypeSelector(selected = state.authType, onSelect = vm::onAuthTypeChanged)
            if (state.authType != AuthType.NONE) {
                Spacer(Modifier.height(12.dp))
                TacticalTextField(
                    value         = state.username,
                    onValueChange = vm::onUsernameChanged,
                    label         = "USERNAME",
                    placeholder   = "admin",
                    modifier      = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                TacticalTextField(
                    value         = state.password,
                    onValueChange = vm::onPasswordChanged,
                    label         = "PASSWORD",
                    placeholder   = "••••••",
                    isPassword    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── Test connection
        SectionCard {
            SectionHeader(title = "CONNECTION_TEST")
            Spacer(Modifier.height(10.dp))
            GhostButton(
                text    = if (state.isTestingConnection) "TESTING..." else "TEST_CONNECTION",
                onClick = vm::testConnection,
                modifier = Modifier.fillMaxWidth(),
            )
            state.connectionTestResult?.let { result ->
                Spacer(Modifier.height(8.dp))
                val statusColor = when {
                    state.connectionTestOk           -> GreenOnline
                    state.connectionTestUnsupported  -> StatusConnecting   // amber: not pass, not fail
                    state.isTestingConnection        -> CyanTertiaryDim
                    else                             -> ErrorRed
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PulseDot(
                        color   = statusColor,
                        size    = 7.dp,
                        animate = state.connectionTestOk || state.isTestingConnection,
                    )
                    Text(
                        text     = state.connectionTestPhase?.let { "$it · $result" } ?: result,
                        color    = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    )
                }
                state.connectionTestDetail?.let { detail ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = "// $detail",
                        color    = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }

        PrimaryButton(
            text     = "NEXT — CONFIRM_SAVE",
            onClick  = vm::goToConfirm,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ProtocolSelector(selected: StreamProtocol, onSelect: (StreamProtocol) -> Unit) {
    val protocols = StreamProtocol.entries
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        protocols.take(4).forEach { proto ->
            ProtocolChip(
                label    = proto.label,
                selected = selected == proto,
                onClick  = { onSelect(proto) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        protocols.drop(4).forEach { proto ->
            ProtocolChip(
                label    = proto.label,
                selected = selected == proto,
                onClick  = { onSelect(proto) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProtocolChip(
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
            .border(
                1.dp,
                if (selected) OrangePrimary else SurfaceStroke,
                RoundedCornerShape(6.dp),
            )
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
private fun AuthTypeSelector(selected: AuthType, onSelect: (AuthType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AuthType.entries.forEach { type ->
            ProtocolChip(
                label    = type.name,
                selected = selected == type,
                onClick  = { onSelect(type) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Step 3: Confirm ───────────────────────────────────────────────────────────

@Composable
private fun ConfirmStep(state: SetupUiState, vm: SetupViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard {
            SectionHeader(title = "DEVICE_PROFILE")
            Spacer(Modifier.height(12.dp))
            MetricRow("NAME",     state.deviceName.ifBlank { state.manualHost })
            MetricRow("LOCATION", state.location.ifBlank { "Unassigned" })
            MetricRow("HOST",     state.manualHost)
            MetricRow("PORT",     state.manualPort)
            MetricRow("PROTOCOL", state.protocol.label)
            MetricRow("PATH",     state.streamPath.ifBlank { "/" })
        }

        SectionCard {
            SectionHeader(title = "AUTH_CONFIG")
            Spacer(Modifier.height(12.dp))
            MetricRow("AUTH_TYPE", state.authType.name)
            if (state.authType != AuthType.NONE) {
                MetricRow("USERNAME", state.username.ifBlank { "—" })
                MetricRow("PASSWORD", if (state.password.isNotBlank()) "••••••" else "—")
            }
        }

        SectionCard {
            SectionHeader(title = "STREAM_URL_PREVIEW")
            Spacer(Modifier.height(8.dp))
            val preview = buildStreamPreview(state)
            Text(
                text     = preview,
                color    = CyanTertiaryDim,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
            )
        }

        state.saveError?.let { err ->
            Text(
                text     = "ERROR: $err",
                color    = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ErrorRed.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            )
        }

        PrimaryButton(
            text    = if (state.isSaving) "SAVING..." else "SAVE_DEVICE",
            onClick = { vm.saveDevice {} },
            enabled = !state.isSaving,
            icon    = Icons.Filled.Check,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(32.dp))
    }
}

private fun buildStreamPreview(state: SetupUiState): String {
    val scheme = when (state.protocol) {
        StreamProtocol.RTSP, StreamProtocol.ONVIF -> "rtsp"
        StreamProtocol.HLS -> "http"
        else -> "http"
    }
    val auth = if (state.username.isNotBlank()) "${state.username}:****@" else ""
    val port = state.manualPort.ifBlank { state.protocol.defaultPort.toString() }
    val path = state.streamPath.ifBlank { "/" }.let { if (it.startsWith("/")) it else "/$it" }
    return "$scheme://$auth${state.manualHost}:$port$path"
}

// ── Shared: tactical text field ───────────────────────────────────────────────

@Composable
private fun TacticalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = {
            Text(
                text       = label,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        },
        placeholder   = {
            Text(placeholder, color = TextDisabled, fontSize = 13.sp)
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
