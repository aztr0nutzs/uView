package com.sentinel.companion.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.sentinel.companion.R

object CompanionIcons {
    val Devices         @Composable get() = painterResource(R.drawable.devices)
    val DevicesLite     @Composable get() = painterResource(R.drawable.devices_lite)
    val AddCam          @Composable get() = painterResource(R.drawable.add_cam)
    val Snapshot        @Composable get() = painterResource(R.drawable.snapshot)
    val Alerts          @Composable get() = painterResource(R.drawable.alerts)
    val Record          @Composable get() = painterResource(R.drawable.record)
    val Playback        @Composable get() = painterResource(R.drawable.playback)
    val LiveView        @Composable get() = painterResource(R.drawable.live_view)
    val Settings        @Composable get() = painterResource(R.drawable.settings)
    val NetConfig       @Composable get() = painterResource(R.drawable.net_config)
    val DeleteCam       @Composable get() = painterResource(R.drawable.delete_cam)
    val ZoomInOut       @Composable get() = painterResource(R.drawable.zoom_in_out)
    val MicTalk         @Composable get() = painterResource(R.drawable.mic_talk)
    val Brand           @Composable get() = painterResource(R.drawable.uview_icon)
    val BrandAlt        @Composable get() = painterResource(R.drawable.uview_icon_2)
    val BrandDark       @Composable get() = painterResource(R.drawable.uview_icon_dark)
}

@Composable
fun CompanionIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = null,
    )
}
