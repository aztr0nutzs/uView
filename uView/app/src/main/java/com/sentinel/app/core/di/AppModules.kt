package com.sentinel.app.core.di

import android.content.Context
import androidx.room.Room
import com.sentinel.app.data.local.SentinelDatabase
import com.sentinel.app.data.local.dao.CameraDao
import com.sentinel.app.data.local.dao.CameraEventDao
import com.sentinel.app.data.preferences.AppPreferencesDataSource
import com.sentinel.app.data.remote.adapters.AndroidPhoneSourceAdapterImpl
import com.sentinel.app.data.remote.adapters.CameraConnectionTesterImpl
import com.sentinel.app.data.remote.adapters.GenericStreamAdapterImpl
import com.sentinel.app.data.remote.adapters.MjpegStreamAdapterImpl
import com.sentinel.app.data.remote.adapters.OnvifCameraAdapterImpl
import com.sentinel.app.data.remote.adapters.RtspCameraAdapterImpl
import com.sentinel.app.data.repository.CameraEventRepositoryImpl
import com.sentinel.app.data.repository.CameraRepositoryImpl
import com.sentinel.app.domain.repository.CameraEventRepository
import com.sentinel.app.domain.repository.CameraRepository
import com.sentinel.app.domain.service.AndroidPhoneSourceAdapter
import com.sentinel.app.domain.service.CameraConnectionTester
import com.sentinel.app.domain.service.CameraDiscoveryService
import com.sentinel.app.domain.service.GenericStreamAdapter
import com.sentinel.app.domain.service.MjpegStreamAdapter
import com.sentinel.app.domain.service.OnvifCameraAdapter
import com.sentinel.app.domain.service.RtspCameraAdapter
import com.sentinel.app.domain.service.SnapshotController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// DatabaseModule
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SentinelDatabase =
        Room.databaseBuilder(
            context,
            SentinelDatabase::class.java,
            "sentinel_db"
        )
            .fallbackToDestructiveMigration()   // Replace with real migrations before production
            .build()

    @Provides
    fun provideCameraDao(db: SentinelDatabase): CameraDao = db.cameraDao()

    @Provides
    fun provideCameraEventDao(db: SentinelDatabase): CameraEventDao = db.cameraEventDao()
}

// ─────────────────────────────────────────────────────────────────────────────
// PreferencesModule
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideAppPreferencesDataSource(
        @ApplicationContext context: Context
    ): AppPreferencesDataSource = AppPreferencesDataSource(context)
}

// ─────────────────────────────────────────────────────────────────────────────
// RepositoryModule
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCameraRepository(impl: CameraRepositoryImpl): CameraRepository

    @Binds
    @Singleton
    abstract fun bindCameraEventRepository(impl: CameraEventRepositoryImpl): CameraEventRepository
}

// ─────────────────────────────────────────────────────────────────────────────
// ServiceModule
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {


    @Binds
    @Singleton
    abstract fun bindConnectionTester(impl: CameraConnectionTesterImpl): CameraConnectionTester

    @Binds
    @Singleton
    abstract fun bindRtspAdapter(impl: RtspCameraAdapterImpl): RtspCameraAdapter

    @Binds
    @Singleton
    abstract fun bindMjpegAdapter(impl: MjpegStreamAdapterImpl): MjpegStreamAdapter

    @Binds
    @Singleton
    abstract fun bindOnvifAdapter(impl: OnvifCameraAdapterImpl): OnvifCameraAdapter

    @Binds
    @Singleton
    abstract fun bindAndroidPhoneAdapter(impl: AndroidPhoneSourceAdapterImpl): AndroidPhoneSourceAdapter

    @Binds
    @Singleton
    abstract fun bindGenericStreamAdapter(impl: GenericStreamAdapterImpl): GenericStreamAdapter
}

// ─────────────────────────────────────────────────────────────────────────────
// PlaybackModule — Phase 3 stream playback bindings
// ─────────────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    @Provides
    @Singleton
    fun provideStreamUrlResolver(
        rtsp: com.sentinel.app.data.remote.adapters.RtspCameraAdapterImpl,
        mjpeg: com.sentinel.app.data.remote.adapters.MjpegStreamAdapterImpl,
        onvif: com.sentinel.app.data.remote.adapters.OnvifCameraAdapterImpl,
        phone: com.sentinel.app.data.remote.adapters.AndroidPhoneSourceAdapterImpl,
        generic: com.sentinel.app.data.remote.adapters.GenericStreamAdapterImpl
    ): com.sentinel.app.data.playback.StreamUrlResolver =
        com.sentinel.app.data.playback.StreamUrlResolver(rtsp, mjpeg, onvif, phone, generic)

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Provides
    @Singleton
    fun provideExoPlayerFactory(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): com.sentinel.app.data.playback.ExoPlayerFactory =
        com.sentinel.app.data.playback.ExoPlayerFactory(context)

    @Provides
    @Singleton
    fun provideMjpegFrameSource(): com.sentinel.app.data.playback.mjpeg.MjpegFrameSource =
        com.sentinel.app.data.playback.mjpeg.MjpegFrameSource()

    @Provides
    @Singleton
    fun provideMjpegSessionRegistry(): com.sentinel.app.data.playback.MjpegSessionRegistry =
        com.sentinel.app.data.playback.MjpegSessionRegistry()

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Provides
    @Singleton
    fun provideCameraPlaybackService(
        urlResolver: com.sentinel.app.data.playback.StreamUrlResolver,
        playerFactory: com.sentinel.app.data.playback.ExoPlayerFactory,
        mjpegFrameSource: com.sentinel.app.data.playback.mjpeg.MjpegFrameSource,
        mjpegRegistry: com.sentinel.app.data.playback.MjpegSessionRegistry
    ): com.sentinel.app.data.playback.CameraPlaybackServiceImpl =
        com.sentinel.app.data.playback.CameraPlaybackServiceImpl(
            urlResolver, playerFactory, mjpegFrameSource, mjpegRegistry
        )

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Provides
    @Singleton
    fun provideSnapshotController(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
        mjpegRegistry: com.sentinel.app.data.playback.MjpegSessionRegistry,
        playbackService: com.sentinel.app.data.playback.CameraPlaybackServiceImpl
    ): SnapshotController =
        com.sentinel.app.data.playback.SnapshotControllerImpl(context, mjpegRegistry, playbackService)
}
