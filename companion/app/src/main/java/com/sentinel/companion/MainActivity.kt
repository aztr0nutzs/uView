package com.sentinel.companion

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.sentinel.companion.data.repository.CameraRepository
import com.sentinel.companion.data.repository.DeviceRepository
import com.sentinel.companion.data.repository.PreferencesRepository
import com.sentinel.companion.navigation.CompanionNavHost
import com.sentinel.companion.navigation.Routes
import com.sentinel.companion.security.BiometricGate
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.SentinelCompanionTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var prefsRepo: PreferencesRepository
    @Inject lateinit var deviceRepo: DeviceRepository
    @Inject lateinit var cameraRepo: CameraRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Re-encrypt any plaintext credentials left over from older app versions.
        // Safe to run on every launch — encrypted rows are skipped.
        lifecycleScope.launch {
            runCatching { deviceRepo.migrateLegacyCredentials() }
            runCatching { cameraRepo.migrateLegacyCredentials() }
        }

        val startDestination = runBlocking {
            val connPrefs = prefsRepo.connectionPrefs.first()
            if (connPrefs.hostAddress.isNotBlank() || connPrefs.autoConnect) Routes.DASHBOARD
            else Routes.CONNECT
        }

        setContent {
            val appPrefs by prefsRepo.appPrefs.collectAsState(initial = null)
            SentinelCompanionTheme {
                if (appPrefs == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundDeep),
                    )
                } else {
                    BiometricGate(enabled = appPrefs?.biometricLock == true) {
                        CompanionNavHost(startDestination = startDestination)
                    }
                }
            }
        }
    }
}
