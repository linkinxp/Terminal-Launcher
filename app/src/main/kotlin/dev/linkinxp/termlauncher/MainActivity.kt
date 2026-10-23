package dev.linkinxp.termlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.view.WindowManager
import androidx.compose.runtime.LaunchedEffect
import dev.linkinxp.termlauncher.wall.WallMode
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import dev.linkinxp.termlauncher.sys.bootWindowOpen
import dev.linkinxp.termlauncher.ui.isReduceMotion
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.ui.AppDrawer
import dev.linkinxp.termlauncher.ui.AppMenu
import dev.linkinxp.termlauncher.ui.EditMode
import dev.linkinxp.termlauncher.ui.FolderPanel
import dev.linkinxp.termlauncher.ui.BootLogOverlay
import dev.linkinxp.termlauncher.ui.EffectsOverlay
import dev.linkinxp.termlauncher.ui.MatrixRain
import dev.linkinxp.termlauncher.ui.rememberReduceMotion
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.TermTheme

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        vm.onCalendarPermission(granted)
    }

    private val homePresses = mutableIntStateOf(0)

    /** Pressing Home (button or gesture) while the launcher is open goes back to the first home page. */
    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            vm.closeDrawer()
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
        ContextCompat.registerReceiver(
            this, unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        setContent {
            val ui by vm.ui.collectAsState()
            var menuPkg by remember { mutableStateOf<String?>(null) }
            var folderName by remember { mutableStateOf<String?>(null) }
            var editing by remember { mutableStateOf(false) }
            val homeSignal = homePresses.intValue
            LaunchedEffect(homeSignal) {
                if (homeSignal > 0) { menuPkg = null; folderName = null; editing = false }
            }
            // The system wallpaper is only composited behind the window in SYSTEM mode (saves power otherwise).
            LaunchedEffect(ui.wall.mode) {
                window.setFlags(
                    if (ui.wall.mode == WallMode.SYSTEM) WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER else 0,
                    WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
                )
            }
            BackHandler { /* a launcher stays home */ }
            TermTheme(ui.theme, ui.wall) {
                val tap = { item: Item ->
                    when (item) {
                        is Item.App -> vm.launch(item.pkg)
                        is Item.Folder -> folderName = item.name
                        is Item.Widget -> Unit
                    }
                }
                PagerHome(
                    ui, vm,
                    onItemTap = tap,
                    onItemLongPress = { item ->
                        when (item) {
                            is Item.App -> menuPkg = item.pkg
                            is Item.Folder -> folderName = item.name
                            is Item.Widget -> Unit
                        }
                    },
                    onSwipeUp = { vm.openDrawer() },
                    onEdit = { editing = true },
                    homeSignal = homeSignal,
                    // Kept composed but invisible under the drawer so the wall shows and the page is kept.
                    modifier = Modifier.alpha(if (ui.drawerOpen) 0f else 1f),
                )
                if (ui.drawerOpen) {
                    AppDrawer(ui, vm, onAppLongPress = { menuPkg = it }, onFolderTap = { folderName = it })
                }
                menuPkg?.let { AppMenu(it, ui, vm) { menuPkg = null } }
                folderName?.let { FolderPanel(it, ui, vm) { folderName = null } }
                if (editing) EditMode(ui, vm) { editing = false }
                EffectsOverlay(ui.fx, ui.theme, rememberReduceMotion())
                if (ui.matrix) MatrixRain(onDone = vm::stopMatrix)
                if (ui.boot) BootLogOverlay(onDone = vm::endBoot)
            }
        }
    }
}
