package com.sentinel.app.core.di

import com.sentinel.app.core.logging.CrashReporter
import com.sentinel.app.core.logging.CrashReportingTree
import com.sentinel.app.core.logging.NoOpCrashReporter
import com.sentinel.app.core.security.AppLockManager
import com.sentinel.app.core.security.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Phase8Module — Security Hardening DI
 *
 * Provides:
 *   - [CryptoManager]         — AES/GCM credential encryption (Android Keystore)
 *   - [AppLockManager]        — BiometricPrompt-based app lock
 *   - [CrashReporter]         — Pluggable crash reporting interface
 *   - [CrashReportingTree]    — Timber tree that delegates to [CrashReporter]
 *
 * To integrate a real crash reporting backend (Crashlytics, Sentry, etc.):
 *   1. Add the vendor SDK dependency to build.gradle.kts
 *   2. Replace [NoOpCrashReporter] binding with your implementation
 */
@Module
@InstallIn(SingletonComponent::class)
object Phase8Module {

    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager = CryptoManager()

    @Provides
    @Singleton
    fun provideCrashReporter(): CrashReporter = NoOpCrashReporter()

    @Provides
    @Singleton
    fun provideCrashReportingTree(reporter: CrashReporter): CrashReportingTree =
        CrashReportingTree(reporter)
}
