

import android.app.Activity
import android.os.Build
import android.view.Display
import android.view.WindowManager

/** Refresh-rate policy for this app's own window. Android/OEM policy remains authoritative. */
object DesktopRefreshRateController {
    data class Capability(val supportedHz: List<Float>, val maxHz: Float, val minReportedHz: Float)

    fun capability(activity: Activity): Capability {
        val modes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.windowManager.defaultDisplay.supportedModes?.map { it.refreshRate } ?: emptyList()
        } else emptyList()
        val rates = modes.filter { it.isFinite() && it > 0f }.distinct().sorted()
        val fallback = activity.windowManager.defaultDisplay.refreshRate.takeIf { it > 0f } ?: 60f
        val actual = rates.ifEmpty { listOf(fallback) }
        return Capability(actual, actual.maxOrNull() ?: fallback, actual.minOrNull() ?: fallback)
    }

    /** Request the highest mode reported by the display for this window; not a system-wide override. */
    fun requestMaximumForWindow(activity: Activity): Capability {
        val capability = capability(activity)
        val params = activity.window.attributes
        params.preferredRefreshRate = capability.maxHz
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val preferred = activity.windowManager.defaultDisplay.supportedModes
                ?.filter { it.refreshRate <= capability.maxHz + 0.01f }
                ?.maxByOrNull { it.refreshRate }
            if (preferred != null) params.preferredDisplayModeId = preferred.modeId
        }
        activity.window.attributes = params
        return capability
    }

    /** Re-apply after resume/configuration changes; user battery/display settings can still win. */
    fun reapply(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) requestMaximumForWindow(activity)
    }
}
