package com.sentinel.app.features.addcamera

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.app.domain.model.CameraSourceType
import com.sentinel.app.domain.model.StreamQualityProfile
import com.sentinel.app.ui.components.GhostButton
import com.sentinel.app.ui.components.PrimaryButton
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanPrimary
import com.sentinel.app.ui.theme.CyanSubtle
import com.sentinel.app.ui.theme.SentinelTheme
import com.sentinel.app.ui.theme.StatusOffline
import com.sentinel.app.ui.theme.StatusOnline
import com.sentinel.app.ui.theme.SurfaceElevated
import com.sentinel.app.ui.theme.SurfaceHighest
import com.sentinel.app.ui.theme.SurfaceStroke
import com.sentinel.app.ui.theme.TextDisabled
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary

@Composable
fun AddCameraScreen(
    onNavigateBack: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: AddCameraViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.saveSuccess) {
        onSaveComplete()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (state.step == WizardStep.SELECT_TYPE) onNavigateBack()
                else viewModel.prevStep()
            }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.step.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Step ${WizardStep.values().indexOf(state.step) + 1} of ${WizardStep.values().size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // ── Step progress bar — a slim luminous stripe ────────────────────
        LinearProgressIndicator(
            progress = { state.stepProgress },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = CyanPrimary,
            trackColor = SurfaceHighest
        )
        Spacer(Modifier.height(8.dp))

        // ── Step content ──────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when (state.step) {
                WizardStep.SELECT_TYPE   -> StepSelectType(state, viewModel)
                WizardStep.NAME_AND_ROOM -> StepNameAndRoom(state, viewModel)
                WizardStep.NETWORK       -> StepNetwork(state, viewModel)
                WizardStep.CREDENTIALS   -> StepCredentials(state, viewModel)
                WizardStep.TEST          -> StepTestConnection(state, viewModel)
                WizardStep.CONFIRM       -> StepConfirm(state, viewModel)
            }
        }

        // ── Bottom navigation bar — always present ────────────────────────
        WizardBottomBar(
            step = state.step,
            canProceed = state.canProceed,
            isSaving = state.isSaving,
            onNext = viewModel::nextStep,
            onSave = viewModel::saveCamera
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — Source Type Grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepSelectType(state: AddCameraUiState, vm: AddCameraViewModel) {
    val sourceGroups = listOf(
        "IP / Security Cameras" to listOf(
            CameraSourceType.RTSP,
            CameraSourceType.MJPEG,
            CameraSourceType.ONVIF,
            CameraSourceType.HLS
        ),
        "Android Phone as Camera" to listOf(
            CameraSourceType.ANDROID_IPWEBCAM,
            CameraSourceType.ANDROID_DROIDCAM,
            CameraSourceType.ANDROID_ALFRED,
            CameraSourceType.ANDROID_CUSTOM
        ),
        "Other" to listOf(
            CameraSourceType.GENERIC_URL,
            CameraSourceType.DEMO
        )
    )

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        sourceGroups.forEach { (groupName, types) ->
            item {
                Text(
                    text = groupName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp),
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.chunked(2).forEach { rowTypes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowTypes.forEach { type ->
                                SourceTypeCard(
                                    type = type,
                                    selected = state.selectedSourceType == type,
                                    onClick = { vm.selectSourceType(type); vm.nextStep() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowTypes.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SourceTypeCard(
    type: CameraSourceType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        CameraSourceType.ANDROID_DROIDCAM,
        CameraSourceType.ANDROID_ALFRED,
        CameraSourceType.ANDROID_IPWEBCAM,
        CameraSourceType.ANDROID_CUSTOM -> Icons.Default.PhoneAndroid
        CameraSourceType.ONVIF          -> Icons.Default.Router
        else                            -> Icons.Default.Videocam
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) CyanSubtle else SurfaceElevated)
            .border(
                1.5.dp,
                if (selected) CyanPrimary else SurfaceStroke,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null,
            tint = if (selected) CyanPrimary else TextSecondary, modifier = Modifier.size(24.dp))
        Text(type.displayName, style = MaterialTheme.typography.labelMedium,
            color = if (selected) CyanPrimary else TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(type.description, style = MaterialTheme.typography.labelSmall,
            color = TextSecondary, maxLines = 2)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2 — Name & Room
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepNameAndRoom(state: AddCameraUiState, vm: AddCameraViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            WizardTextField(
                label = "Camera Name",
                value = state.cameraName,
                onValueChange = vm::setName,
                placeholder = "e.g. Front Door, Back Yard"
            )
        }
        item {
            WizardTextField(
                label = "Room / Zone",
                value = state.cameraRoom,
                onValueChange = vm::setRoom,
                placeholder = "e.g. Exterior, Living Room"
            )
        }
        if (state.existingRooms.isNotEmpty()) {
            item {
                Text("Existing Rooms", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    state.existingRooms.take(5).forEach { room ->
                        RoomSuggestionChip(room = room, onClick = { vm.setRoom(room) })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun RoomSuggestionChip(room: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceHighest)
            .border(1.dp, SurfaceStroke, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(room, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3 — Network Details
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepNetwork(state: AddCameraUiState, vm: AddCameraViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (state.isPhoneSource) {
            item {
                InfoBanner(
                    text = when (state.selectedSourceType) {
                        CameraSourceType.ANDROID_DROIDCAM  -> "DroidCam streams on port 4747 by default. Enter the phone's local IP address."
                        CameraSourceType.ANDROID_IPWEBCAM  -> "IP Webcam streams on port 8080. Enter the phone's local IP."
                        CameraSourceType.ANDROID_ALFRED    -> "Alfred does not expose a direct LAN stream. Remote viewing through Alfred's own app only. Manual URL entry required."
                        else -> "Enter the stream URL or IP address provided by your phone camera app."
                    }
                )
            }
            item { WizardTextField("Phone Nickname", state.phoneNickname, vm::setPhoneNickname, "e.g. Old Pixel 3a") }
            item { WizardTextField("Stream URL or IP Address", state.phoneEndpointUrl, vm::setPhoneEndpoint, "e.g. 192.168.1.115") }
        } else {
            item { WizardTextField("Host / IP Address", state.host, vm::setHost, "e.g. 192.168.1.101") }
            item { WizardTextField("Port", state.port, vm::setPort, "e.g. 554", keyboardType = KeyboardType.Number) }
            item { WizardTextField("Stream Path (optional)", state.streamPath, vm::setStreamPath, "e.g. /stream1  /live") }
        }

        item {
            Text("Stream Quality", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamQualityProfile.values().take(3).forEach { q ->
                    QualityChip(
                        label = q.label,
                        selected = state.streamQuality == q,
                        onClick = { vm.setQuality(q) }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun QualityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) CyanSubtle else SurfaceElevated)
            .border(1.dp, if (selected) CyanPrimary.copy(alpha = 0.5f) else SurfaceStroke, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) CyanPrimary else TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 4 — Credentials
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepCredentials(state: AddCameraUiState, vm: AddCameraViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            InfoBanner("Credentials are stored locally on your device. Leave blank if your camera does not require authentication.")
        }
        item { WizardTextField("Username (optional)", state.username, vm::setUsername, "e.g. admin") }
        item {
            WizardTextField(
                label = "Password (optional)",
                value = state.password,
                onValueChange = vm::setPassword,
                placeholder = "••••••••",
                isPassword = true
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 5 — Test Connection
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepTestConnection(state: AddCameraUiState, vm: AddCameraViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        item { Spacer(Modifier.height(16.dp)) }
        item {
            when {
                state.isTesting -> {
                    CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Testing connection…", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                state.testResult != null -> {
                    val ok = state.testResult.success
                    Icon(
                        if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                        null,
                        tint = if (ok) StatusOnline else StatusOffline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (ok) "Host reachable" else "Could not reach host",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (ok) StatusOnline else StatusOffline,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    state.testResult.errorMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    state.testResult.latencyMs?.let {
                        Text("Latency: ${it}ms", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Spacer(Modifier.height(8.dp))
                    InfoBanner("Stream-level probe requires ExoPlayer — only TCP reachability is tested here.")
                }
                state.testSkipped -> {
                    Icon(Icons.Default.Check, null, tint = TextDisabled, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Test skipped", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                }
                else -> {
                    Icon(Icons.Default.Refresh, null, tint = CyanPrimary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Ready to test", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Test Connection", onClick = vm::runConnectionTest, icon = Icons.Default.Refresh, modifier = Modifier.fillMaxWidth(0.8f))
                    Spacer(Modifier.height(10.dp))
                    GhostButton("Skip Test", onClick = { vm.skipTest(); vm.nextStep() }, modifier = Modifier.fillMaxWidth(0.8f))
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 6 — Confirm and Save
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepConfirm(state: AddCameraUiState, vm: AddCameraViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            com.sentinel.app.ui.components.SectionCard(title = "Summary") {
                com.sentinel.app.ui.components.InfoRow("Name", state.cameraName)
                Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(SurfaceStroke))
                com.sentinel.app.ui.components.InfoRow("Room", state.cameraRoom)
                Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(SurfaceStroke))
                com.sentinel.app.ui.components.InfoRow("Source", state.selectedSourceType?.displayName ?: "—")
                Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(SurfaceStroke))
                com.sentinel.app.ui.components.InfoRow(
                    "Endpoint",
                    if (state.isPhoneSource) state.phoneEndpointUrl else "${state.host}:${state.port}"
                )
                Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(SurfaceStroke))
                com.sentinel.app.ui.components.InfoRow("Auth", if (state.username.isNotBlank()) "Configured" else "None")
                Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(SurfaceStroke))
                com.sentinel.app.ui.components.InfoRow("Quality", state.streamQuality.label)
                state.testResult?.let {
                    Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(SurfaceStroke))
                    com.sentinel.app.ui.components.InfoRow(
                        "Connection Test",
                        if (it.success) "✓ Passed" else "✗ Failed"
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared form helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WizardTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextDisabled) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = SurfaceStroke,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = CyanPrimary,
                focusedContainerColor = SurfaceElevated,
                unfocusedContainerColor = SurfaceElevated
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun InfoBanner(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyanSubtle)
            .border(1.dp, CyanPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = CyanPrimary.copy(alpha = 0.9f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wizard bottom navigation bar — persistent save/next stripe
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WizardBottomBar(
    step: WizardStep,
    canProceed: Boolean,
    isSaving: Boolean,
    onNext: () -> Unit,
    onSave: () -> Unit
) {
    val isLastStep = step == WizardStep.CONFIRM
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated)
            .padding(16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        if (isLastStep) {
            PrimaryButton(
                text = if (isSaving) "Saving…" else "Save Camera",
                onClick = onSave,
                enabled = !isSaving,
                icon = Icons.Default.Check,
                modifier = Modifier.fillMaxWidth()
            )
        } else if (step != WizardStep.SELECT_TYPE) {
            PrimaryButton(
                text = "Next",
                onClick = onNext,
                enabled = canProceed,
                icon = Icons.Default.ArrowForward,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060B10)
@Composable
private fun AddCameraPreview() {
    SentinelTheme {
        AddCameraScreen(onNavigateBack = {}, onSaveComplete = {})
    }
}
