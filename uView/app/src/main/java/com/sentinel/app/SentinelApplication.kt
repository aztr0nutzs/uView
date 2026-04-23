package com.sentinel.app

import android.app.Application
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

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

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
