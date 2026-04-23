package com.sentinel.app.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Phase5Module
 *
 * All Phase 5 components use @Singleton + @Inject constructor and are
 * wired automatically by Hilt without explicit @Provides/@Binds:
 *
 *   MotionDetector            — @Singleton @Inject
 *   MotionMonitorService      — @Singleton @Inject
 *   EventPipeline             — @Singleton @Inject
 *   NotificationHelper        — @Singleton @Inject
 *   NotificationEventRelay    — @Singleton @Inject
 *
 * This module is intentionally empty. It exists as a placeholder for
 * future Phase 5 bindings (e.g. if a MotionAnalysisStrategy interface
 * is introduced with multiple implementations).
 */
@Module
@InstallIn(SingletonComponent::class)
object Phase5Module
