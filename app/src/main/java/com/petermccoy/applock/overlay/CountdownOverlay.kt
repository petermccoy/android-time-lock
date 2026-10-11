package com.petermccoy.applock.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.petermccoy.applock.session.SessionState
import java.util.Locale

/**
 * A small always-on-top ticking countdown, shown while a locked app is unlocked for a fixed
 * duration, so the remaining grace period is visible while actually using the app instead of
 * only discoverable from inside AppLock's own screen. Never shown for "until screen off"
 * unlocks, which have no fixed remaining time to display.
 *
 * Requires SYSTEM_ALERT_WINDOW (shared with [TapBlockerOverlay]); silently no-ops without it.
 */
object CountdownOverlay {

    private const val TICK_INTERVAL_MS = 1_000L

    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var textView: TextView? = null
    private var trackedPackage: String? = null

    private val tickRunnable = object : Runnable {
        override fun run() {
            val packageName = trackedPackage ?: return
            val remainingMs = SessionState.remainingMillis(packageName)
            if (remainingMs == null) {
                hide()
                return
            }
            updateText(remainingMs)
            handler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    /** Shows (or re-targets) the countdown for [packageName]; no-ops if it has no fixed remaining time. */
    fun show(context: Context, packageName: String) {
        if (trackedPackage == packageName) return

        val remainingMs = SessionState.remainingMillis(packageName)
        if (remainingMs == null) {
            hide()
            return
        }
        if (!Settings.canDrawOverlays(context)) return

        hide()

        val appContext = context.applicationContext
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = appContext.resources.displayMetrics.density
        val view = TextView(appContext).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(160, 0, 0, 0))
            textSize = 14f
            setPadding((12 * density).toInt(), (6 * density).toInt(), (12 * density).toInt(), (6 * density).toInt())
        }

        val margin = (8 * density).toInt()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = margin
            y = margin
        }

        try {
            wm.addView(view, params)
        } catch (e: WindowManager.BadTokenException) {
            return
        } catch (e: SecurityException) {
            return
        }

        windowManager = wm
        textView = view
        trackedPackage = packageName
        updateText(remainingMs)
        handler.postDelayed(tickRunnable, TICK_INTERVAL_MS)
    }

    fun hide() {
        handler.removeCallbacks(tickRunnable)
        trackedPackage = null
        val wm = windowManager
        val view = textView
        windowManager = null
        textView = null
        if (wm != null && view != null) {
            try {
                wm.removeView(view)
            } catch (e: IllegalArgumentException) {
                // Already detached; nothing to do.
            }
        }
    }

    private fun updateText(remainingMs: Long) {
        val totalSeconds = remainingMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        textView?.text = String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
