package de.mm20.launcher2.ui.desktop

import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

/** Entry points for the optional browser, Linux runtime and game-control companion modules. */
object DesktopCompanionFeatures {
    private const val PREFS = "desktop_companion"
    private const val PROFILES = "game_profiles"

    fun openDesktopBrowser(context: Context, url: String = "https://www.youtube.com") {
        val intent = Intent().setClassName(
            context.packageName,
            "de.mm20.launcher2.activity.DesktopBrowserActivity"
        ).putExtra("desktop_browser_url", url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { Toast.makeText(context, "Desktop browser could not be opened", Toast.LENGTH_SHORT).show() }
    }

    /** Launches an already-installed Termux environment; no Linux root or bundled distro is assumed. */
    fun openLinuxRuntime(context: Context) {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage("com.termux")
            ?: pm.getLaunchIntentForPackage("com.termux.x11")
        if (intent == null) {
            Toast.makeText(context, "Install Termux and a compatible Linux desktop runtime first", Toast.LENGTH_LONG).show()
            return
        }
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Toast.makeText(context, "Linux runtime could not be started", Toast.LENGTH_SHORT).show() }
    }

    /** Stores editable per-game key labels/actions. Actual touch injection requires a separate
     * user-enabled accessibility/overlay service and is intentionally not silently enabled. */
    fun saveGameBinding(context: Context, packageName: String, key: String, action: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val root = runCatching { JSONObject(prefs.getString(PROFILES, "{}") ?: "{}") }.getOrDefault(JSONObject())
        val profile = root.optJSONObject(packageName) ?: JSONObject().also { root.put(packageName, it) }
        profile.put(key, action)
        prefs.edit().putString(PROFILES, root.toString()).apply()
    }

    fun getGameBindings(context: Context, packageName: String): Map<String, String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PROFILES, "{}") ?: "{}"
        val profile = runCatching { JSONObject(raw).optJSONObject(packageName) }.getOrNull() ?: return emptyMap()
        return profile.keys().asSequence().associateWith { profile.optString(it) }
    }

    fun removeGameProfile(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val root = runCatching { JSONObject(prefs.getString(PROFILES, "{}") ?: "{}") }.getOrDefault(JSONObject())
        root.remove(packageName)
        prefs.edit().putString(PROFILES, root.toString()).apply()
    }
}
