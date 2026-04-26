package com.sentinel.companion.di

import android.content.Context
import com.sentinel.companion.data.db.AlertDao
import com.sentinel.companion.data.db.AppDatabase
import com.sentinel.companion.data.db.CameraDao
import com.sentinel.companion.data.db.DeviceDao
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
    fun provideCameraDao(db: AppDatabase): CameraDao = db.cameraDao()

    @Provides
    @Singleton
    fun provideAlertDao(db: AppDatabase): AlertDao = db.alertDao()
}
