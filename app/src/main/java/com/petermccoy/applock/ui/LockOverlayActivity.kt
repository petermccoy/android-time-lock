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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
                        initialMinutes = prefs.getInt(KEY_LAST_DURATION_MINUTES, DEFAULT_DURATION_MINUTES),
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

    private fun onDurationConfirmed(minutes: Int) {
        prefs.edit().putInt(KEY_LAST_DURATION_MINUTES, minutes).apply()
        SessionState.unlock(targetPackage, minutes * 60_000L)
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
        private const val KEY_LAST_DURATION_MINUTES = "last_unlock_duration_minutes"
        private const val DEFAULT_DURATION_MINUTES = 5
    }
}

@Composable
private fun LockScreenBackground(appLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Text(text = appLabel, style = MaterialTheme.typography.titleLarge)
        }
    }
}
