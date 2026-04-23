package com.sentinel.app

import android.app.Application
import com.sentinel.app.core.logging.CrashReportingTree
import com.sentinel.app.core.notifications.NotificationEventRelay
import com.sentinel.app.core.notifications.NotificationHelper
import com.sentinel.app.core.power.PowerAwarenessManager
import com.sentinel.app.data.events.EventPipeline
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SentinelApplication : Application() {

    @Inject lateinit var eventPipeline: EventPipeline
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var notificationEventRelay: NotificationEventRelay
    @Inject lateinit var powerAwarenessManager: PowerAwarenessManager
    @Inject lateinit var crashReportingTree: CrashReportingTree

    override fun onCreate() {
        super.onCreate()

        // Phase 8: Plant crash reporting tree for release builds,
        // DebugTree for debug builds. Both are planted so debug builds
        // get logcat AND crash reporting.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Always plant the crash reporting tree — in debug builds with
        // NoOpCrashReporter it's harmless. In release builds with a real
        // backend (Crashlytics/Sentry), it captures warnings and errors.
        Timber.plant(crashReportingTree)

        // Phase 5: notification channels + event pipeline
        notificationHelper.createChannels()
        eventPipeline.start()
        notificationEventRelay.start()

        // Phase 6: battery and data-saver awareness
        powerAwarenessManager.start()
    }

    override fun onTerminate() {
        powerAwarenessManager.stop()
        super.onTerminate()
    }
}
