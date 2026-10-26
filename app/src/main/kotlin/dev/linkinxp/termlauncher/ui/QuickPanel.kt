package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.layout.HomeStyle
import dev.linkinxp.termlauncher.quick.tileLabel
import dev.linkinxp.termlauncher.quick.tileRows

private const val PER_ROW = 4

/** The `quick` panel: your important apps as big text tiles, four per row. Tap launches, long-press opens the app menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickPanel(ui: LauncherUi, onTap: (String) -> Unit, onLongPress: (String) -> Unit, modifier: Modifier = Modifier) {
    if (!ui.panels.quick || ui.quick.isEmpty() || bottomBar(ui) != null) return // merged into the dock row
    if (ui.homeStyle == HomeStyle.ICONS) {
        // Icon style: no bordered panel and no title, just a compact row of small icons, so the apps get the room.
        val perRow = ui.density.columns
        Column(modifier.fillMaxWidth().padding(top = 2.dp)) {
            tileRows(ui.quick, perRow).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { pkg ->
                        val label = tileLabel(ui.apps[pkg]?.label ?: pkg).lowercase()
                        IconCell(label, { onTap(pkg) }, { onLongPress(pkg) }, Modifier.weight(1f).testTag("quick-$pkg"), labelSize = minOf(10f, ui.density.labelSp).sp, vPad = 3.dp) {
                            BadgedIcon(listOf(pkg), ui.badges, "badge-$pkg") { IconTile(pkg, ui.apps[pkg]?.label ?: pkg, ui.density.quickDp.dp) }
                        }
                    }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        return
    }
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
