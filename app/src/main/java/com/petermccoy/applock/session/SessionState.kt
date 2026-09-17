package com.petermccoy.applock.session

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory record of which packages are currently unlocked and until when.
 * Intentionally not persisted: if the app process dies, locked apps re-prompt
 * for auth on next foreground, which is the safer default.
 */
object SessionState {

    private val unlockedUntilMillis = ConcurrentHashMap<String, Long>()

    fun isUnlocked(packageName: String): Boolean {
        val until = unlockedUntilMillis[packageName] ?: return false
        return System.currentTimeMillis() < until
    }

    fun unlock(packageName: String, durationMs: Long) {
        unlockedUntilMillis[packageName] = System.currentTimeMillis() + durationMs
    }

    /** Packages currently unlocked, mapped to their remaining time in milliseconds. */
    fun activeUnlocks(): Map<String, Long> {
        val now = System.currentTimeMillis()
        return unlockedUntilMillis
            .filterValues { now < it }
            .mapValues { (_, until) -> until - now }
    }
}
