package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.quick.tileLabel
import dev.linkinxp.termlauncher.quick.tileRows

private const val PER_ROW = 4

/** The `quick` panel: your important apps as big text tiles, four per row. Tap launches, long-press opens the app menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickPanel(ui: LauncherUi, onTap: (String) -> Unit, onLongPress: (String) -> Unit, modifier: Modifier = Modifier) {
    if (!ui.panels.quick || ui.quick.isEmpty()) return
    TermPanel("quick", modifier.fillMaxWidth().padding(top = 8.dp)) {
        tileRows(ui.quick, PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { pkg ->
                    TermText(
                        "[${tileLabel(ui.apps[pkg]?.label ?: pkg)}]",
                        Modifier.weight(1f).testTag("quick-$pkg")
                            .combinedClickable(onClick = { onTap(pkg) }, onLongClick = { onLongPress(pkg) })
                            .padding(vertical = 10.dp),
                        bold = true,
                    )
                }
                repeat(PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
