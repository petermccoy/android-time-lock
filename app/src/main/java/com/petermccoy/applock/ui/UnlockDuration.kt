package com.petermccoy.applock.ui

/** How long a successful auth should keep an app unlocked. */
sealed interface UnlockDuration {
    data class Fixed(val minutes: Int) : UnlockDuration
    data object UntilScreenOff : UnlockDuration
}

val FIXED_UNLOCK_DURATION_MINUTES = listOf(5, 10, 15, 30, 45, 60)

val UNLOCK_DURATION_OPTIONS: List<UnlockDuration> =
    listOf(UnlockDuration.UntilScreenOff) + FIXED_UNLOCK_DURATION_MINUTES.map { UnlockDuration.Fixed(it) }

private const val PREF_VALUE_UNTIL_SCREEN_OFF = "until_screen_off"

fun UnlockDuration.toPrefValue(): String = when (this) {
    is UnlockDuration.Fixed -> minutes.toString()
    UnlockDuration.UntilScreenOff -> PREF_VALUE_UNTIL_SCREEN_OFF
}

fun unlockDurationFromPrefValue(value: String?): UnlockDuration = when {
    value == null || value == PREF_VALUE_UNTIL_SCREEN_OFF -> UnlockDuration.UntilScreenOff
    else -> value.toIntOrNull()?.let { UnlockDuration.Fixed(it) } ?: UnlockDuration.UntilScreenOff
}
