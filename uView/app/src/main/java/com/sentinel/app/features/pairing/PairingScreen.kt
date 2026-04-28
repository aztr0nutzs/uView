package com.sentinel.app.features.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.app.core.pairing.PairingHost
import com.sentinel.app.core.pairing.QrCodeRenderer
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanAccent
import com.sentinel.app.ui.theme.GreenOnline
import com.sentinel.app.ui.theme.SurfaceLow
import com.sentinel.app.ui.theme.TextDisabled
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary

@Composable
fun PairingScreen(
    onNavigateBack: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start() }
    DisposableEffect(Unit) { onDispose { viewModel.stop() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            Text(
                "Pair Companion",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val s = state) {
                is PairingHost.State.Idle -> Loading("Starting pairing window…")
                is PairingHost.State.Listening -> ListeningBody(s, onCancel = {
                    viewModel.stop(); onNavigateBack()
                })
                is PairingHost.State.Paired -> PairedBody(s, onClose = onNavigateBack)
                is PairingHost.State.Failed -> FailedBody(s.reason, onRetry = { viewModel.start() }, onClose = onNavigateBack)
            }
        }
    }
}

@Composable
private fun Loading(label: String) {
    Spacer(Modifier.height(64.dp))
    CircularProgressIndicator(color = CyanAccent)
    Spacer(Modifier.height(16.dp))
    Text(label, color = TextSecondary)
}

@Composable
private fun ListeningBody(s: PairingHost.State.Listening, onCancel: () -> Unit) {
    val density = LocalDensity.current
    val sizePx = with(density) { 280.dp.toPx().toInt() }
    val image = remember(s.session.qrPayload) {
        QrCodeRenderer.render(s.session.qrPayload, sizePx)
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "Open the Sentinel Companion app and scan this code to pair.",
        color = TextSecondary,
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(20.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(SurfaceLow, RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = BitmapPainter(image, filterQuality = FilterQuality.None),
            contentDescription = "Pairing QR code",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
        )
    }
    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("HOST", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
            Text("${s.session.host}:${s.session.port}", color = TextPrimary, fontWeight = FontWeight.Medium)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("EXPIRES IN", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
            Text("${(s.remainingMs / 1000).coerceAtLeast(0)}s", color = CyanAccent, fontWeight = FontWeight.Medium)
        }
    }
    Spacer(Modifier.height(16.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceLow, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Column {
            Text("FALLBACK CODE", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
            Text(
                s.session.fallbackCode,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Use this if the camera can't read the QR.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDisabled,
            )
        }
    }
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = onCancel,
        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Cancel pairing", color = BackgroundDeep, fontWeight = FontWeight.Bold) }
}

@Composable
private fun PairedBody(s: PairingHost.State.Paired, onClose: () -> Unit) {
    Spacer(Modifier.height(64.dp))
    Text("✓ Paired", color = GreenOnline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("Companion completed handshake at ${s.deviceLabel}", color = TextSecondary)
    Spacer(Modifier.height(24.dp))
    Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}

@Composable
private fun FailedBody(reason: String, onRetry: () -> Unit, onClose: () -> Unit) {
    Spacer(Modifier.height(64.dp))
    Text("Pairing failed", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(reason, color = TextSecondary)
    Spacer(Modifier.height(24.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onClose, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceLow)) { Text("Close", color = TextPrimary) }
        Button(onClick = onRetry, modifier = Modifier.weight(1f)) { Text("Retry") }
    }
}
