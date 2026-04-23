package com.sentinel.app.core.notifications

import com.sentinel.app.data.preferences.AppPreferencesDataSource
import com.sentinel.app.domain.model.CameraEventType
import com.sentinel.app.domain.repository.CameraEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationEventRelay
 *
 * Observes the [CameraEventRepository] for new events and posts
 * notifications via [NotificationHelper] when:
 *   - Notifications are enabled in app preferences
 *   - The event type warrants a notification
 *
 * This intentionally runs off-pipeline from [EventPipeline] — it reads
 * events that were already persisted to the DB, so if the notification
 * is missed (app killed), the event is still in the history.
 *
 * The relay tracks the last seen event timestamp to avoid re-notifying
 * for events it has already processed across sessions.
 */
@Singleton
class NotificationEventRelay @Inject constructor(
    private val eventRepository: CameraEventRepository,
    private val notificationHelper: NotificationHelper,
    private val prefsDataSource: AppPreferencesDataSource
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastProcessedTimestampMs = System.currentTimeMillis()

    /** Event types that generate a push notification. */
    private val notifiableTypes = setOf(
        CameraEventType.MOTION_DETECTED,
        CameraEventType.CONNECTION_LOST,
        CameraEventType.CONNECTION_RESTORED,
        CameraEventType.STREAM_ERROR
    )

    fun start() {
        scope.launch {
            eventRepository.observeEvents(limit = 50)
                .distinctUntilChanged()
                .collect { events ->
                    val prefs = prefsDataSource.preferences.first()
                    if (!prefs.notificationsEnabled) return@collect

                    // Only process new events since relay started (or last processed)
                    val newEvents = events.filter {
                        it.timestampMs > lastProcessedTimestampMs &&
                        it.eventType in notifiableTypes &&
                        !it.isRead
                    }

                    newEvents.forEach { event ->
                        notificationHelper.postEventNotification(event)
                        Timber.d("NotificationRelay: notified for ${event.eventType} on ${event.cameraName}")
                    }

                    if (newEvents.isNotEmpty()) {
                        lastProcessedTimestampMs = newEvents.maxOf { it.timestampMs }
                    }
                }
        }
    }
}
