package com.petermccoy.applock.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.petermccoy.applock.data.LockedAppsRepository
import com.petermccoy.applock.session.SessionState
import com.petermccoy.applock.ui.LockOverlayActivity

/**
 * Watches for foreground app changes and gates locked-but-not-currently-unlocked
 * packages behind [LockOverlayActivity].
 */
class LockAccessibilityService : AccessibilityService() {

    private lateinit var lockedAppsRepository: LockedAppsRepository
    private var screenOffReceiverRegistered = false

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            SessionState.clearUntilScreenOff()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        lockedAppsRepository = LockedAppsRepository(applicationContext)

        if (!screenOffReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                screenOffReceiver,
                IntentFilter(Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            screenOffReceiverRegistered = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (screenOffReceiverRegistered) {
            unregisterReceiver(screenOffReceiver)
            screenOffReceiverRegistered = false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!::lockedAppsRepository.isInitialized) return

        val packageName = event.packageName?.toString() ?: return

        // AppLock locks itself too (see LockedAppsRepository.isLocked), so its own window
        // state changes reach here. Only skip the lock screen showing itself, to avoid
        // relaunching it on top of itself in a loop; MainActivity coming to the foreground
        // should still be gated normally.
        if (packageName == applicationContext.packageName &&
            event.className?.toString() == LockOverlayActivity::class.java.name
        ) {
            return
        }

        if (lockedAppsRepository.isLocked(packageName) && !SessionState.isUnlocked(packageName)) {
            val intent = Intent(this, LockOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(LockOverlayActivity.EXTRA_TARGET_PACKAGE, packageName)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // No-op: nothing to clean up.
    }
}
