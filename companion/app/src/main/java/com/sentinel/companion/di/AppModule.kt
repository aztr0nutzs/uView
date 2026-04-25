package com.sentinel.companion.di

import com.sentinel.companion.data.repository.CameraRepository
import com.sentinel.companion.data.repository.PreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCameraRepository(): CameraRepository = CameraRepository()
}
