package de.mm20.launcher2.ui.desktop

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.applications.AppRepository
import de.mm20.launcher2.globalactions.GlobalActionsService
import de.mm20.launcher2.icons.IconService
import de.mm20.launcher2.icons.LauncherIcon
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.search.SavableSearchable
import de.mm20.launcher2.searchable.PinnedLevel
import de.mm20.launcher2.services.favorites.FavoritesService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DesktopVM : ViewModel(), KoinComponent {
    private val appRepository: AppRepository by inject()
    private val favoritesService: FavoritesService by inject()
    private val iconService: IconService by inject()
    val globalActions: GlobalActionsService by inject()

    /** Every launchable app, sorted by name. */
    val allApps: StateFlow<List<Application>> = appRepository.findMany()
        .map { list -> list.sortedBy { (it.labelOverride ?: it.label).lowercase() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Apps pinned to the desktop. */
    val desktopApps: StateFlow<List<Application>> = favoritesService
        .getFavorites(minPinnedLevel = PinnedLevel.AutomaticallySorted)
        .map { list -> list.filterIsInstance<Application>() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _running = MutableStateFlow<List<Application>>(emptyList())

    /** Recently used apps, most recent first (not a live-process list). */
    val running: StateFlow<List<Application>> = _running

    private val launchedFromDesktop = ArrayList<String>()
    private val launchedLock = Any()

    fun iconFlow(app: SavableSearchable, sizePx: Int): Flow<LauncherIcon?> =
        iconService.getIcon(app, sizePx)

    fun addToDesktop(app: Application) = favoritesService.pinItem(app)
    fun removeFromDesktop(app: Application) = favoritesService.unpinItem(app)

    fun isOnDesktop(app: Application): Boolean =
        desktopApps.value.any { it.key == app.key }

    fun noteLaunched(app: Application) {
        val pkg = app.componentName.packageName
        synchronized(launchedLock) {
            launchedFromDesktop.remove(pkg)
            launchedFromDesktop.add(0, pkg)
            while (launchedFromDesktop.size > 24) launchedFromDesktop.removeAt(launchedFromDesktop.lastIndex)
        }
        favoritesService.reportLaunch(app)
    }

    fun hasUsageAccess(context: Context): Boolean {
        return try {
            val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Updates a *recently used* apps strip. Android does not expose a reliable list of all
     * live processes to ordinary launchers, so this deliberately does not claim these apps
     * are currently running. UsageStats queries and list processing run off the main thread.
     */
    suspend fun refreshRunning(context: Context) {
        val snapshot = allApps.value
        if (snapshot.isEmpty()) return

        val result = withContext(Dispatchers.IO) {
            try {
                val appByPackage = snapshot
                    .asSequence()
                    .filter { it.user == Process.myUserHandle() }
                    .associateBy { it.componentName.packageName }
                val packages = LinkedHashSet<String>()

                if (hasUsageAccess(context)) {
                    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                    val now = System.currentTimeMillis()
                    usm.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        now - 12 * 60 * 60 * 1000L,
                        now
                    )
                        .asSequence()
                        .filter { it.lastTimeUsed > now - 3 * 60 * 60 * 1000L }
                        .sortedByDescending { it.lastTimeUsed }
                        .forEach { packages.add(it.packageName) }
                }

                synchronized(launchedLock) {
                    launchedFromDesktop.forEach { packages.add(it) }
                }

                packages.asSequence()
                    .filter { it != context.packageName }
                    .mapNotNull { appByPackage[it] }
                    .take(12)
                    .toList()
            } catch (e: Exception) {
                Log.w("DesktopVM", "Unable to refresh recently used applications", e)
                emptyList()
            }
        }

        if (result.map { it.key } != _running.value.map { it.key }) {
            _running.value = result
        }
    }

}
