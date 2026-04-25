package com.sentinel.companion.ui.screens.connect

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.companion.ui.components.GhostButton
import com.sentinel.companion.ui.components.PrimaryButton
import com.sentinel.companion.ui.components.TacticalLabel
import com.sentinel.companion.ui.components.carbonBackground
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.CyanTertiaryDim
import com.sentinel.companion.ui.theme.ErrorRed
import com.sentinel.companion.ui.theme.GreenOnline
import com.sentinel.companion.ui.theme.OrangeGlow
import com.sentinel.companion.ui.theme.OrangePrimary
import com.sentinel.companion.ui.theme.OrangeSubtle
import com.sentinel.companion.ui.theme.SurfaceBase
import com.sentinel.companion.ui.theme.SurfaceElevated
import com.sentinel.companion.ui.theme.SurfaceStroke
import com.sentinel.companion.ui.theme.TextDisabled
import com.sentinel.companion.ui.theme.TextPrimary
import com.sentinel.companion.ui.theme.TextSecondary

@Composable
fun ConnectScreen(
    onConnected: () -> Unit,
    viewModel: ConnectViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.3f,
        targetValue   = 0.7f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label         = "glowAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
            .carbonBackground(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Logo ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .drawBehind {
                        drawCircle(
                            color  = OrangePrimary.copy(alpha = glowAlpha * 0.4f),
                            radius = size.minDimension / 2 + 16.dp.toPx(),
                        )
                    }
                    .background(OrangeSubtle, RoundedCornerShape(24.dp))
                    .border(1.dp, OrangePrimary.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "SENTINEL_COMPANION",
                color = OrangePrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 2.sp,
            )
            Text(
                text = "v1.0 // SECURE_VIEWER",
                color = TextSecondary,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
            )

            Spacer(Modifier.height(40.dp))

            // ── Connection form ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceBase, RoundedCornerShape(16.dp))
                    .border(1.dp, SurfaceStroke, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TacticalLabel("NETWORK_CONFIG", size = 10f)

                    // Host address
                    OutlinedTextField(
                        value = state.hostAddress,
                        onValueChange = viewModel::onHostChanged,
                        label = { Text("HOST_ADDRESS", fontSize = 11.sp, letterSpacing = 0.5.sp) },
                        placeholder = { Text("192.168.1.100", color = TextDisabled, fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction    = ImeAction.Next,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = CyanTertiaryDim,
                            unfocusedBorderColor = SurfaceStroke,
                            focusedLabelColor    = CyanTertiaryDim,
                            unfocusedLabelColor  = TextSecondary,
                            cursorColor          = CyanTertiaryDim,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            focusedContainerColor  = SurfaceElevated,
                            unfocusedContainerColor= SurfaceElevated,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Port
                    OutlinedTextField(
                        value = state.port,
                        onValueChange = viewModel::onPortChanged,
                        label = { Text("PORT", fontSize = 11.sp, letterSpacing = 0.5.sp) },
                        placeholder = { Text("8080", color = TextDisabled, fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction    = ImeAction.Done,
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = CyanTertiaryDim,
                            unfocusedBorderColor = SurfaceStroke,
                            focusedLabelColor    = CyanTertiaryDim,
                            unfocusedLabelColor  = TextSecondary,
                            cursorColor          = CyanTertiaryDim,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            focusedContainerColor  = SurfaceElevated,
                            unfocusedContainerColor= SurfaceElevated,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // HTTPS toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("USE_HTTPS", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Encrypt connection (TLS)", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = state.useHttps,
                            onCheckedChange = viewModel::onHttpsToggled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OrangePrimary,
                                checkedTrackColor = OrangeSubtle,
                            ),
                        )
                    }
                }
            }

            // Error message
            if (state.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "// ${state.errorMessage}",
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Connect button
            if (state.isConnecting) {
                CircularProgressIndicator(color = OrangePrimary, modifier = Modifier.size(36.dp))
            } else {
                PrimaryButton(
                    text = "ESTABLISH_LINK",
                    onClick = { viewModel.connect(onConnected) },
                    icon = Icons.Filled.Wifi,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(16.dp))

            GhostButton(
                text = "CONTINUE_LOCAL",
                onClick = { viewModel.continueLocal(onConnected) },
                color = CyanTertiaryDim,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "SENTINEL_COMPANION pairs with a running Sentinel Home instance on your network.",
                color = TextDisabled,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}
