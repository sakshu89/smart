package de.mm20.launcher2.ui.desktop

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mm20.launcher2.icons.LauncherIcon
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.ui.component.ShapedLauncherIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

private val TaskbarHeight = 60.dp

/**
 * Linux / desktop style home screen: icons on the desktop, a taskbar with a start menu and the
 * running apps. This is a regular full screen launcher UI, not an overlay window.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopScreen(
    onOpenSettings: () -> Unit,
    onExitDesktopMode: () -> Unit,
    vm: DesktopVM = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val allApps by vm.allApps.collectAsState()
    val desktopApps by vm.desktopApps.collectAsState()
    val running by vm.running.collectAsState()

    var startOpen by remember { mutableStateOf(false) }
    val taskbarInsetPx = with(density) { (TaskbarHeight + 24.dp).roundToPx() }

    LaunchedEffect(allApps.size) {
        while (true) {
            vm.refreshRunning(context)
            delay(4000)
        }
    }

    val actions = remember(vm, context) {
        DesktopActions(context, vm, { taskbarInsetPx }, { startOpen = false }) { block ->
            scope.launch { block() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ---------- Desktop icons (fill top to bottom, then next column) ----------
        val iconSize = DesktopPrefs.iconSizeDp.dp
        LazyHorizontalGrid(
            rows = GridCells.Adaptive(minSize = iconSize + 40.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp, end = 12.dp, bottom = TaskbarHeight + 24.dp),
        ) {
            items(desktopApps, key = { it.key }) { app ->
                DesktopIcon(vm, actions, app, iconSize)
            }
        }

        // ---------- Start menu ----------
        if (startOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { startOpen = false }
            )
            StartMenu(
                vm = vm,
                actions = actions,
                apps = allApps,
                onOpenSettings = {
                    startOpen = false
                    onOpenSettings()
                },
                onExitDesktopMode = onExitDesktopMode,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 12.dp, bottom = TaskbarHeight + 20.dp)
            )
        }

        // ---------- Taskbar ----------
        Taskbar(
            vm = vm,
            actions = actions,
            allApps = allApps,
            running = running,
            startOpen = startOpen,
            onToggleStart = { startOpen = !startOpen },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(8.dp)
                .then(
                    if (DesktopPrefs.floatingTaskbar) Modifier.fillMaxWidth(0.8f)
                    else Modifier.fillMaxWidth()
                )
        )
    }
}

// =====================================================================================
// Actions
// =====================================================================================

private class DesktopActions(
    val context: Context,
    val vm: DesktopVM,
    val bottomInsetPx: () -> Int,
    val closeMenus: () -> Unit,
    val runAsync: (suspend () -> Unit) -> Unit,
) {
    private fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    fun open(app: Application) {
        closeMenus()
        if (WindowLauncher.launchNormal(context, app)) vm.noteLaunched(app)
    }

    fun openLeft(app: Application) {
        closeMenus()
        if (WindowLauncher.launchHalf(context, app, left = true, bottomInsetPx = bottomInsetPx()))
            vm.noteLaunched(app)
        else toast("Freeform windows are off. Enable them in Developer options.")
    }

    fun openRight(app: Application) {
        closeMenus()
        if (WindowLauncher.launchHalf(context, app, left = false, bottomInsetPx = bottomInsetPx()))
            vm.noteLaunched(app)
        else toast("Freeform windows are off. Enable them in Developer options.")
    }

    fun openFloating(app: Application) {
        closeMenus()
        if (WindowLauncher.launchFloating(context, app, bottomInsetPx()))
            vm.noteLaunched(app)
        else toast("Freeform windows are off. Enable them in Developer options.")
    }

    /** App A fullscreen -> system split screen -> launcher shows in the other half to pick app B. */
    fun openSplit(app: Application) {
        closeMenus()
        if (!vm.globalActions.isAccessibilityAvailable()) {
            toast("Turn on the launcher's accessibility service in Settings to use split screen.")
            return
        }
        if (!WindowLauncher.launchNormal(context, app)) return
        vm.noteLaunched(app)
        runAsync {
            delay(900)
            if (!vm.globalActions.toggleSplitScreen())
                toast("Split screen is not available for this app.")
            else
                toast("Now pick the second app.")
        }
    }

    fun appInfo(app: Application) {
        closeMenus()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${app.componentName.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
        }
    }
}

// =====================================================================================
// Pieces
// =====================================================================================

@Composable
private fun rememberAppIcon(vm: DesktopVM, app: Application, size: Dp): LauncherIcon? {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val flow = remember(app.key, px) { vm.iconFlow(app, px) }
    val icon by flow.collectAsState(initial = null)
    return icon
}

