package de.mm20.launcher2.ui.desktop

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * "Desktop boost" flips the same system switches as Developer options so that apps behave like
 * they do on a desktop: every app becomes resizable, freeform (floating) windows work, and apps
 * that normally refuse multi-window are forced into windows anyway.
 *
 * Writing these needs WRITE_SECURE_SETTINGS, which can only be granted once over ADB.
 */
object DesktopBoost {
    private const val PERMISSION = "android.permission.WRITE_SECURE_SETTINGS"

    private val keys = listOf(
        "enable_freeform_support",
        "force_resizable_activities",
        "enable_non_resizable_multi_window",
    )

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun adbCommand(context: Context): String =
        "adb shell pm grant ${context.packageName} $PERMISSION"

    /** True when freeform + forced resizing are on. */
    fun isOn(context: Context): Boolean = try {
        val cr = context.contentResolver
        Settings.Global.getInt(cr, "enable_freeform_support", 0) == 1 &&
                Settings.Global.getInt(cr, "force_resizable_activities", 0) == 1
    } catch (e: Exception) {
        false
    }

    /** Returns false when the permission is missing or the system refused. */
    fun setEnabled(context: Context, on: Boolean): Boolean {
        if (!hasPermission(context)) return false
        return try {
            val cr = context.contentResolver
            keys.forEach { Settings.Global.putInt(cr, it, if (on) 1 else 0) }
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
