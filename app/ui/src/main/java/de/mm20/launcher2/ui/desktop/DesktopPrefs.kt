package de.mm20.launcher2.ui.desktop

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Settings for the Linux-style desktop mode. Kept in a plain SharedPreferences file so the
 * desktop feature is fully self-contained and does not touch the launcher's own settings store.
 */
object DesktopPrefs {
    private const val FILE = "desktop_mode"

    /** Desktop mode on/off (off = classic Kvaesitso home screen). */
    var enabled by mutableStateOf(true)
        private set

    /** Floating (rounded, 80% width) taskbar instead of a full-width bar. */
    var floatingTaskbar by mutableStateOf(true)
        private set

    /** Desktop icon size in dp. */
    var iconSizeDp by mutableIntStateOf(56)
        private set

    /** Component names (flattened) pinned to the taskbar, in order. */
    var taskbarPinned by mutableStateOf<List<String>>(emptyList())
        private set

    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        enabled = p.getBoolean("enabled", false)
        floatingTaskbar = p.getBoolean("floating_taskbar", true)
        iconSizeDp = p.getInt("icon_size", 56)
        taskbarPinned = (p.getString("taskbar_pinned", "") ?: "")
            .split("|").filter { it.isNotBlank() }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun setEnabled(context: Context, value: Boolean) {
        enabled = value
        prefs(context).edit().putBoolean("enabled", value).apply()
    }

    fun setFloatingTaskbar(context: Context, value: Boolean) {
        floatingTaskbar = value
        prefs(context).edit().putBoolean("floating_taskbar", value).apply()
    }

    fun setIconSize(context: Context, value: Int) {
        iconSizeDp = value
        prefs(context).edit().putInt("icon_size", value).apply()
    }

    fun toggleTaskbarPinned(context: Context, flatComponent: String) {
        taskbarPinned = if (taskbarPinned.contains(flatComponent))
            taskbarPinned - flatComponent
        else
            taskbarPinned + flatComponent
        prefs(context).edit().putString("taskbar_pinned", taskbarPinned.joinToString("|")).apply()
    }
}
