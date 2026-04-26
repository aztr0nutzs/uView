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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeomSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.app.R
import com.sentinel.app.domain.model.CameraSourceType
import com.sentinel.app.domain.model.StreamQualityProfile
import com.sentinel.app.domain.model.isSelectableInShipBuild
import com.sentinel.app.domain.model.supportInfo
import com.sentinel.app.ui.components.FastenerDots
import com.sentinel.app.ui.components.GhostButton
import com.sentinel.app.ui.components.PrimaryButton
import com.sentinel.app.ui.components.TacticalSectionHeader
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanPrimary
import com.sentinel.app.ui.theme.CyanSubtle
import com.sentinel.app.ui.theme.CyanTertiaryDim
import com.sentinel.app.ui.theme.GreenOnline
import com.sentinel.app.ui.theme.OrangePrimary
import com.sentinel.app.ui.theme.SentinelTheme
import com.sentinel.app.ui.theme.StatusOffline
import com.sentinel.app.ui.theme.StatusOnline
import com.sentinel.app.ui.theme.SurfaceBase
import com.sentinel.app.ui.theme.SurfaceElevated
import com.sentinel.app.ui.theme.SurfaceHighest
import com.sentinel.app.ui.theme.SurfaceLowest
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
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.saveSuccess) {
        onSaveComplete()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        // ── Tactical top bar — matches the SENTINEL_HUB family ────────────
        TacticalWizardTopBar(
            step = state.step,
            stepIndex = WizardStep.values().indexOf(state.step),
            stepCount = WizardStep.values().size,
            onBack = {
                if (state.step == WizardStep.SELECT_TYPE) onNavigateBack()
                else viewModel.prevStep()
            }
        )

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
// Tactical top bar — mirrors the SENTINEL_HUB header from the camera list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TacticalWizardTopBar(
    step: WizardStep,
    stepIndex: Int,
    stepCount: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLowest)
            .drawBehind {
                drawRect(
                    color = SurfaceLowest,
                    topLeft = Offset(0f, this.size.height - 6.dp.toPx()),
                    size = GeomSize(this.size.width, 6.dp.toPx())
                )
            }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bracketed back button
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SurfaceHighest)
                .border(2.dp, OrangePrimary.copy(alpha = 0.35f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = OrangePrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "SENTINEL_HUB // SOURCE_DEPLOY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = OrangePrimary,
                letterSpacing = 1.6.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                step.title.uppercase().replace(" ", "_"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = TextPrimary,
                letterSpacing = (-0.2).sp
            )
        }

        // Step counter chip
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "PHASE",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = CyanTertiaryDim,
                letterSpacing = 1.5.sp
            )
            Text(
                "%02d/%02d".format(stepIndex + 1, stepCount),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = TextPrimary
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — Source Type Grid (tactical HUD reconstruction)
// ─────────────────────────────────────────────────────────────────────────────

private data class SourceGroup(
    val title: String,
    val codename: String,
    val accent: Color,
    val types: List<CameraSourceType>
)

@Composable
private fun StepSelectType(state: AddCameraUiState, vm: AddCameraViewModel) {
    val sourceGroups = listOf(
        SourceGroup(
            title = "IP / SECURITY CAMERAS",
            codename = "PROTOCOL_CLASS_A",
            accent = OrangePrimary,
            types = listOf(
                CameraSourceType.RTSP,
                CameraSourceType.MJPEG,
                CameraSourceType.HLS
            )
        ),
        SourceGroup(
            title = "ANDROID PHONE AS CAMERA",
            codename = "PROTOCOL_CLASS_B",
            accent = CyanTertiaryDim,
            types = listOf(
                CameraSourceType.ANDROID_IPWEBCAM,
                CameraSourceType.ANDROID_DROIDCAM,
                CameraSourceType.ANDROID_CUSTOM
            )
        ),
        SourceGroup(
            title = "OTHER",
            codename = "PROTOCOL_CLASS_X",
            accent = GreenOnline,
            types = listOf(
                CameraSourceType.GENERIC_URL
            )
        )
    )

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // ── Mission strap line ────────────────────────────────────────────
        item {
            SelectFeedHeroPanel()
        }

        sourceGroups.forEachIndexed { gi, group ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TacticalSectionHeader(title = group.title, color = group.accent)
                        Text(
                            "[ ${group.codename} ]",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = group.accent.copy(alpha = 0.7f),
                            letterSpacing = 1.2.sp
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.types.chunked(2).forEachIndexed { ri, rowTypes ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowTypes.forEachIndexed { ci, type ->
                                    val slot = "%02d".format(gi * 10 + ri * 2 + ci + 1)
                                    TacticalSourceTile(
                                        type = type,
                                        slot = slot,
                                        accent = group.accent,
                                        selected = state.selectedSourceType == type,
                                        enabled = type.isSelectableInShipBuild,
                                        onClick = {
                                            vm.selectSourceType(type)
                                            vm.nextStep()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowTypes.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // ── Encrypted footer line — matches screen 1 ──────────────────────
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) {
                        Box(Modifier.size(6.dp).background(CyanTertiaryDim.copy(alpha = 0.4f)))
                    }
                }
                Text(
                    "// SELECT FEED PROTOCOL · AES-256-GCM ·",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanTertiaryDim.copy(alpha = 0.4f),
                    letterSpacing = 2.sp
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SelectFeedHeroPanel() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceBase)
            .drawBehind {
                drawRect(
                    color = OrangePrimary,
                    topLeft = Offset.Zero,
                    size = GeomSize(4.dp.toPx(), this.size.height)
                )
            }
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(6.dp).background(GreenOnline)
                )
                Text(
                    "DEPLOYMENT_OPS // NEW_NODE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = GreenOnline,
                    letterSpacing = 1.6.sp
                )
            }
            Text(
                "SELECT FEED PROTOCOL",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = TextPrimary,
                letterSpacing = (-0.3).sp
            )
            Text(
                "This build accepts direct RTSP, MJPEG/HTTP, HLS, IP Webcam, DroidCam, and custom direct URLs. ONVIF profile setup and Alfred cloud relay are unavailable.",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
        }
        FastenerDots()
    }
}

@Composable
private fun TacticalSourceTile(
    type: CameraSourceType,
    slot: String,
    accent: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val support = type.supportInfo
    val iconRes = when (type) {
        CameraSourceType.ANDROID_DROIDCAM,
        CameraSourceType.ANDROID_ALFRED,
        CameraSourceType.ANDROID_IPWEBCAM,
        CameraSourceType.ANDROID_CUSTOM -> R.drawable.ic_devices_lite
        CameraSourceType.ONVIF          -> R.drawable.ic_net_config
        CameraSourceType.RTSP,
        CameraSourceType.MJPEG,
        CameraSourceType.HLS            -> R.drawable.ic_live_view
        CameraSourceType.GENERIC_URL    -> R.drawable.ic_devices
        CameraSourceType.DEMO           -> R.drawable.ic_add_cam
    }

    val protocolTag = when (type) {
        CameraSourceType.RTSP             -> "RTSP/554"
        CameraSourceType.MJPEG            -> "MJPEG/HTTP"
        CameraSourceType.ONVIF            -> "ONVIF"
        CameraSourceType.HLS              -> "HLS/M3U8"
        CameraSourceType.ANDROID_DROIDCAM -> "DROIDCAM/4747"
        CameraSourceType.ANDROID_IPWEBCAM -> "IP_WEBCAM/8080"
        CameraSourceType.ANDROID_ALFRED   -> "ALFRED/CLOUD"
        CameraSourceType.ANDROID_CUSTOM   -> "PHONE/CUSTOM"
        CameraSourceType.GENERIC_URL      -> "URL/RAW"
        CameraSourceType.DEMO             -> "LOCAL/DEMO"
    }

    val borderColor = when {
        !enabled -> TextDisabled.copy(alpha = 0.45f)
        selected -> accent
        else -> SurfaceStroke
    }
    val tintColor = when {
        !enabled -> TextDisabled
        selected -> accent
        else -> TextSecondary
    }
    val nameColor = when {
        !enabled -> TextDisabled
        selected -> accent
        else -> TextPrimary
    }

    Box(
        modifier = modifier
            .background(if (selected) SurfaceElevated else SurfaceBase)
            .border(if (selected) 2.dp else 1.dp, borderColor)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Slot badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "[ $slot ]",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    color = accent.copy(alpha = 0.85f),
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .background((if (enabled) accent else TextDisabled).copy(alpha = if (selected) 0.20f else 0.10f))
                        .border(1.dp, (if (enabled) accent else TextDisabled).copy(alpha = 0.5f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        if (enabled) protocolTag else support.badge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = if (enabled) accent else TextDisabled,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            // Chamfer-style icon block
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(SurfaceLowest)
                    .border(1.5.dp, accent.copy(alpha = if (selected) 0.7f else 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Name + description
            Text(
                type.displayName.uppercase().replace(" ", "_"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                color = nameColor,
                letterSpacing = 0.3.sp,
                maxLines = 2
            )
            Text(
                support.detail,
                fontSize = 10.sp,
                color = TextSecondary,
                lineHeight = 13.sp,
                maxLines = 3
            )

            // Bottom signal stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (selected) 3.dp else 1.dp)
                    .background((if (enabled) accent else TextDisabled).copy(alpha = if (selected) 0.9f else 0.3f))
            )
        }
        FastenerDots(color = (if (enabled) accent else TextDisabled).copy(alpha = if (selected) 0.6f else 0.25f))
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
                    Text("Running TCP check...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
                        if (ok) "TCP endpoint reachable" else "Could not reach TCP endpoint",
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
                    InfoBanner("This check opens TCP host:port only. It does not verify credentials or decode RTSP/MJPEG/HLS frames.")
                }
                state.testSkipped -> {
                    Icon(Icons.Default.Check, null, tint = TextDisabled, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Test skipped", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                }
                else -> {
                    Icon(Icons.Default.Refresh, null, tint = CyanPrimary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Ready for TCP host check", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Run TCP Check", onClick = vm::runConnectionTest, icon = Icons.Default.Refresh, modifier = Modifier.fillMaxWidth(0.8f))
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
                        if (it.success) "TCP reachable" else "TCP failed"
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
