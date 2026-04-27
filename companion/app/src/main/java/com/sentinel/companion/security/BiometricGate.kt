package com.sentinel.companion.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sentinel.companion.ui.components.PrimaryButton
import com.sentinel.companion.ui.theme.BackgroundDeep
import com.sentinel.companion.ui.theme.ErrorRed
import com.sentinel.companion.ui.theme.OrangePrimary
import com.sentinel.companion.ui.theme.OrangeSubtle
import com.sentinel.companion.ui.theme.SurfaceStroke
import com.sentinel.companion.ui.theme.TextDisabled
import com.sentinel.companion.ui.theme.TextPrimary
import com.sentinel.companion.ui.theme.TextSecondary

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HW_UNAVAILABLE,
    NONE_ENROLLED,
    UNSUPPORTED,
}

fun biometricAvailability(ctx: android.content.Context): BiometricAvailability {
    val mgr = BiometricManager.from(ctx)
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
    return when (mgr.canAuthenticate(authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS               -> BiometricAvailability.AVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE     -> BiometricAvailability.NO_HARDWARE
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE  -> BiometricAvailability.HW_UNAVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED   -> BiometricAvailability.NONE_ENROLLED
        else                                             -> BiometricAvailability.UNSUPPORTED
    }
}

/**
 * Wrap the app content with a biometric / device-credential gate. While locked,
 * [content] is not composed at all — the lock screen is the only thing the user
 * sees, so even a screenshot triggered before unlock cannot leak app state.
 *
 * The host activity must be a [FragmentActivity] (BiometricPrompt requirement).
 */
@Composable
fun BiometricGate(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? FragmentActivity

    var unlocked by remember { mutableStateOf(false) }
    var lastError by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableStateOf(0) }
    var shouldPrompt by remember { mutableStateOf(false) }

    if (activity == null) {
        // Without a FragmentActivity we cannot show BiometricPrompt — refuse to
        // pretend we're locked. Make the failure visible.
        LockFailedScreen(
            reason = "App lock requires FragmentActivity host (got ${context.javaClass.simpleName})",
            onRetry = { /* no-op — this is a configuration bug, not a runtime issue */ },
        )
        return
    }

    val availability = remember { biometricAvailability(context) }
    if (availability != BiometricAvailability.AVAILABLE) {
        LockFailedScreen(
            reason = "Device credential unavailable: ${availability.name}. Disable APP_LOCK in settings to continue.",
            onRetry = { /* user must change settings */ },
        )
        return
    }

    fun prompt() {
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                unlocked = true
                lastError = null
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                lastError = "Auth error $errorCode: $errString"
                shouldPrompt = false
            }
            override fun onAuthenticationFailed() {
                attempts += 1
                lastError = "Biometric did not match (attempt $attempts)"
            }
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Sentinel Companion")
            .setSubtitle("Biometric or device credential required")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        BiometricPrompt(activity, executor, callback).authenticate(info)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (!unlocked) shouldPrompt = true
                Lifecycle.Event.ON_STOP -> {
                    unlocked = false
                    shouldPrompt = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        shouldPrompt = true
    }

    // Auto-prompt on launch and after returning from background; user can also
    // tap UNLOCK after cancellation or a non-matching biometric attempt.
    LaunchedEffect(shouldPrompt, unlocked) {
        if (shouldPrompt && !unlocked) {
            shouldPrompt = false
            prompt()
        }
    }

    if (!unlocked) {
        LockScreen(
            error = lastError,
            onUnlock = {
                shouldPrompt = false
                prompt()
            },
        )
    } else {
        content()
    }
}

@Composable
private fun LockScreen(error: String?, onUnlock: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(OrangeSubtle, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(40.dp),
                )
            }
            Text("APP_LOCK_ACTIVE", color = TextPrimary, fontSize = 14.sp,
                fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(
                "Authenticate with biometric or device credential to continue.",
                color = TextSecondary, fontSize = 12.sp,
            )
            if (error != null) {
                Text(
                    text = "// $error",
                    color = ErrorRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = "UNLOCK",
                onClick = onUnlock,
                icon = Icons.Filled.Fingerprint,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LockFailedScreen(reason: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .background(BackgroundDeep, RoundedCornerShape(12.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("APP_LOCK_BLOCKED", color = ErrorRed, fontSize = 13.sp,
                fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(reason, color = TextDisabled, fontSize = 11.sp)
        }
    }
}