@Composable
private fun AppMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    app: Application,
    vm: DesktopVM,
    actions: DesktopActions,
) {
    val context = LocalContext.current
    val flat = app.componentName.flattenToString()
    val onDesktop = vm.desktopApps.collectAsState().value.any { it.key == app.key }
    val onTaskbar = DesktopPrefs.taskbarPinned.contains(flat)
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Open") }, onClick = { onDismiss(); actions.open(app) })
        DropdownMenuItem(text = { Text("Open on left half") }, onClick = { onDismiss(); actions.openLeft(app) })
        DropdownMenuItem(text = { Text("Open on right half") }, onClick = { onDismiss(); actions.openRight(app) })
        DropdownMenuItem(text = { Text("Open in floating window") }, onClick = { onDismiss(); actions.openFloating(app) })
        DropdownMenuItem(text = { Text("Open in split screen") }, onClick = { onDismiss(); actions.openSplit(app) })
        DropdownMenuItem(
            text = { Text(if (onDesktop) "Remove from desktop" else "Add to desktop") },
            onClick = {
                onDismiss()
                if (onDesktop) vm.removeFromDesktop(app) else vm.addToDesktop(app)
            })
        DropdownMenuItem(
            text = { Text(if (onTaskbar) "Unpin from taskbar" else "Pin to taskbar") },
            onClick = { onDismiss(); DesktopPrefs.toggleTaskbarPinned(context, flat) })
        DropdownMenuItem(text = { Text("App info") }, onClick = { onDismiss(); actions.appInfo(app) })
    }
}

private val ShadowStyle = TextStyle(
    color = Color.White,
    fontSize = 12.sp,
    textAlign = TextAlign.Center,
    shadow = Shadow(Color.Black.copy(alpha = 0.8f), Offset(0f, 1f), 6f),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopIcon(vm: DesktopVM, actions: DesktopActions, app: Application, iconSize: Dp) {
    val icon = rememberAppIcon(vm, app, iconSize)
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            modifier = Modifier
                .width(iconSize + 32.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = { actions.open(app) },
                    onLongClick = { menu = true },
                )
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShapedLauncherIcon(size = iconSize, icon = { icon })
            Spacer(Modifier.height(4.dp))
            Text(
                app.labelOverride ?: app.label,
                style = ShadowStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AppMenu(menu, { menu = false }, app, vm, actions)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Taskbar(
    vm: DesktopVM,
    actions: DesktopActions,
    allApps: List<Application>,
    running: List<Application>,
    startOpen: Boolean,
    onToggleStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pinnedFlat = DesktopPrefs.taskbarPinned
    val pinnedApps = pinnedFlat.mapNotNull { flat ->
        allApps.firstOrNull { it.componentName.flattenToString() == flat }
    }
    val runningPkgs = running.map { it.componentName.packageName }.toSet()
    val extraRunning = running.filter { r -> pinnedApps.none { it.key == r.key } }
    val taskbarApps = pinnedApps + extraRunning

    Surface(
        modifier = modifier.height(TaskbarHeight),
        shape = RoundedCornerShape(if (DesktopPrefs.floatingTaskbar) 28.dp else 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Start button
            val dotColor = MaterialTheme.colorScheme.onSurface
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (startOpen) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .clickable { onToggleStart() },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(22.dp)) {
                    val step = size.width / 3f
                    for (i in 0..2) for (j in 0..2) {
                        drawCircle(
                            dotColor,
                            radius = step * 0.26f,
                            center = Offset(step * (i + 0.5f), step * (j + 0.5f))
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))

            // Pinned + running apps. Scrolls instead of overlapping the clock on the right.
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(taskbarApps, key = { it.key }) { app ->
                    TaskbarItem(vm, actions, app, isRunning = runningPkgs.contains(app.componentName.packageName))
                }
            }

            // Clock
            val context = LocalContext.current
            var now by remember { mutableStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) {
                while (true) {
                    now = System.currentTimeMillis()
                    delay(10_000)
                }
            }
            val time = DateFormat.getTimeFormat(context).format(Date(now))
            val date = DateFormat.format("EEE, d MMM", Date(now)).toString()
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(time, style = MaterialTheme.typography.titleMedium)
                Text(date, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskbarItem(vm: DesktopVM, actions: DesktopActions, app: Application, isRunning: Boolean) {
    val icon = rememberAppIcon(vm, app, 36.dp)
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(onClick = { actions.open(app) }, onLongClick = { menu = true })
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShapedLauncherIcon(size = 36.dp, icon = { icon })
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .height(3.dp)
                    .width(if (isRunning) 14.dp else 0.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        AppMenu(menu, { menu = false }, app, vm, actions)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartMenu(
    vm: DesktopVM,
    actions: DesktopActions,
    apps: List<Application>,
    onOpenSettings: () -> Unit,
    onExitDesktopMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { (it.labelOverride ?: it.label).contains(query.trim(), ignoreCase = true) }
    }
    Surface(
        modifier = modifier
            .width(400.dp)
            .height(480.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search apps") },
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.key }) { app ->
                    var menu by remember { mutableStateOf(false) }
                    val icon = rememberAppIcon(vm, app, 40.dp)
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { actions.open(app) },
                                    onLongClick = { menu = true },
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ShapedLauncherIcon(size = 40.dp, icon = { icon })
                            Spacer(Modifier.width(12.dp))
                            Text(
                                app.labelOverride ?: app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        AppMenu(menu, { menu = false }, app, vm, actions)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onOpenSettings) { Text("Settings") }
                TextButton(onClick = {
                    vm.globalActions.openRecents()
                }) { Text("Recents") }
                TextButton(onClick = onExitDesktopMode) { Text("Classic") }
            }
            if (!vm.hasUsageAccess(context)) {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Allow usage access to show running apps") }
            }
        }
    }
}
