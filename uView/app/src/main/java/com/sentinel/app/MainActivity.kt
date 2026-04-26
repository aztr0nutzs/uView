package com.sentinel.app

import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.sentinel.app.core.receiver.NetworkStateReceiver
import com.sentinel.app.core.security.AppLockManager
import com.sentinel.app.core.service.MonitorServiceController
import com.sentinel.app.data.preferences.AppPreferencesDataSource
import com.sentinel.app.navigation.AppNavGraph
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanPrimary
import com.sentinel.app.ui.theme.SentinelTheme
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainActivity — Phase 8 update
 *
 * Changes from Phase 7:
 *   - Extends [FragmentActivity] (required by BiometricPrompt).
 *   - On cold start, if app lock is enabled, shows the biometric/credential
 *     prompt before rendering the main navigation graph.
 *   - If authentication fails or is cancelled, displays a locked state and
 *     finishes the activity — does NOT grant access.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var preferencesDataSource: AppPreferencesDataSource
    @Inject lateinit var networkStateReceiver: NetworkStateReceiver
    @Inject lateinit var monitorServiceController: MonitorServiceController
    @Inject lateinit var appLockManager: AppLockManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Tracks whether the app has been unlocked this session
    private var isUnlocked by mutableStateOf(false)
    private var lockCheckComplete by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check app lock on cold start
        scope.launch {
            val prefs = preferencesDataSource.preferences.first()
            if (prefs.appLockEnabled) {
                val result = appLockManager.requireUnlock(this@MainActivity)
                when (result) {
                    AppLockManager.AuthResult.SUCCESS -> {
                        isUnlocked = true
                    }
                    AppLockManager.AuthResult.NOT_AVAILABLE -> {
                        // Device has no lock screen — grant access but log warning
                        isUnlocked = true
                    }
                    AppLockManager.AuthResult.CANCELLED,
                    AppLockManager.AuthResult.ERROR -> {
                        // Authentication failed — do not grant access
                        isUnlocked = false
                    }
                }
            } else {
                isUnlocked = true
            }
            lockCheckComplete = true
        }

        setContent {
            val prefs by preferencesDataSource.preferences.collectAsState(
                initial = com.sentinel.app.data.preferences.AppPreferences()
            )
            SentinelTheme(darkTheme = prefs.darkThemeEnabled) {
                if (!lockCheckComplete) {
                    // Show nothing while checking lock — prevents flash of content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundDeep)
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Authenticating…",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                } else if (isUnlocked) {
                    AppNavGraph(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundDeep)
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    )
                } else {
                    // Locked state — user cancelled or failed authentication
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundDeep)
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Sentinel Home is Locked",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Authentication required to access your cameras",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(2000)
                        finish()
                    }
                }
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
