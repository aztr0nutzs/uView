package com.sentinel.app.core.di

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.sentinel.app.data.playback.CameraPlaybackServiceImpl
import com.sentinel.app.domain.service.CameraPlaybackService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * PlaybackModule
 *
 * Hilt module for Phase 3 playback components.
 *
 * Singletons provided here:
 *   - [CameraPlaybackService] → [CameraPlaybackServiceImpl]
 *     (ExoPlayerFactory, MjpegSessionRegistry, StreamUrlResolver are all
 *      @Singleton and injected directly — no manual provides needed)
 *
 * PlaybackManager is @Singleton and @Inject constructor, so Hilt creates it
 * automatically without an explicit @Provides.
 */
@Module
@InstallIn(SingletonComponent::class)
@OptIn(UnstableApi::class)
abstract class PlaybackModule {

    @Binds
    @Singleton
    abstract fun bindCameraPlaybackService(
        impl: CameraPlaybackServiceImpl
    ): CameraPlaybackService
}
// Note: SnapshotControllerImpl binding added to AppModules.kt separately
// since it requires @Provides not @Binds (context-dependent construction).
