package com.sentinel.app.core.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.sentinel.app.data.preferences.AppPreferencesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * AppLockManager — Phase 8
 *
 * Manages the app-lock gate using Android's BiometricPrompt API.
 *
 * What is real:
 *   - Reads the `appLockEnabled` preference reactively.
 *   - Uses [BiometricManager] to check hardware/enrollment availability.
 *   - Presents a real [BiometricPrompt] (fingerprint / face / device credential).
 *   - Falls back to device credential (PIN/pattern/password) if biometrics are
 *     not enrolled, using [Authenticators.DEVICE_CREDENTIAL].
 *   - Returns an honest result: SUCCESS, CANCELLED, or NOT_AVAILABLE.
 *
 * What is NOT implemented:
 *   - Custom PIN entry inside the app. We rely entirely on the system
 *     credential screen. A custom PIN would require a separate enrollment UI.
 *   - Auto-lock timeout. Currently the lock is checked once on each cold
 *     start in [MainActivity]. A timeout-based re-lock would require lifecycle
 *     tracking.
 */
@Singleton
class AppLockManager @Inject constructor(
    private val preferencesDataSource: AppPreferencesDataSource
) {

    /**
     * Observe whether app lock is enabled. The Settings toggle writes this pref.
     */
    val isEnabled: Flow<Boolean> = preferencesDataSource.preferences
        .map { it.appLockEnabled }
        .distinctUntilChanged()

    /**
     * Check whether the device supports at least one form of authentication
     * that we accept (biometric OR device credential).
     */
    fun canAuthenticate(activity: FragmentActivity): AuthCapability {
        val biometricManager = BiometricManager.from(activity)
        val allowedAuthenticators = Authenticators.BIOMETRIC_STRONG or
                Authenticators.BIOMETRIC_WEAK or
                Authenticators.DEVICE_CREDENTIAL

        return when (biometricManager.canAuthenticate(allowedAuthenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                AuthCapability.AVAILABLE

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                // Device credential (PIN/pattern) might still work
                when (biometricManager.canAuthenticate(Authenticators.DEVICE_CREDENTIAL)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> AuthCapability.DEVICE_CREDENTIAL_ONLY
                    else -> AuthCapability.NOT_AVAILABLE
                }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                // Last resort: try device credential alone
                when (biometricManager.canAuthenticate(Authenticators.DEVICE_CREDENTIAL)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> AuthCapability.DEVICE_CREDENTIAL_ONLY
                    else -> AuthCapability.NOT_AVAILABLE
                }

            else -> AuthCapability.NOT_AVAILABLE
        }
    }

    /**
     * Show the system authentication prompt (biometric + device credential).
     * Suspends until the user completes or cancels.
     *
     * @return [AuthResult.SUCCESS] if authenticated, [AuthResult.CANCELLED] if the
     *         user dismissed the prompt, [AuthResult.NOT_AVAILABLE] if the device
     *         has no lock screen set up.
     */
    suspend fun requireUnlock(activity: FragmentActivity): AuthResult {
        val capability = canAuthenticate(activity)
        if (capability == AuthCapability.NOT_AVAILABLE) {
            Timber.w("AppLockManager: no authentication method available — granting access")
            return AuthResult.NOT_AVAILABLE
        }

        return suspendCancellableCoroutine { continuation ->
            val executor = ContextCompat.getMainExecutor(activity)

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    Timber.d("AppLockManager: authentication succeeded")
                    if (continuation.isActive) continuation.resume(AuthResult.SUCCESS)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Timber.w("AppLockManager: authentication error $errorCode — $errString")
                    if (continuation.isActive) {
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED ->
                                continuation.resume(AuthResult.CANCELLED)
                            else ->
                                continuation.resume(AuthResult.ERROR)
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    // Called when a biometric is recognized but doesn't match.
                    // The prompt stays open for retry — don't resume yet.
                    Timber.d("AppLockManager: authentication attempt failed (retry allowed)")
                }
            }

            val prompt = BiometricPrompt(activity, executor, callback)

            val authenticators = if (capability == AuthCapability.DEVICE_CREDENTIAL_ONLY) {
                Authenticators.DEVICE_CREDENTIAL
            } else {
                Authenticators.BIOMETRIC_STRONG or
                        Authenticators.BIOMETRIC_WEAK or
                        Authenticators.DEVICE_CREDENTIAL
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Sentinel Home")
                .setSubtitle("Authenticate to access your cameras")
                .setAllowedAuthenticators(authenticators)
                .build()

            prompt.authenticate(promptInfo)

            continuation.invokeOnCancellation {
                prompt.cancelAuthentication()
            }
        }
    }

    /**
     * Enable or disable app lock. Persists the preference.
     * If enabling on a device with no lock screen, returns false.
     */
    suspend fun setEnabled(enabled: Boolean, activity: FragmentActivity): Boolean {
        if (enabled) {
            val capability = canAuthenticate(activity)
            if (capability == AuthCapability.NOT_AVAILABLE) {
                Timber.w("AppLockManager: cannot enable — no auth method available")
                return false
            }
        }
        preferencesDataSource.setAppLockEnabled(enabled)
        return true
    }

    enum class AuthCapability {
        AVAILABLE,              // Biometric + device credential
        DEVICE_CREDENTIAL_ONLY, // PIN/pattern only, no biometric
        NOT_AVAILABLE           // No lock screen configured
    }

    enum class AuthResult {
        SUCCESS,
        CANCELLED,
        ERROR,
        NOT_AVAILABLE
    }
}
