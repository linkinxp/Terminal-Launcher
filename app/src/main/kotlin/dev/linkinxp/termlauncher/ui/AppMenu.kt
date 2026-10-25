package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.layout.HomeStyle
import dev.linkinxp.termlauncher.layout.Item

@Composable
private fun MenuLine(text: String, onClick: () -> Unit) {
    TermText(text, Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp))
}

/** Long-press menu for an app: pin, add to folder, hide, app info, uninstall. */
@Composable
fun AppMenu(pkg: String, ui: LauncherUi, vm: LauncherViewModel, onDismiss: () -> Unit) {
    val app = ui.apps[pkg] ?: return
    var pickingFolder by remember { mutableStateOf(false) }
    val pinned = ui.layout.dock.any { it == Item.App(pkg) }
    val onHome = ui.layout.pages.any { p -> p.items.contains(Item.App(pkg)) }
    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.padding(8.dp)) {
            DialogSurface(ui.theme) {
                TermPanel(app.label.lowercase(), Modifier.fillMaxWidth().padding(8.dp)) {
                    if (ui.homeStyle == HomeStyle.ICONS) IconTile(pkg, app.label, 40.dp, Modifier.padding(bottom = 6.dp))
                    if (pickingFolder) {
                        val folders = ui.layout.folders()
                        if (folders.isEmpty()) TermText("no folders: mkdir <name>", dim = true)
                        folders.forEach { f ->
                            MenuLine("~/${f.name}") { vm.mv(pkg, f.name); onDismiss() }
                        }
                    } else {
                        MenuLine(if (pinned) "unpin from dock" else "pin to dock") {
                            if (pinned) vm.unpin(pkg) else vm.pin(pkg); onDismiss()
                        }
                        if (onHome) MenuLine("remove from home") { vm.removeShortcut(pkg); onDismiss() }
                        MenuLine("add to folder") { pickingFolder = true }
                        MenuLine(if (pkg in ui.hidden) "unhide" else "hide") { vm.toggleHidden(pkg); onDismiss() }
                        MenuLine("app info") { vm.appInfo(pkg); onDismiss() }
                        MenuLine("uninstall") { vm.uninstall(pkg); onDismiss() }
                    }
                }
            }
        }
    }
}
