package dev.linkinxp.termlauncher

import dev.linkinxp.termlauncher.gestures.AdminLock
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import dev.linkinxp.termlauncher.wall.WallMode
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import dev.linkinxp.termlauncher.search.searchUri
import dev.linkinxp.termlauncher.sys.bootWindowOpen
import dev.linkinxp.termlauncher.ui.isReduceMotion
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.linkinxp.termlauncher.apps.DrawerState
import dev.linkinxp.termlauncher.fx.FxSettings
import dev.linkinxp.termlauncher.panel.PanelData
import dev.linkinxp.termlauncher.panel.PanelSettings
import dev.linkinxp.termlauncher.weather.WxSettings
import dev.linkinxp.termlauncher.importer.FavRow
import dev.linkinxp.termlauncher.importer.FavoritesReader
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.WidgetSize
import dev.linkinxp.termlauncher.ui.AppDrawer
import dev.linkinxp.termlauncher.ui.AppMenu
import dev.linkinxp.termlauncher.ui.EditMode
import dev.linkinxp.termlauncher.ui.FolderPanel
import dev.linkinxp.termlauncher.ui.BootLogOverlay
import dev.linkinxp.termlauncher.ui.EffectsOverlay
import dev.linkinxp.termlauncher.ui.MatrixRain
import dev.linkinxp.termlauncher.ui.WakeUpOverlay
import dev.linkinxp.termlauncher.ui.rememberReduceMotion
import dev.linkinxp.termlauncher.ui.ImportDialog
import dev.linkinxp.termlauncher.gestures.Features
import dev.linkinxp.termlauncher.gestures.ScreenLock
import dev.linkinxp.termlauncher.gestures.SwipeDownAction
import dev.linkinxp.termlauncher.sys.expandNotificationShade
import dev.linkinxp.termlauncher.sys.expandQuickSettings
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.drawerDragWatcher
import dev.linkinxp.termlauncher.ui.SettingsScreen
import dev.linkinxp.termlauncher.ui.TermTheme

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.onCalendarPermission(granted)
        vm.refreshExtras()
    }

    private val homeRoleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    private fun requestHomeRole() = homeRoleLauncher.launch(dev.linkinxp.termlauncher.home.homeRoleIntent(this))

    private val bindLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) afterBind() else cancelPendingWidget()
    }

    /** Edit mode picked [provider] for [page]: bind it (asking the user if needed), configure it, then add it. */
    private fun beginAddWidget(page: Int, provider: String) {
        cancelPendingWidget() // never leave an earlier allocation behind
        val id = vm.widgets.allocate()
        vm.pendingWidget = PendingWidget(page, provider, id)
        if (vm.widgets.bindIfAllowed(id, provider)) afterBind() else bindLauncher.launch(vm.widgets.bindIntent(id, provider))
    }

    private fun afterBind() {
        val p = vm.pendingWidget ?: return
        if (vm.widgets.needsConfigure(p.provider)) {
            if (!vm.widgets.configure(this, p.id, REQ_CONFIGURE)) cancelPendingWidget()
        } else {
            finishAddWidget()
        }
    }

    private fun finishAddWidget() {
        val p = vm.pendingWidget ?: return
        vm.pendingWidget = null
        vm.addWidgetOrDelete(p.page, Item.Widget(p.id, p.provider, WidgetSize.MEDIUM))
    }

    private fun cancelPendingWidget() {
        val p = vm.pendingWidget ?: return
        vm.pendingWidget = null
        vm.widgets.delete(p.id)
        val label = vm.widgets.providers().firstOrNull { it.provider == p.provider }?.label ?: "widget"
        vm.showMessage("widget: ${label.lowercase()}: not added")
    }

    @Deprecated("Widget configure screens report back through the classic result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CONFIGURE) {
            if (resultCode == RESULT_OK) finishAddWidget() else cancelPendingWidget()
        }
    }

    override fun onStart() {
        super.onStart()
        vm.widgets.start()
    }

    override fun onStop() {
        vm.widgets.stop()
        super.onStop()
    }

    private companion object {
        val EMPTY_DRAWER = DrawerState(emptyList(), emptyList())
        const val DRAWER_WARMUP_MS = 1_500L
        const val REQ_CONFIGURE = 4711
        const val MAX_IMPORT_BYTES = 5 * 1024 * 1024
        const val BACKUP_FILE = "terminal-launcher-backup.json"
    }

    /** Hands a typed query to the system (browser or the Google app) as a Google search URL. */
    private fun openSearch(text: String) {
        val uri = searchUri(text) ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            vm.showMessage("search: no browser found")
        }
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) lifecycleScope.launch { writeBackup(uri) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch { vm.startImport(readText(uri)) }
    }

    private val launcherImportLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lifecycleScope.launch { vm.startLauncherImport(readLauncherBackup(uri)) }
    }

    private suspend fun readLauncherBackup(uri: Uri): Result<List<FavRow>> = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use { FavoritesReader.read(it, cacheDir) }
                ?: Result.failure(java.io.IOException("import: could not read that file"))
        } catch (_: Exception) {
            Result.failure(java.io.IOException("import: could not read that file"))
        }
    }

    private suspend fun writeBackup(uri: Uri) {
        val text = vm.exportJson()
        val ok = withContext(Dispatchers.IO) {
            try {
                contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
            } catch (_: Exception) { false }
        }
        vm.showMessage(if (ok) "backup: saved" else "backup: could not save")
    }

    /** The picked file as text, or null when it cannot be read or is larger than 5 MB. */
    private suspend fun readText(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use { s ->
                val bytes = s.readNBytes(MAX_IMPORT_BYTES + 1)
                if (bytes.size > MAX_IMPORT_BYTES) null else String(bytes, Charsets.UTF_8)
            }
        } catch (_: Exception) { null }
    }

    private val settingsOpen = mutableStateOf(false)
    private val homePresses = mutableIntStateOf(0)

    /** Pressing Home (button or gesture) while the launcher is open goes back to the first home page. */
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            vm.closeDrawer()
            vm.cancelImport()
            settingsOpen.value = false
            homePresses.intValue++
        }
    }

    private var unlockAt = 0L

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            unlockAt = SystemClock.elapsedRealtime()
            maybeBoot()
        }
    }

    /** Plays the boot log only if the launcher is on screen right after an unlock (never a stale one later). */
    private fun maybeBoot() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        if (bootWindowOpen(SystemClock.elapsedRealtime(), unlockAt) && vm.ui.value.fx.boot && !isReduceMotion(this)) {
            unlockAt = 0
            vm.startBoot()
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshPanels()
        maybeBoot() // unlock broadcast may arrive before the activity is resumed
    }

    override fun onPause() {
        vm.stopMatrix() // overlays never outlive the screen
        vm.endBoot()
        super.onPause()
    }

    override fun onDestroy() {
        unregisterReceiver(unlockReceiver)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch { vm.permissionRequests.collect { permissionLauncher.launch(it) } }
        lifecycleScope.launch { vm.settingsRequests.collect { settingsOpen.value = true } }
        ContextCompat.registerReceiver(
            this, unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        setContent {
            val uiState = vm.ui.collectAsState()
            // The root reads only these narrow derived values (each changes only when its own part changes), so a
            // change elsewhere in the UI state (a keystroke, a panel refresh, the drawer flipping) leaves it alone.
            val theme by remember { derivedStateOf { uiState.value.theme } }
            val wall by remember { derivedStateOf { uiState.value.wall } }
            val fx by remember { derivedStateOf { uiState.value.fx } }
            val showMatrix by remember { derivedStateOf { uiState.value.matrix } }
            val showWake by remember { derivedStateOf { uiState.value.wake } }
            val showBoot by remember { derivedStateOf { uiState.value.boot } }
            // What the home screen shows: the state minus the drawer/filter parts (equal when only those change).
            val homeUi by remember {
                derivedStateOf { uiState.value.copy(drawerOpen = false, showHidden = false, filter = "", drawer = EMPTY_DRAWER) }
            }
            // The drawer only needs its own slice (not the prompt text, panels, effects...).
            val drawerUi by remember {
                derivedStateOf {
                    uiState.value.copy(
                        prompt = PromptState(), historyAll = emptyList(), calendar = PanelData.Off, weather = PanelData.Off,
                        matrix = false, wake = false, boot = false, fx = FxSettings(), quick = emptyList(), panels = PanelSettings(),
                        wx = WxSettings(), drawerOpen = false,
                    )
                }
            }
            // Compose the drawer shortly after start-up (while idle) so the first open does not pay for it.
            var drawerWarm by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(DRAWER_WARMUP_MS); drawerWarm = true }
            val drawerComposed by remember { derivedStateOf { drawerWarm || uiState.value.drawerOpen } }
            var menuPkg by remember { mutableStateOf<String?>(null) }
            var folderName by remember { mutableStateOf<String?>(null) }
            var editing by remember { mutableStateOf(false) }
            var askHome by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                if (dev.linkinxp.termlauncher.home.shouldAskDefaultHome(dev.linkinxp.termlauncher.home.isDefaultHome(this@MainActivity), vm.defaultHomeAsked())) askHome = true
            }
            if (askHome) {
                dev.linkinxp.termlauncher.home.DefaultHomeDialog(
                    theme,
                    onYes = { askHome = false; vm.markDefaultHomeAsked(); requestHomeRole() },
                    onLater = { askHome = false; vm.markDefaultHomeAsked() },
                )
            }
            val homeSignal = homePresses.intValue
            LaunchedEffect(homeSignal) {
                if (homeSignal > 0) { menuPkg = null; folderName = null; editing = false }
            }
            // The system wallpaper is only composited behind the window in SYSTEM mode (saves power otherwise).
            LaunchedEffect(wall.mode) {
                window.setFlags(
                    if (wall.mode == WallMode.SYSTEM) WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER else 0,
                    WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
                )
            }
            BackHandler { /* a launcher stays home */ }
            val drag = remember { dev.linkinxp.termlauncher.ui.DragState() }
            TermTheme(theme, wall) {
                val tap = { item: Item ->
                    when (item) {
                        is Item.App -> vm.launch(item.pkg)
                        is Item.Folder -> folderName = item.name
                        is Item.Widget, is Item.Panel, is Item.Gap -> Unit
                    }
                }
                Box(Modifier.fillMaxSize().drawerDragWatcher(drag, onLeaveDrawer = vm::closeDrawer, onMenu = { menuPkg = it })) {
                PagerHome(
                    homeUi, vm,
                    onItemTap = tap,
                    onItemLongPress = { item ->
                        when (item) {
                            is Item.App -> menuPkg = item.pkg
                            is Item.Folder -> folderName = item.name
                            is Item.Widget, is Item.Panel, is Item.Gap -> Unit
                        }
                    },
                    onSwipeUp = { vm.openDrawer() },
                    onMoveItem = vm::placeItem,
                    onMoveToPage = vm::moveItemToPage,
                    onDropOnDock = vm::moveItemToDock,
                    drag = drag,
                    onAddAt = vm::addAt,
                    onPinNew = vm::pin,
                    onMerge = vm::mergeOnto,
                    onMoveDock = vm::moveInDock,
                    imeSkip = { uiState.value.drawerOpen },
                    onSwipeDown = { action ->
                        if (action == SwipeDownAction.QUICK_SETTINGS) expandQuickSettings(this@MainActivity) else expandNotificationShade(this@MainActivity)
                    },
                    onDoubleTap = {
                        if (uiState.value.doubleTapLock && !AdminLock.lock(this@MainActivity) && !(Features.lockService && ScreenLock.lock())) {
                            vm.showMessage("double-tap lock: turn on lock access in settings > gestures")
                        }
                    },
                    onEdit = { editing = true },
                    homeSignal = homeSignal,
                    onSearch = ::openSearch,
                    onSettings = { settingsOpen.value = true },
                    // Kept composed but invisible under the drawer so the wall shows and the page is kept.
                    // A permanent layer whose alpha is read at draw time: opening the drawer neither recomposes nor re-records home.
                    modifier = Modifier.graphicsLayer { alpha = if (uiState.value.drawerOpen) 0f else 1f },
                )
                if (drawerComposed) {
                    AppDrawer(
                        drawerUi, vm,
                        open = { uiState.value.drawerOpen },
                        onAppLongPress = { menuPkg = it },
                        onFolderTap = { folderName = it },
                        onSettings = { vm.closeDrawer(); settingsOpen.value = true },
                        onWebSearch = ::openSearch,
                        drag = drag,
                    )
                }
                }
                menuPkg?.let { AppMenu(it, uiState.value, vm) { menuPkg = null } }
                folderName?.let { FolderPanel(it, uiState.value, vm) { folderName = null } }
                if (settingsOpen.value) {
                    SettingsScreen(
                        uiState.value, vm,
                        onExport = { exportLauncher.launch(BACKUP_FILE) },
                        onImport = { importLauncher.launch(arrayOf("*/*")) },
                        onImportLauncher = { launcherImportLauncher.launch(arrayOf("*/*")) },
                        onEditPages = { settingsOpen.value = false; editing = true },
                        onMakeDefault = ::requestHomeRole,
                        onClose = { settingsOpen.value = false },
                    )
                }
                val preview by vm.importPreview.collectAsState()
                preview?.let { ImportDialog(theme, it, onReplace = vm::confirmImport, onCancel = vm::cancelImport) }
                if (editing) {
                    EditMode(
                        uiState.value, vm,
                        onAddWidget = ::beginAddWidget,
                        onOpenSettings = { editing = false; settingsOpen.value = true },
                    ) { editing = false }
                }
                EffectsOverlay(fx, theme, rememberReduceMotion())
                if (showMatrix) MatrixRain(onDone = vm::stopMatrix)
                if (showBoot) BootLogOverlay(onDone = vm::endBoot)
                if (showWake) WakeUpOverlay(onDone = vm::stopWake)
            }
        }
    }
}
