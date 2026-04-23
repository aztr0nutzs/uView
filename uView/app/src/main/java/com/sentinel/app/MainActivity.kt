package com.sentinel.app

import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sentinel.app.core.receiver.NetworkStateReceiver
import com.sentinel.app.core.service.MonitorServiceController
import com.sentinel.app.data.preferences.AppPreferencesDataSource
import com.sentinel.app.navigation.AppNavGraph
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.SentinelTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesDataSource: AppPreferencesDataSource
    @Inject lateinit var networkStateReceiver: NetworkStateReceiver
    @Inject lateinit var monitorServiceController: MonitorServiceController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs by preferencesDataSource.preferences.collectAsState(
                initial = com.sentinel.app.data.preferences.AppPreferences()
            )
            SentinelTheme(darkTheme = prefs.darkThemeEnabled) {
                AppNavGraph(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundDeep)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Register network receiver dynamically — avoids unnecessary wakeups
        val filter = IntentFilter().apply {
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction("android.net.wifi.WIFI_STATE_CHANGED")
            addAction("android.net.wifi.STATE_CHANGE")
        }
        @Suppress("UnspecifiedRegisterReceiverFlag")
        registerReceiver(networkStateReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        // Unregister network receiver when app is fully hidden
        try { unregisterReceiver(networkStateReceiver) } catch (_: Exception) {}

        // Start background monitoring service when app goes to background
        scope.launch {
            monitorServiceController.startIfEnabled()
        }
    }

    override fun onResume() {
        super.onResume()
        // Stop the background service when user returns to the app —
        // the foreground UI handles its own playback and monitoring
        monitorServiceController.stop()
    }
}
