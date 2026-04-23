package com.sentinel.app.data.playback

import com.sentinel.app.domain.model.ReconnectPolicy
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.min
import kotlin.math.pow

/**
 * ReconnectScheduler
 *
 * Implements the exponential backoff described in [ReconnectPolicy].
 *
 * Usage:
 *   val scheduler = ReconnectScheduler(policy)
 *   while (scheduler.shouldRetry()) {
 *       scheduler.waitBeforeNextAttempt()
 *       val success = tryConnect()
 *       if (success) { scheduler.reset(); break }
 *   }
 *
 * Thread-safe for use within a single coroutine. Not thread-safe across
 * multiple coroutines — each camera gets its own instance.
 */
class ReconnectScheduler(private val policy: ReconnectPolicy) {

    private var attemptCount = 0

    val currentAttempt: Int get() = attemptCount

    fun shouldRetry(): Boolean =
        policy.enabled && attemptCount < policy.maxAttempts

    fun reset() {
        attemptCount = 0
    }

    /**
     * Suspends for the backoff delay appropriate to the current attempt count,
     * then increments the counter.
     *
     * Delay formula: min(initialDelay × multiplier^attempt, maxDelay)
     */
    suspend fun waitBeforeNextAttempt() {
        val delayMs = computeDelay()
        Timber.d("ReconnectScheduler: attempt ${attemptCount + 1}/${policy.maxAttempts}, waiting ${delayMs}ms")
        delay(delayMs)
        attemptCount++
    }

    fun computeDelay(): Long {
        val raw = policy.initialDelayMs * policy.backoffMultiplier.pow(attemptCount)
        return min(raw.toLong(), policy.maxDelayMs)
    }

    private fun Float.pow(exp: Int): Float {
        var result = 1f
        repeat(exp) { result *= this }
        return result
    }
}

/**
 * Default reconnect policy used by [CameraPlaybackServiceImpl].
 * Retries up to 5 times with exponential backoff capped at 30 seconds.
 */
val DefaultReconnectPolicy = ReconnectPolicy(
    enabled           = true,
    maxAttempts       = 5,
    initialDelayMs    = 2_000L,
    backoffMultiplier = 1.5f,
    maxDelayMs        = 30_000L
)
