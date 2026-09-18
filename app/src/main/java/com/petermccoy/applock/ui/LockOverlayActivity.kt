package com.petermccoy.applock.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.petermccoy.applock.R
import com.petermccoy.applock.session.SessionState
import com.petermccoy.applock.ui.theme.AppLockTheme

/**
 * Full-screen auth gate. Launched by [com.petermccoy.applock.accessibility.LockAccessibilityService]
 * whenever a locked, not-currently-unlocked package comes to the foreground.
 */
class LockOverlayActivity : FragmentActivity() {

    private enum class Stage { AUTHENTICATING, PICK_DURATION }

    private lateinit var targetPackage: String
    private lateinit var prefs: SharedPreferences
    private var stage by mutableStateOf(Stage.AUTHENTICATING)
    private var promptRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialTarget = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        if (initialTarget == null) {
            finish()
            return
        }
        targetPackage = initialTarget
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // The lock screen must not be dismissable via back navigation.
        onBackPressedDispatcher.addCallback(this) { /* consume */ }

        setContent {
            AppLockTheme {
                when (stage) {
                    Stage.AUTHENTICATING -> LockScreenBackground(appLabel = targetAppLabel())
                    Stage.PICK_DURATION -> UnlockDurationPicker(
                        initialSelection = unlockDurationFromPrefValue(prefs.getString(KEY_LAST_DURATION, null)),
                        onConfirm = ::onDurationConfirmed,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newTarget = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: return
        if (newTarget == targetPackage) return
        // A different locked app came to the foreground while this overlay
        // (singleTask) was already showing; restart the auth flow for it.
        targetPackage = newTarget
        stage = Stage.AUTHENTICATING
        promptRequested = true
        showBiometricPrompt()
    }

    override fun onResume() {
        super.onResume()
        if (stage == Stage.AUTHENTICATING && !promptRequested) {
            promptRequested = true
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    stage = Stage.PICK_DURATION
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    dismissToHome()
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.unlock_prompt_title, targetAppLabel()))
            .setSubtitle(getString(R.string.unlock_prompt_subtitle))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun onDurationConfirmed(duration: UnlockDuration) {
        prefs.edit().putString(KEY_LAST_DURATION, duration.toPrefValue()).apply()
        when (duration) {
            is UnlockDuration.Fixed -> SessionState.unlock(targetPackage, duration.minutes * 60_000L)
            UnlockDuration.UntilScreenOff -> SessionState.unlockUntilScreenOff(targetPackage)
        }
        finish()
    }

    private fun dismissToHome() {
        finish()
        moveTaskToBack(true)
    }

    private fun targetAppLabel(): String {
        val pm = packageManager
        return try {
            val info: ApplicationInfo = pm.getApplicationInfo(targetPackage, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            targetPackage
        }
    }

    companion object {
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        private const val PREFS_NAME = "applock_overlay_prefs"
        private const val KEY_LAST_DURATION = "last_unlock_duration"
    }
}
