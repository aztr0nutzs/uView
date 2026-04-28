package com.sentinel.companion.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sentinel.companion.data.model.AppPrefs
import com.sentinel.companion.data.model.ConnectionPrefs
import com.sentinel.companion.data.model.StreamQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("companion_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val HOST_ADDRESS         = stringPreferencesKey("host_address")
        val HOST_PORT            = intPreferencesKey("host_port")
        val USE_HTTPS            = booleanPreferencesKey("use_https")
        val AUTO_CONNECT         = booleanPreferencesKey("auto_connect")
        val LAST_CONNECTED_MS    = longPreferencesKey("last_connected_ms")
        val DARK_THEME           = booleanPreferencesKey("dark_theme")
        val NOTIFICATIONS        = booleanPreferencesKey("notifications_enabled")
        val MOTION_ALERTS        = booleanPreferencesKey("motion_alerts")
        val CONNECTION_ALERTS    = booleanPreferencesKey("connection_alerts")
        val DATA_SAVER           = booleanPreferencesKey("data_saver")
        val STREAM_QUALITY       = stringPreferencesKey("stream_quality")
        val BIOMETRIC_LOCK       = booleanPreferencesKey("biometric_lock")
        val LOCAL_ONLY_MODE      = booleanPreferencesKey("local_only_mode")
        val PAIRED_AUTH_TOKEN    = stringPreferencesKey("paired_auth_token")
    }

    val pairedAuthToken: Flow<String> = context.dataStore.data.map {
        it[Keys.PAIRED_AUTH_TOKEN] ?: ""
    }

    suspend fun savePairedHubAuthToken(encryptedToken: String) {
        context.dataStore.edit { it[Keys.PAIRED_AUTH_TOKEN] = encryptedToken }
    }

    val connectionPrefs: Flow<ConnectionPrefs> = context.dataStore.data.map { prefs ->
        ConnectionPrefs(
            hostAddress     = prefs[Keys.HOST_ADDRESS] ?: "",
            port            = prefs[Keys.HOST_PORT] ?: 8080,
            useHttps        = prefs[Keys.USE_HTTPS] ?: false,
            autoConnect     = prefs[Keys.AUTO_CONNECT] ?: true,
            lastConnectedMs = prefs[Keys.LAST_CONNECTED_MS] ?: 0L,
        )
    }

    val appPrefs: Flow<AppPrefs> = context.dataStore.data.map { prefs ->
        AppPrefs(
            darkTheme              = prefs[Keys.DARK_THEME] ?: true,
            notificationsEnabled   = prefs[Keys.NOTIFICATIONS] ?: true,
            motionAlertsEnabled    = prefs[Keys.MOTION_ALERTS] ?: true,
            connectionAlertsEnabled= prefs[Keys.CONNECTION_ALERTS] ?: true,
            dataSaverMode          = prefs[Keys.DATA_SAVER] ?: false,
            streamQuality          = StreamQuality.entries.firstOrNull {
                it.name == prefs[Keys.STREAM_QUALITY]
            } ?: StreamQuality.AUTO,
            biometricLock          = prefs[Keys.BIOMETRIC_LOCK] ?: false,
            localOnlyMode          = prefs[Keys.LOCAL_ONLY_MODE] ?: false,
        )
    }

    suspend fun saveConnectionPrefs(prefs: ConnectionPrefs) {
        context.dataStore.edit {
            it[Keys.HOST_ADDRESS]      = prefs.hostAddress
            it[Keys.HOST_PORT]         = prefs.port
            it[Keys.USE_HTTPS]         = prefs.useHttps
            it[Keys.AUTO_CONNECT]      = prefs.autoConnect
            it[Keys.LAST_CONNECTED_MS] = prefs.lastConnectedMs
        }
    }

    suspend fun saveAppPrefs(prefs: AppPrefs) {
        context.dataStore.edit {
            it[Keys.DARK_THEME]       = prefs.darkTheme
            it[Keys.NOTIFICATIONS]    = prefs.notificationsEnabled
            it[Keys.MOTION_ALERTS]    = prefs.motionAlertsEnabled
            it[Keys.CONNECTION_ALERTS]= prefs.connectionAlertsEnabled
            it[Keys.DATA_SAVER]       = prefs.dataSaverMode
            it[Keys.STREAM_QUALITY]   = prefs.streamQuality.name
            it[Keys.BIOMETRIC_LOCK]   = prefs.biometricLock
            it[Keys.LOCAL_ONLY_MODE]  = prefs.localOnlyMode
        }
    }
}
