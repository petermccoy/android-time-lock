package com.petermccoy.applock.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.petermccoy.applock.data.LockedAppsRepository
import com.petermccoy.applock.session.SessionState
import com.petermccoy.applock.ui.LockOverlayActivity

/**
 * Watches for foreground app changes and gates locked-but-not-currently-unlocked
 * packages behind [LockOverlayActivity].
 */
class LockAccessibilityService : AccessibilityService() {

    private lateinit var lockedAppsRepository: LockedAppsRepository

    override fun onServiceConnected() {
        super.onServiceConnected()
        lockedAppsRepository = LockedAppsRepository(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!::lockedAppsRepository.isInitialized) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

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
