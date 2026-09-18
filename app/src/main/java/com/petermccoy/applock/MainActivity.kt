package com.petermccoy.applock

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.petermccoy.applock.data.LockedAppsRepository
import com.petermccoy.applock.ui.AppListScreen
import com.petermccoy.applock.ui.LockScreenBackground
import com.petermccoy.applock.ui.theme.AppLockTheme

/**
 * Gates its own UI behind biometric auth, independently of the accessibility-service
 * pipeline used for other locked apps: AppLock controls every other app's lock, so it
 * must never itself be reachable via that same pipeline's timing/task edge cases.
 */
class MainActivity : FragmentActivity() {

    private lateinit var lockedAppsRepository: LockedAppsRepository
    private var isUnlocked by mutableStateOf(false)
    private var promptPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockedAppsRepository = LockedAppsRepository(applicationContext)

        setContent {
            AppLockTheme {
                if (isUnlocked) {
                    AppListScreen(
                        lockedAppsRepository = lockedAppsRepository,
                        onOpenAccessibilitySettings = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onOpenSecuritySettings = {
                            startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                        },
                    )
                } else {
                    LockScreenBackground(appLabel = getString(R.string.app_name))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isUnlocked && !promptPending) {
            promptPending = true
            showBiometricPrompt()
        }
    }

    override fun onStop() {
        super.onStop()
        // Re-authenticate every time the app is reopened after leaving the foreground.
        isUnlocked = false
        promptPending = false
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isUnlocked = true
                    promptPending = false
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    promptPending = false
                    finish()
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.unlock_prompt_title, getString(R.string.app_name)))
            .setSubtitle(getString(R.string.unlock_prompt_subtitle))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
