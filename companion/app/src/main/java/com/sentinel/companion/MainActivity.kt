package com.sentinel.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import com.sentinel.companion.data.repository.PreferencesRepository
import com.sentinel.companion.navigation.CompanionNavHost
import com.sentinel.companion.navigation.Routes
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.SentinelCompanionTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefsRepo: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Determine start destination based on saved prefs
        val startDestination = runBlocking {
            val connPrefs = prefsRepo.connectionPrefs.first()
            if (connPrefs.hostAddress.isNotBlank() || connPrefs.autoConnect) {
                Routes.DASHBOARD
            } else {
                Routes.CONNECT
            }
        }

        setContent {
            SentinelCompanionTheme {
                CompanionNavHost(startDestination = startDestination)
            }
        }
    }
}
