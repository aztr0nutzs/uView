package com.sentinel.companion.di

import android.content.Context
import com.sentinel.companion.data.db.AppDatabase
import com.sentinel.companion.data.db.DeviceDao
import com.sentinel.companion.data.repository.CameraRepository
import com.sentinel.companion.data.repository.DeviceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)

    @Provides
    @Singleton
    fun provideDeviceDao(db: AppDatabase): DeviceDao = db.deviceDao()

    @Provides
    @Singleton
    fun provideDeviceRepository(dao: DeviceDao): DeviceRepository = DeviceRepository(dao)

    @Provides
    @Singleton
    fun provideCameraRepository(): CameraRepository = CameraRepository()
}
