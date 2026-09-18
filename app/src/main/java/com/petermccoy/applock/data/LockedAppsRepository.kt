package com.petermccoy.applock.data

import android.content.Context

/**
 * Persisted set of package names the user has chosen to lock.
 */
class LockedAppsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** AppLock always locks itself, so opening it requires auth too; it's not user-toggleable. */
    fun isLocked(packageName: String): Boolean =
        packageName == appContext.packageName || getAllLocked().contains(packageName)

    fun setLocked(packageName: String, locked: Boolean) {
        val updated = getAllLocked().toMutableSet()
        if (locked) updated.add(packageName) else updated.remove(packageName)
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, updated).apply()
    }

    fun getAllLocked(): Set<String> =
        prefs.getStringSet(KEY_LOCKED_PACKAGES, emptySet()) ?: emptySet()

    private companion object {
        const val PREFS_NAME = "locked_apps"
        const val KEY_LOCKED_PACKAGES = "locked_packages"
    }
}
