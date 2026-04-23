package com.sentinel.app.data.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.sentinel.app.domain.model.CameraSourceType
import com.sentinel.app.domain.model.CameraStreamEndpoint
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExoPlayerFactory
 *
 * Creates and configures [ExoPlayer] instances for the given stream endpoint.
 * Source type routing:
 *
 *   RTSP / RTSPS → [RtspMediaSource]       (Media3 native, handles TCP/UDP transport)
 *   HLS          → [HlsMediaSource]        (Media3 native)
 *   HTTP/HTTPS   → [ProgressiveMediaSource] (handles generic HTTP streams, not MJPEG)
 *
 * MJPEG (multipart/x-mixed-replace) is NOT routed through ExoPlayer — it uses
 * [MjpegFrameSource] and [MjpegStreamView] instead.
 *
 * Each [ExoPlayer] returned here is NOT yet prepared — callers must call
 * [ExoPlayer.prepare] after attaching a [PlayerView].
 */
@UnstableApi
@Singleton
class ExoPlayerFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Returns a fully configured [ExoPlayer] ready for [ExoPlayer.prepare].
     * Returns null if the endpoint type requires MJPEG rendering instead.
     */
    fun create(endpoint: CameraStreamEndpoint): ExoPlayer? {
        if (endpoint.sourceType == CameraSourceType.MJPEG) {
            Timber.d("ExoPlayerFactory: MJPEG endpoint — use MjpegFrameSource instead")
            return null
        }

        Timber.d("ExoPlayerFactory: creating player for ${endpoint.url}")

        val player = ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(0)
            .setSeekForwardIncrementMs(0)
            .build()

        val mediaSource = buildMediaSource(endpoint)
        player.setMediaSource(mediaSource)
        player.playWhenReady = true

        return player
    }

    private fun buildMediaSource(endpoint: CameraStreamEndpoint): MediaSource {
        val mediaItem = MediaItem.fromUri(endpoint.url)

        return when {
            endpoint.url.startsWith("rtsp://", ignoreCase = true) ||
            endpoint.url.startsWith("rtsps://", ignoreCase = true) -> {
                RtspMediaSource.Factory()
                    .setForceUseRtpTcp(true)   // TCP is more reliable through NAT/firewalls
                    .createMediaSource(mediaItem)
            }

            endpoint.sourceType == CameraSourceType.HLS ||
            endpoint.url.contains(".m3u8", ignoreCase = true) -> {
                val httpFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("SentinelHome/1.0")
                    .setConnectTimeoutMs(8_000)
                    .setReadTimeoutMs(15_000)
                val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }

            else -> {
                // Generic HTTP progressive source (handles raw JPEG snapshots,
                // some vendor-specific formats, and unknown HTTP sources)
                val httpFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent("SentinelHome/1.0")
                    .setConnectTimeoutMs(8_000)
                    .setReadTimeoutMs(15_000)
                val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }
}
