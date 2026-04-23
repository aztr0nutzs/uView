package com.sentinel.app.core.di

import com.sentinel.app.data.discovery.CameraDiscoveryServiceImpl
import com.sentinel.app.domain.service.CameraDiscoveryService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DiscoveryModule
 *
 * Replaces the old [ServiceModule] binding for [CameraDiscoveryService].
 * The new [CameraDiscoveryServiceImpl] in data.discovery supersedes the
 * placeholder in data.remote.adapters.
 *
 * All sub-components (SubnetDetector, ArpTableScanner, MdnsDiscovery,
 * OnvifWsDiscovery, PortProber, OnvifProbeClient) are @Singleton with
 * @Inject constructors — Hilt creates them automatically.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoveryModule {

    @Binds
    @Singleton
    abstract fun bindCameraDiscoveryService(
        impl: CameraDiscoveryServiceImpl
    ): CameraDiscoveryService
}
