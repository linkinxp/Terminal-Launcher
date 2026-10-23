package dev.linkinxp.termlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.ui.AppDrawer
import dev.linkinxp.termlauncher.ui.AppMenu
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.TermTheme

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ui by vm.ui.collectAsState()
            var menuPkg by remember { mutableStateOf<String?>(null) }
            BackHandler { /* a launcher stays home */ }
            TermTheme(ui.theme) {
                PagerHome(
                    ui, vm,
                    onItemTap = { item -> if (item is Item.App) vm.launch(item.pkg) },
                    onItemLongPress = { item -> if (item is Item.App) menuPkg = item.pkg },
                    onSwipeUp = { vm.openDrawer() },
                )
                if (ui.drawerOpen) AppDrawer(ui, vm, onAppLongPress = { menuPkg = it })
                menuPkg?.let { AppMenu(it, ui, vm) { menuPkg = null } }
            }
        }
    }
}
