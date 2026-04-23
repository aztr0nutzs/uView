package com.sentinel.app.features.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.ConnectionTestResult
import com.sentinel.app.ui.components.InfoRow
import com.sentinel.app.ui.components.PrimaryButton
import com.sentinel.app.ui.components.SectionCard
import com.sentinel.app.ui.components.StatusChip
import com.sentinel.app.ui.preview.SampleData
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanPrimary
import com.sentinel.app.ui.theme.SentinelTheme
import com.sentinel.app.ui.theme.StatusOffline
import com.sentinel.app.ui.theme.StatusOnline
import com.sentinel.app.ui.theme.SurfaceElevated
import com.sentinel.app.ui.theme.SurfaceStroke
import com.sentinel.app.ui.theme.TextDisabled
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary
import com.sentinel.app.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DiagnosticsContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onTestCamera = viewModel::testCamera,
        onTestAll = viewModel::testAll,
        onClearResults = viewModel::clearResults
    )
}

@Composable
private fun DiagnosticsContent(
    state: DiagnosticsUiState,
    onNavigateBack: () -> Unit,
    onTestCamera: (CameraDevice) -> Unit,
    onTestAll: () -> Unit,
    onClearResults: () -> Unit
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
                .padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
            }
            Text(
                "Diagnostics",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (state.testResults.isNotEmpty()) {
                IconButton(onClick = onClearResults) {
                    Icon(Icons.Default.ClearAll, "Clear results", tint = TextSecondary)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Summary / Test All ────────────────────────────────────────
            item {
                SectionCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Connection Tests",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tests TCP reachability to each camera host. " +
                            "Stream-level probing (RTSP/MJPEG) requires ExoPlayer wiring — not yet implemented.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PrimaryButton(
                                text = if (state.isTestingAll) "Testing all…" else "Test All Cameras",
                                onClick = onTestAll,
                                enabled = !state.isTestingAll,
                                icon = Icons.Default.NetworkCheck,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (state.testResults.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            val passed = state.testResults.values.count { it.success }
                            val total  = state.testResults.size
                            Text(
                                "$passed / $total cameras reachable",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (passed == total) StatusOnline else WarningAmber
                            )
                        }
                    }
                }
            }

            // ── Per-camera diagnostic cards ───────────────────────────────
            items(state.cameras, key = { it.id }) { camera ->
                CameraDiagnosticCard(
                    camera = camera,
                    testResult = state.testResults[camera.id],
                    isTesting = camera.id in state.testingIds,
                    onTest = { onTestCamera(camera) }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraDiagnosticCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CameraDiagnosticCard(
    camera: CameraDevice,
    testResult: ConnectionTestResult?,
    isTesting: Boolean,
    onTest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceStroke, RoundedCornerShape(14.dp))
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    camera.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${camera.room} · ${camera.connectionProfile.host}:${camera.connectionProfile.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            StatusChip(status = camera.displayStatus)
            Spacer(Modifier.width(8.dp))
            // Test button / spinner
            if (isTesting) {
                CircularProgressIndicator(
                    color = CyanPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                IconButton(onClick = onTest, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        "Test connection",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Test result details
        if (testResult != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SurfaceStroke)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val ok = testResult.success
                Icon(
                    if (ok) Icons.Default.CheckCircle else Icons.Default.Error,
                    null,
                    tint = if (ok) StatusOnline else StatusOffline,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    if (ok) "Reachable" else "Unreachable",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (ok) StatusOnline else StatusOffline,
                    fontWeight = FontWeight.SemiBold
                )
                testResult.latencyMs?.let { ms ->
                    Text(
                        "· ${ms}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(testResult.testedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDisabled
                )
            }
            testResult.errorMessage?.let { err ->
                Text(
                    err,
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusOffline,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
                )
            }
            // Stream probe notice
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.HourglassTop, null, tint = TextDisabled, modifier = Modifier.size(12.dp))
                Text(
                    "Stream probe: not implemented — requires ExoPlayer",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextDisabled
                )
            }
        } else if (!isTesting) {
            Text(
                "Tap ↻ to test this camera",
                style = MaterialTheme.typography.labelSmall,
                color = TextDisabled,
                modifier = Modifier.padding(start = 14.dp, bottom = 10.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060B10)
@Composable
private fun DiagnosticsPreview() {
    SentinelTheme {
        DiagnosticsContent(
            state = DiagnosticsUiState(
                cameras = SampleData.allCameras,
                testResults = mapOf(
                    "cam_001" to SampleData.sampleConnectionTestResult,
                    "cam_003" to SampleData.sampleConnectionTestResult.copy(
                        cameraId = "cam_003", success = false,
                        errorMessage = "Connection refused"
                    )
                )
            ),
            onNavigateBack = {}, onTestCamera = {}, onTestAll = {}, onClearResults = {}
        )
    }
}
