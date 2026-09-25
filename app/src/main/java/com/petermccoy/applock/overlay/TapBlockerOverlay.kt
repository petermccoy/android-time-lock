package com.petermccoy.applock.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager

/**
 * A raw always-on-top touch-blocking view, added directly via [WindowManager] rather than
 * through an Activity launch. It exists purely to cover the locked app the instant it's
 * detected, buying time until [com.petermccoy.applock.ui.LockOverlayActivity] - the actual
 * auth gate - has launched and gained focus; an Activity transition alone leaves a window
 * where a tap can still land on the app underneath.
 *
 * Requires SYSTEM_ALERT_WINDOW. Silently no-ops if that permission hasn't been granted, so
 * the rest of the lock flow (and its existing, narrower race window) is unaffected either way.
 */
object TapBlockerOverlay {

    /** Safety net in case [hide] is never called for some reason, so this can never get stuck onscreen. */
    private const val AUTO_HIDE_DELAY_MS = 3_000L

    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var blockerView: View? = null
    private val autoHideRunnable = Runnable { hide() }

    fun show(context: Context) {
        if (blockerView != null) return
        if (!Settings.canDrawOverlays(context)) return

        val appContext = context.applicationContext
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = View(appContext).apply { setBackgroundColor(Color.BLACK) }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE,
        )

        try {
            wm.addView(view, params)
        } catch (e: WindowManager.BadTokenException) {
            return
        } catch (e: SecurityException) {
            return
        }

        windowManager = wm
        blockerView = view
        handler.removeCallbacks(autoHideRunnable)
        handler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY_MS)
    }

    fun hide() {
        handler.removeCallbacks(autoHideRunnable)
        val wm = windowManager
        val view = blockerView
        windowManager = null
        blockerView = null
        if (wm != null && view != null) {
            try {
                wm.removeView(view)
            } catch (e: IllegalArgumentException) {
                // Already detached; nothing to do.
            }
        }
    }
}
