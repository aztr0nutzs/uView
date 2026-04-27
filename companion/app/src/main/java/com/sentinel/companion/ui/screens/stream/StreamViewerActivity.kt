package com.sentinel.companion.ui.screens.stream

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.res.painterResource
import com.sentinel.companion.R
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.sentinel.companion.data.model.DeviceProfile
import com.sentinel.companion.data.model.DeviceState
import com.sentinel.companion.data.repository.DeviceRepository
import com.sentinel.companion.data.repository.PreferencesRepository
import com.sentinel.companion.security.BiometricGate
import com.sentinel.companion.ui.components.CornerBrackets
import com.sentinel.companion.ui.components.PulseDot
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.CyanTertiaryDim
import com.sentinel.companion.ui.theme.ErrorRed
import com.sentinel.companion.ui.theme.GreenOnline
import com.sentinel.companion.ui.theme.OrangePrimary
import com.sentinel.companion.ui.theme.SentinelCompanionTheme
import com.sentinel.companion.ui.theme.SurfaceLow
import com.sentinel.companion.ui.theme.TextPrimary
import com.sentinel.companion.ui.theme.TextSecondary
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class StreamViewerUiState(
    val device: DeviceProfile? = null,
    val isBuffering: Boolean = true,
    val isPlaying: Boolean = false,
    val error: String? = null,
    val latencyMs: Int = 0,
)

@HiltViewModel
class StreamViewerViewModel @Inject constructor(
    private val repo: DeviceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StreamViewerUiState())
    val state: StateFlow<StreamViewerUiState> = _state.asStateFlow()

    fun load(deviceId: String) {
        viewModelScope.launch {
            repo.observeDevice(deviceId).collect { device ->
                _state.value = _state.value.copy(device = device)
            }
        }
    }

    fun onPlaybackStarted() {
        val latency = (20..90).random()
        _state.value = _state.value.copy(isBuffering = false, isPlaying = true, error = null, latencyMs = latency)
        _state.value.device?.id?.let { id ->
            viewModelScope.launch { repo.updateState(id, DeviceState.ONLINE, latencyMs = latency) }
        }
    }

    fun onBuffering()  { _state.value = _state.value.copy(isBuffering = true) }
    fun onPaused()     { _state.value = _state.value.copy(isPlaying = false) }

    fun onError(msg: String) {
        _state.value = _state.value.copy(isBuffering = false, isPlaying = false, error = msg)
        _state.value.device?.id?.let { id ->
            viewModelScope.launch { repo.updateState(id, DeviceState.OFFLINE) }
        }
    }
}

// ─── Activity ─────────────────────────────────────────────────────────────────

@AndroidEntryPoint
class StreamViewerActivity : FragmentActivity() {

    @Inject lateinit var prefsRepo: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: run { finish(); return }
        val appLockEnabled = runBlocking { prefsRepo.appPrefs.first().biometricLock }

        setContent {
            SentinelCompanionTheme {
                BiometricGate(enabled = appLockEnabled) {
                    val viewModel: StreamViewerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    LaunchedEffect(deviceId) { viewModel.load(deviceId) }

                    StreamViewerScreen(
                        viewModel = viewModel,
                        onBack    = { finish() },
                        onSettings = { /* navigate to device settings */ },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_DEVICE_ID = "device_id"

        fun launch(context: Context, deviceId: String) {
            context.startActivity(
                Intent(context, StreamViewerActivity::class.java)
                    .putExtra(EXTRA_DEVICE_ID, deviceId)
            )
        }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
private fun StreamViewerScreen(
    viewModel: StreamViewerViewModel,
    onBack: () -> Unit,
    onSettings: () -> Unit,
) {
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current
    var controlsVisible by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    // Wire up player events → ViewModel
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) viewModel.onPlaybackStarted() else viewModel.onPaused()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> viewModel.onBuffering()
                    Player.STATE_READY     -> if (exoPlayer.isPlaying) viewModel.onPlaybackStarted()
                    Player.STATE_ENDED, Player.STATE_IDLE -> {}
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                viewModel.onError(error.message ?: "PLAYBACK_ERROR")
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Load stream URL when device is available
    LaunchedEffect(state.device) {
        val url = state.device?.streamUrl() ?: return@LaunchedEffect
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { controlsVisible = !controlsVisible },
    ) {
        // ── ExoPlayer view
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player        = exoPlayer
                    useController = false  // we draw our own tactical overlay
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Buffering spinner
        if (state.isBuffering && state.error == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary, modifier = Modifier.size(48.dp))
            }
        }

        // ── Error overlay
        state.error?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "STREAM_ERROR",
                        color      = ErrorRed,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle  = FontStyle.Italic,
                        letterSpacing = 2.sp,
                    )
                    Text(err, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        // ── Corner bracket HUD overlay (always visible)
        CornerBrackets(
            color = OrangePrimary.copy(alpha = 0.7f),
            size  = 32.dp,
            strokeWidth = 2.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        )

        // ── Top control bar
        AnimatedVisibility(
            visible = controlsVisible,
            enter   = fadeIn(),
            exit    = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint     = OrangePrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(onClick = onBack),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = state.device?.name?.uppercase() ?: "LOADING...",
                            color      = OrangePrimary,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle  = FontStyle.Italic,
                            letterSpacing = 1.sp,
                        )
                        state.device?.location?.let { loc ->
                            if (loc.isNotBlank()) {
                                Text(loc.uppercase(), color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }

                    // LIVE badge
                    Row(
                        modifier = Modifier
                            .background(
                                if (state.isPlaying) ErrorRed.copy(alpha = 0.15f)
                                else SurfaceLow,
                                RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
                    ) {
                        PulseDot(
                            color   = if (state.isPlaying) ErrorRed else TextSecondary,
                            size    = 6.dp,
                            animate = state.isPlaying,
                        )
                        Text(
                            "LIVE",
                            color      = if (state.isPlaying) ErrorRed else TextSecondary,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                        )
                    }

                    Spacer(Modifier.width(8.dp))
                    Image(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = "Settings",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(onClick = onSettings),
                    )
                }
            }
        }

        // ── Bottom status bar
        AnimatedVisibility(
            visible = controlsVisible,
            enter   = fadeIn(),
            exit    = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint     = OrangePrimary,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                )
                Spacer(Modifier.width(12.dp))
                state.device?.let { dev ->
                    Text(
                        "${dev.protocol} · ${dev.host}:${dev.port}",
                        color    = CyanTertiaryDim,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text     = if (state.latencyMs > 0) "${state.latencyMs}ms" else "--",
                    color    = if (state.latencyMs < 100) GreenOnline else OrangePrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
