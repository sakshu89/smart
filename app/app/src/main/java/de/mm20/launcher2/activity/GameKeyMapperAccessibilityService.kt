package de.mm20.launcher2.activity

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.Display
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import org.json.JSONObject

/**
 * Opt-in game input bridge. It only translates explicitly configured physical keys into
 * Android accessibility gestures. It does not inject arbitrary low-level input or bypass game
 * protections. Some games reject synthetic gestures; those games are unsupported.
 *
 * Profile action syntax (stored per package by DesktopCompanionFeatures):
 *   tap:nx:ny
 *   swipe:nx1:ny1:nx2:ny2:durationMs
 * Coordinates are normalized to [0,1]. Example: tap:0.82:0.78
 */
class GameKeyMapperAccessibilityService : AccessibilityService() {
    @Volatile private var foregroundPackage: String? = null
    private var lastKeyDown = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Service metadata requests key filtering; no permanent foreground polling is used.
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            foregroundPackage = event.packageName?.toString()
        }
    }

    override fun onInterrupt() { foregroundPackage = null }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || event.repeatCount != 0) return false
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastKeyDown < 28L) return false
        lastKeyDown = now
        val pkg = foregroundPackage ?: return false
        val prefs = getSharedPreferences("desktop_companion", MODE_PRIVATE)
        val raw = prefs.getString("game_profiles", "{}") ?: "{}"
        val action = runCatching { JSONObject(raw).optJSONObject(pkg)?.optString(KeyEvent.keyCodeToString(event.keyCode)) }.getOrNull()
            ?.takeIf { !it.isNullOrBlank() } ?: return false
        val gesture = parseGesture(action) ?: return false
        return dispatchGesture(gesture, null, null)
    }

    private fun parseGesture(spec: String): GestureDescription? {
        val p = spec.split(':')
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        fun coord(s: String, max: Float): Float = s.toFloat().coerceIn(0f, 1f) * max
        val path = Path()
        val duration: Long
        when (p.firstOrNull()) {
            "tap" -> {
                if (p.size != 3) return null
                val x = coord(p[1], width); val y = coord(p[2], height)
                path.moveTo(x, y); path.lineTo(x + 0.1f, y + 0.1f)
                duration = 55L
            }
            "swipe" -> {
                if (p.size != 6) return null
                path.moveTo(coord(p[1], width), coord(p[2], height))
                path.lineTo(coord(p[3], width), coord(p[4], height))
                duration = p[5].toLongOrNull()?.coerceIn(80L, 1200L) ?: return null
            }
            else -> return null
        }
        return GestureDescription.Builder().addStroke(
            GestureDescription.StrokeDescription(path, 0L, duration)
        ).build()
    }
}
