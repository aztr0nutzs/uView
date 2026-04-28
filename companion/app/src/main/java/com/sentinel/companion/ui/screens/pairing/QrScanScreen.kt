package com.sentinel.companion.ui.screens.pairing

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.companion.data.pairing.QrAnalyzer
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.CyanAccent
import com.sentinel.companion.ui.theme.ErrorRed
import com.sentinel.companion.ui.theme.GreenOnline
import com.sentinel.companion.ui.theme.SurfaceLow
import com.sentinel.companion.ui.theme.TextPrimary
import com.sentinel.companion.ui.theme.TextSecondary
import java.util.concurrent.Executors

@Composable
fun QrScanScreen(
    onPaired: () -> Unit,
    onCancel: () -> Unit,
    viewModel: QrScanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
            .padding(16.dp),
    ) {
        Text(
            "PAIR_VIA_QR",
            color = GreenOnline,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            "Open Settings → Pair Companion on the Sentinel hub and point your camera at the QR code.",
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(SurfaceLow),
        ) {
            when {
                !hasCameraPermission -> CenterText("Camera permission required to scan.", TextPrimary)
                state is QrScanState.Pairing -> CenterLoading("Establishing secure link…")
                state is QrScanState.Paired -> CenterText("✓ Paired", GreenOnline)
                state is QrScanState.Failed -> CenterText((state as QrScanState.Failed).reason, ErrorRed)
                else -> CameraPreview(onDecoded = viewModel::onQrDecoded)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceLow),
                modifier = Modifier.weight(1f),
            ) { Text("Manual Entry", color = TextPrimary) }
            when (val s = state) {
                is QrScanState.Failed -> Button(onClick = viewModel::retry, modifier = Modifier.weight(1f)) { Text("Retry") }
                is QrScanState.Paired -> Button(onClick = onPaired, modifier = Modifier.weight(1f)) { Text("Continue") }
                else -> Spacer(Modifier.weight(1f))
            }
        }

        // Auto-advance on success after a brief moment so the user sees the confirmation.
        LaunchedEffect(state) {
            if (state is QrScanState.Paired) {
                kotlinx.coroutines.delay(800)
                onPaired()
            }
        }
    }
}

@Composable
private fun CenterText(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = color, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun CenterLoading(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = CyanAccent)
            Spacer(Modifier.height(12.dp))
            Text(label, color = TextSecondary)
        }
    }
}

@Composable
private fun CameraPreview(onDecoded: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    LaunchedEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, QrAnalyzer(onDecoded)) }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }
}
