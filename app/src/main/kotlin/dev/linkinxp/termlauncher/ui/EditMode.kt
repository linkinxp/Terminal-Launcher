package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item

private sealed interface Adding {
    data class App(val page: Int) : Adding
    data class Dir(val page: Int) : Adding
}

@Composable
private fun Action(text: String, onClick: () -> Unit) {
    TermText(text, Modifier.clickable(onClick = onClick).padding(end = 12.dp, top = 4.dp, bottom = 4.dp), bold = true)
}

/** Edit pages: add, remove and reorder pages; add an app shortcut or a folder to a page. */
@Composable
fun EditMode(ui: LauncherUi, vm: LauncherViewModel, onDismiss: () -> Unit) {
    val theme = ui.theme
    var adding by remember { mutableStateOf<Adding?>(null) }
    var newName by remember { mutableStateOf("") }
    val pages = ui.layout.pages
    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.padding(8.dp)) {
            DialogSurface(theme) {
                TermPanel("edit pages", Modifier.fillMaxWidth().padding(8.dp), trailing = "[done]") {
                    Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                        ui.prompt.message?.let { TermText(it, color = theme.accent.color(), maxLines = 3) }
                        pages.forEachIndexed { i, page ->
                            TermText("page ${i + 1} (${page.items.size} items)", Modifier.padding(top = 10.dp), color = theme.accent.color(), bold = true)
                            Row(horizontalArrangement = Arrangement.Start) {
                                if (i > 0) Action("[up]") { vm.movePage(i, i - 1) }
                                if (i < pages.lastIndex) Action("[down]") { vm.movePage(i, i + 1) }
                                if (pages.size > 1) Action("[rm]") { vm.removePage(i) }
                                Action("[+app]") { adding = Adding.App(i) }
                                Action("[+dir]") { adding = Adding.Dir(i); newName = "" }
                            }
                            when (val a = adding) {
                                is Adding.Dir -> if (a.page == i) {
                                    Row {
                                        TermText("mkdir ", dim = true)
                                        BasicTextField(
                                            value = newName,
                                            onValueChange = { newName = it },
                                            singleLine = true,
                                            textStyle = TextStyle(color = theme.foreground.color(), fontFamily = TermFont, fontSize = 14.sp),
                                            cursorBrush = SolidColor(theme.accent.color()),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { vm.mkdirOnPage(newName.trim(), i); adding = null }),
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                is Adding.App -> if (a.page == i) {
                                    ui.apps.values.sortedBy { it.label.lowercase() }.filter { it.pkg !in ui.hidden }.forEach { app ->
                                        TermText(
                                            "  > ${app.label.lowercase()}",
                                            Modifier.fillMaxWidth().clickable { vm.addToPage(i, Item.App(app.pkg)); adding = null }.padding(vertical = 5.dp),
                                        )
                                    }
                                }
                                null -> {}
                            }
                        }
                        if (pages.size < HomeLayout.MAX_PAGES) {
                            TermText("+ add page", Modifier.clickable { vm.addPage() }.padding(top = 14.dp, bottom = 6.dp), bold = true)
                        }
                        TermText("[done]", Modifier.clickable(onClick = onDismiss).padding(top = 10.dp), dim = true)
                    }
                }
            }
        }
    }
}
