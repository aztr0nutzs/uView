package com.sentinel.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.sentinel.app.domain.model.StreamQualityProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sentinel_preferences"
)

// ─────────────────────────────────────────────────────────────────────────────
// AppPreferences — typed model exposed to the rest of the app
// ─────────────────────────────────────────────────────────────────────────────

data class AppPreferences(
    val darkThemeEnabled: Boolean = true,
    val autoReconnectEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val localStoragePath: String = "",
    val defaultStreamQuality: StreamQualityProfile = StreamQualityProfile.AUTO,
    val motionSensitivity: MotionSensitivity = MotionSensitivity.MEDIUM,
    val localOnlyMode: Boolean = false,
    val networkScanEnabled: Boolean = true,
    val networkScanSubnet: String = "",  // empty = auto-detect
    val dataSaverMode: Boolean = false,
    val appLockEnabled: Boolean = false, // future: biometric/PIN
    val diagnosticsLoggingEnabled: Boolean = false,
    val eventRetentionDays: Int = 30
)

enum class MotionSensitivity(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CUSTOM("Custom")
}

// ─────────────────────────────────────────────────────────────────────────────
// AppPreferencesDataSource
// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class AppPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LOCAL_STORAGE_PATH = stringPreferencesKey("local_storage_path")
        val DEFAULT_STREAM_QUALITY = stringPreferencesKey("default_stream_quality")
        val MOTION_SENSITIVITY = stringPreferencesKey("motion_sensitivity")
        val LOCAL_ONLY_MODE = booleanPreferencesKey("local_only_mode")
        val NETWORK_SCAN_ENABLED = booleanPreferencesKey("network_scan_enabled")
        val NETWORK_SCAN_SUBNET = stringPreferencesKey("network_scan_subnet")
        val DATA_SAVER_MODE = booleanPreferencesKey("data_saver_mode")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val DIAGNOSTICS_LOGGING = booleanPreferencesKey("diagnostics_logging")
        val EVENT_RETENTION_DAYS = intPreferencesKey("event_retention_days")
    }

    val preferences: Flow<AppPreferences> = dataStore.data
        .catch { exception ->
            Timber.e(exception, "Failed to read preferences")
            emit(emptyPreferences())
        }
        .map { prefs ->
            AppPreferences(
                darkThemeEnabled = prefs[Keys.DARK_THEME] ?: true,
                autoReconnectEnabled = prefs[Keys.AUTO_RECONNECT] ?: true,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                localStoragePath = prefs[Keys.LOCAL_STORAGE_PATH] ?: "",
                defaultStreamQuality = prefs[Keys.DEFAULT_STREAM_QUALITY]?.let {
                    runCatching { StreamQualityProfile.valueOf(it) }.getOrNull()
                } ?: StreamQualityProfile.AUTO,
                motionSensitivity = prefs[Keys.MOTION_SENSITIVITY]?.let {
                    runCatching { MotionSensitivity.valueOf(it) }.getOrNull()
                } ?: MotionSensitivity.MEDIUM,
                localOnlyMode = prefs[Keys.LOCAL_ONLY_MODE] ?: false,
                networkScanEnabled = prefs[Keys.NETWORK_SCAN_ENABLED] ?: true,
                networkScanSubnet = prefs[Keys.NETWORK_SCAN_SUBNET] ?: "",
                dataSaverMode = prefs[Keys.DATA_SAVER_MODE] ?: false,
                appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
                diagnosticsLoggingEnabled = prefs[Keys.DIAGNOSTICS_LOGGING] ?: false,
                eventRetentionDays = prefs[Keys.EVENT_RETENTION_DAYS] ?: 30
            )
        }

    suspend fun setDarkTheme(enabled: Boolean) = update { it[Keys.DARK_THEME] = enabled }
    suspend fun setAutoReconnect(enabled: Boolean) = update { it[Keys.AUTO_RECONNECT] = enabled }
    suspend fun setNotificationsEnabled(enabled: Boolean) = update { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    suspend fun setLocalStoragePath(path: String) = update { it[Keys.LOCAL_STORAGE_PATH] = path }
    suspend fun setDefaultStreamQuality(q: StreamQualityProfile) = update { it[Keys.DEFAULT_STREAM_QUALITY] = q.name }
    suspend fun setMotionSensitivity(s: MotionSensitivity) = update { it[Keys.MOTION_SENSITIVITY] = s.name }
    suspend fun setLocalOnlyMode(enabled: Boolean) = update { it[Keys.LOCAL_ONLY_MODE] = enabled }
    suspend fun setNetworkScanEnabled(enabled: Boolean) = update { it[Keys.NETWORK_SCAN_ENABLED] = enabled }
    suspend fun setNetworkScanSubnet(subnet: String) = update { it[Keys.NETWORK_SCAN_SUBNET] = subnet }
    suspend fun setDataSaverMode(enabled: Boolean) = update { it[Keys.DATA_SAVER_MODE] = enabled }
    suspend fun setAppLockEnabled(enabled: Boolean) = update { it[Keys.APP_LOCK_ENABLED] = enabled }
    suspend fun setDiagnosticsLogging(enabled: Boolean) = update { it[Keys.DIAGNOSTICS_LOGGING] = enabled }
    suspend fun setEventRetentionDays(days: Int) = update { it[Keys.EVENT_RETENTION_DAYS] = days }

    private suspend fun update(block: (MutablePreferences) -> Unit) {
        dataStore.edit { prefs -> block(prefs) }
    }
}
