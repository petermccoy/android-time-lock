package com.petermccoy.applock.session

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory record of which packages are currently unlocked and until when.
 * Intentionally not persisted: if the app process dies, locked apps re-prompt
 * for auth on next foreground, which is the safer default.
 */
object SessionState {

    /** Notified when a fixed-duration unlock's timer runs out. */
    fun interface ExpiryListener {
        fun onUnlockExpired(packageName: String)
    }

    private val unlockedUntilMillis = ConcurrentHashMap<String, Long>()
    private val unlockedUntilScreenOff = ConcurrentHashMap.newKeySet<String>()
    private val pendingExpiries = ConcurrentHashMap<String, Runnable>()
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var expiryListener: ExpiryListener? = null

    fun setExpiryListener(listener: ExpiryListener?) {
        expiryListener = listener
    }

    fun isUnlocked(packageName: String): Boolean {
        if (unlockedUntilScreenOff.contains(packageName)) return true
        val until = unlockedUntilMillis[packageName] ?: return false
        return System.currentTimeMillis() < until
    }

    /** Unlocks [packageName] for [durationMs] and schedules [ExpiryListener] to fire the moment it runs out. */
    fun unlock(packageName: String, durationMs: Long) {
        unlockedUntilScreenOff.remove(packageName)
        val expiresAt = System.currentTimeMillis() + durationMs
        unlockedUntilMillis[packageName] = expiresAt

        pendingExpiries.remove(packageName)?.let { handler.removeCallbacks(it) }
        val expiryRunnable = Runnable {
            pendingExpiries.remove(packageName)
            // Only fire if this is still the current unlock, not one superseded by a newer call.
            if (unlockedUntilMillis[packageName] == expiresAt) {
                expiryListener?.onUnlockExpired(packageName)
            }
        }
        pendingExpiries[packageName] = expiryRunnable
        handler.postDelayed(expiryRunnable, durationMs)
    }

    /** Unlocks [packageName] with no expiry until the next [clearUntilScreenOff] call. */
    fun unlockUntilScreenOff(packageName: String) {
        pendingExpiries.remove(packageName)?.let { handler.removeCallbacks(it) }
        unlockedUntilMillis.remove(packageName)
        unlockedUntilScreenOff.add(packageName)
    }

    /** Called when the device screen turns off, to re-lock every "until screen off" unlock. */
    fun clearUntilScreenOff() {
        unlockedUntilScreenOff.clear()
    }

    /** Packages currently unlocked, mapped to remaining unlock time in ms, or null if unlocked until screen off. */
    fun activeUnlocks(): Map<String, Long?> {
        val now = System.currentTimeMillis()
        val result = LinkedHashMap<String, Long?>()
        unlockedUntilScreenOff.forEach { packageName -> result[packageName] = null }
        unlockedUntilMillis.forEach { (packageName, until) ->
            if (now < until) result[packageName] = until - now
        }
        return result
    }
}
