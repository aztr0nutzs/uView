package com.sentinel.app.core.di

import com.sentinel.app.data.recording.LocalRecordingController
import com.sentinel.app.domain.service.RecordingController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class Phase7Module {

    @Binds
    @Singleton
    abstract fun bindRecordingController(
        impl: LocalRecordingController
    ): RecordingController
}
