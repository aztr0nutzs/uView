package com.sentinel.app.core.logging

import android.util.Log
import timber.log.Timber

/**
 * CrashReportingTree — Phase 8
 *
 * A [Timber.Tree] that intercepts warning and error-level logs and forwards
 * them to a pluggable crash reporting backend.
 *
 * What is real:
 *   - Filters logs at WARN and ERROR levels — everything else is dropped.
 *   - Captures log messages and throwables into a structured [CrashReport].
 *   - Delegates to a [CrashReporter] interface, allowing any backend
 *     (Crashlytics, Sentry, Bugsnag, etc.) to be swapped in at DI time.
 *   - Ships with [NoOpCrashReporter] for builds where no crash service is
 *     configured, ensuring zero fake reporting behavior.
 *
 * What is NOT implemented:
 *   - A concrete Crashlytics or Sentry integration. This requires adding
 *     the vendor SDK as a dependency. The pluggable interface is ready.
 *   - User identification or session tagging — would be added in the
 *     concrete [CrashReporter] implementation.
 *
 * Integration:
 *   Plant this tree in [SentinelApplication.onCreate] for release builds.
 *   Debug builds should continue to use [Timber.DebugTree] for logcat output.
 */
class CrashReportingTree(
    private val reporter: CrashReporter
) : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean =
        priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val level = when (priority) {
            Log.WARN  -> CrashReport.Level.WARNING
            Log.ERROR -> CrashReport.Level.ERROR
            Log.ASSERT -> CrashReport.Level.FATAL
            else -> return  // Should not happen due to isLoggable filter
        }

        val report = CrashReport(
            level = level,
            tag = tag,
            message = message,
            throwable = t,
            timestampMs = System.currentTimeMillis()
        )

        reporter.report(report)

        // If there's a throwable at ERROR or FATAL level, also report the exception
        if (t != null && level != CrashReport.Level.WARNING) {
            reporter.reportException(t)
        }
    }
}

/**
 * Structured crash report emitted by [CrashReportingTree].
 */
data class CrashReport(
    val level: Level,
    val tag: String?,
    val message: String,
    val throwable: Throwable?,
    val timestampMs: Long
) {
    enum class Level { WARNING, ERROR, FATAL }
}

/**
 * Pluggable interface for crash reporting backends.
 *
 * To integrate a real crash service:
 *   1. Add the vendor SDK to build.gradle.kts
 *   2. Create a class that implements [CrashReporter]
 *   3. Bind it in [Phase8Module] instead of [NoOpCrashReporter]
 *
 * Example for Firebase Crashlytics:
 * ```
 * class CrashlyticsCrashReporter : CrashReporter {
 *     override fun report(report: CrashReport) {
 *         FirebaseCrashlytics.getInstance().log("${report.tag}: ${report.message}")
 *     }
 *     override fun reportException(throwable: Throwable) {
 *         FirebaseCrashlytics.getInstance().recordException(throwable)
 *     }
 *     override fun setUserId(userId: String) {
 *         FirebaseCrashlytics.getInstance().setUserId(userId)
 *     }
 * }
 * ```
 */
interface CrashReporter {
    /** Log a structured crash report (warning or error). */
    fun report(report: CrashReport)

    /** Report an exception/throwable directly. */
    fun reportException(throwable: Throwable)

    /** Set a user identifier for session tracking (optional). */
    fun setUserId(userId: String) {}
}

/**
 * No-op implementation used when no crash reporting service is configured.
 * This is NOT fake reporting — it explicitly does nothing.
 * Swap this out in [Phase8Module] when integrating a real backend.
 */
class NoOpCrashReporter : CrashReporter {
    override fun report(report: CrashReport) {
        // Intentionally empty — no crash service configured.
        // In a production build, replace this binding with a real implementation.
    }

    override fun reportException(throwable: Throwable) {
        // Intentionally empty — no crash service configured.
    }
}
