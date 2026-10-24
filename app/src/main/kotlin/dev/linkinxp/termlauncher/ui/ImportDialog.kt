package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.linkinxp.termlauncher.ImportPreview
import dev.linkinxp.termlauncher.theme.Theme

/** What an import would change, with `[replace]` / `[cancel]`. */
@Composable
fun ImportDialog(theme: Theme, preview: ImportPreview, onReplace: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Box(Modifier.padding(8.dp)) {
            DialogSurface(theme) {
                TermPanel(preview.title, Modifier.fillMaxWidth().padding(8.dp)) {
                    preview.lines.forEach { TermText(it) }
                    TermText("this replaces your current layout", Modifier.padding(top = 10.dp), dim = true)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        TermText("[replace]", Modifier.clickable(onClick = onReplace), bold = true)
                        TermText("[cancel]", Modifier.clickable(onClick = onCancel), dim = true)
                    }
                }
            }
        }
    }
}
