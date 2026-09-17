package com.petermccoy.applock.session

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory record of which packages are currently unlocked and until when.
 * Intentionally not persisted: if the app process dies, locked apps re-prompt
 * for auth on next foreground, which is the safer default.
 */
object SessionState {

    private val unlockedUntilMillis = ConcurrentHashMap<String, Long>()
    private val unlockedUntilScreenOff = ConcurrentHashMap.newKeySet<String>()

    fun isUnlocked(packageName: String): Boolean {
        if (unlockedUntilScreenOff.contains(packageName)) return true
        val until = unlockedUntilMillis[packageName] ?: return false
        return System.currentTimeMillis() < until
    }

    fun unlock(packageName: String, durationMs: Long) {
        unlockedUntilScreenOff.remove(packageName)
        unlockedUntilMillis[packageName] = System.currentTimeMillis() + durationMs
    }

    /** Unlocks [packageName] with no expiry until the next [clearUntilScreenOff] call. */
    fun unlockUntilScreenOff(packageName: String) {
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
