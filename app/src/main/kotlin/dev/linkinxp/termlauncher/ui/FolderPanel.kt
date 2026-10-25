package dev.linkinxp.termlauncher.ui

import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.layout.Item

/** A folder as a bordered `~/name` panel: tap an app to launch, long-press to take it out, manage the folder below. */
@Composable
fun FolderPanel(name: String, ui: LauncherUi, vm: LauncherViewModel, onDismiss: () -> Unit) {
    val folder = ui.layout.folders().firstOrNull { it.name == name }
    LaunchedEffect(folder == null) { if (folder == null) onDismiss() }
    if (folder == null) return
    var selected by remember { mutableStateOf<String?>(null) }
    val pinned = ui.layout.dock.any { it is Item.Folder && it.name == name }
    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.padding(8.dp)) {
            DialogSurface(ui.theme) {
                TermPanel("~/${folder.name}", Modifier.fillMaxWidth().padding(8.dp)) {
                    val apps = folder.apps.mapNotNull { ui.apps[it] }
                    if (apps.isEmpty()) TermText("empty: mv <app> ${folder.name}", dim = true)
                    apps.forEach { app ->
                        AppRow(
                            app, ui.iconMode,
                            onClick = { vm.launch(app.pkg); onDismiss() },
                            onLongClick = { selected = app.pkg },
                        )
                        if (selected == app.pkg) {
                            TermText(
                                "  remove ${app.label.lowercase()} from folder?",
                                Modifier.fillMaxWidth().clickable { vm.unfile(app.pkg); selected = null }.padding(vertical = 6.dp),
                                color = ui.theme.accent.color(),
                            )
                        }
                    }
                    var newName by remember(name) { mutableStateOf("") }
                    val rename = { if (newName.isNotBlank()) { vm.renameFolder(name, newName.trim()); onDismiss() } }
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TermText("rename ", dim = true)
                        BasicTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            singleLine = true,
                            textStyle = TextStyle(color = ui.theme.foreground.color(), fontFamily = TermFont, fontSize = 14.sp),
                            cursorBrush = SolidColor(ui.theme.accent.color()),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { rename() }),
                            decorationBox = { inner -> Box { if (newName.isEmpty()) TermText(folder.name, dim = true); inner() } },
                            modifier = Modifier.weight(1f).testTag("folder-rename"),
                        )
                        TapText("[ok]", { rename() }, Modifier.testTag("folder-rename-ok"), bold = true)
                    }
                    TermText(
                        if (pinned) "unpin from dock" else "pin to dock",
                        Modifier.fillMaxWidth().clickable { if (pinned) vm.unpinFolder(name) else vm.pinFolder(name); onDismiss() }.padding(top = 14.dp, bottom = 6.dp),
                        dim = true,
                    )
                    TermText(
                        "rmdir ~/${folder.name}",
                        Modifier.fillMaxWidth().clickable { vm.rmdir(name) }.padding(vertical = 6.dp),
                        dim = true,
                    )
                }
            }
        }
    }
}
