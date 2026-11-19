package de.mm20.launcher2.ui.desktop

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Process
import android.provider.Settings
import de.mm20.launcher2.search.Application

/**
 * Launches apps in different window configurations.
 *
 * Android (not the launcher) owns windowing, so:
 *  - "half" tiles use freeform launch bounds. They need freeform windows enabled
 *    (Developer options -> "Enable freeform windows") or a device with desktop windowing.
 *  - split screen uses the system split-screen toggle through the accessibility service.
 */
object WindowLauncher {

    fun isFreeformCapable(context: Context): Boolean {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)) return true
        return try {
            Settings.Global.getInt(context.contentResolver, "enable_freeform_support", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    private var cascade = 0

    /** Opens [app] the way the user configured it: floating window (desktop style) or fullscreen. */
    fun launchDefault(context: Context, app: Application, bottomInsetPx: Int): Boolean {
        if (DesktopPrefs.defaultLaunch == "floating" && isFreeformCapable(context)) {
            if (launchFloating(context, app, bottomInsetPx)) return true
        }
        return launchNormal(context, app)
    }

    /** Launch [app] normally. */
    fun launchNormal(context: Context, app: Application): Boolean {
        return app.launch(context, ActivityOptions.makeBasic().toBundle())
    }

    /**
     * Launch [app] tiled on the left or right half of the screen (freeform window).
     * Returns false when freeform windows are not available.
     */
    fun launchHalf(context: Context, app: Application, left: Boolean, bottomInsetPx: Int): Boolean {
        if (!isFreeformCapable(context)) return false
        val dm = context.resources.displayMetrics
        val half = dm.widthPixels / 2
        val bottom = (dm.heightPixels - bottomInsetPx).coerceAtLeast(dm.heightPixels / 2)
        val bounds = if (left) Rect(0, 0, half, bottom) else Rect(half, 0, dm.widthPixels, bottom)
        return launchWithBounds(context, app, bounds)
    }

    /** Launch [app] in a floating window in the middle of the screen (freeform window). */
    fun launchFloating(context: Context, app: Application, bottomInsetPx: Int): Boolean {
        if (!isFreeformCapable(context)) return false
        val dm = context.resources.displayMetrics
        val w = (dm.widthPixels * 0.6f).toInt()
        val h = ((dm.heightPixels - bottomInsetPx) * 0.8f).toInt()
        // Each new window is offset a bit so windows cascade like on a desktop
        val step = (cascade % 6) * (40 * dm.density).toInt()
        cascade++
        val baseLeft = (dm.widthPixels - w) / 2 - 3 * (40 * dm.density).toInt() + step
        val baseTop = ((dm.heightPixels - bottomInsetPx) - h) / 2 - 3 * (40 * dm.density).toInt() + step
        val left = baseLeft.coerceIn(0, (dm.widthPixels - w).coerceAtLeast(0))
        val top = baseTop.coerceIn(0, (dm.heightPixels - bottomInsetPx - h).coerceAtLeast(0))
        return launchWithBounds(context, app, Rect(left, top, left + w, top + h))
    }

    private fun launchWithBounds(context: Context, app: Application, bounds: Rect): Boolean {
        val options = ActivityOptions.makeBasic().setLaunchBounds(bounds)
        if (app.user != Process.myUserHandle()) {
            // Work profile apps cannot be started with a plain intent
            return app.launch(context, options.toBundle())
        }
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.componentName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            context.startActivity(intent, options.toBundle())
            true
        } catch (e: Exception) {
            false
        }
    }
}
