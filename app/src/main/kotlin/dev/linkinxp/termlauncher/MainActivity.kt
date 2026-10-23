package dev.linkinxp.termlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.TermTheme

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ui by vm.ui.collectAsState()
            BackHandler { /* a launcher stays home */ }
            TermTheme(ui.theme) {
                PagerHome(
                    ui, vm,
                    onItemTap = { item -> if (item is Item.App) vm.launch(item.pkg) },
                    onItemLongPress = { },
                )
            }
        }
    }
}
