package com.petermccoy.applock

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.petermccoy.applock.data.LockedAppsRepository
import com.petermccoy.applock.ui.AppListScreen
import com.petermccoy.applock.ui.theme.AppLockTheme

class MainActivity : ComponentActivity() {

    private lateinit var lockedAppsRepository: LockedAppsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockedAppsRepository = LockedAppsRepository(applicationContext)

        setContent {
            AppLockTheme {
                AppListScreen(
                    lockedAppsRepository = lockedAppsRepository,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                )
            }
        }
    }
}
