package dev.linkinxp.termlauncher.ui

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import dev.linkinxp.termlauncher.layout.HomeLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.layout.HomeStyle
import dev.linkinxp.termlauncher.layout.Item
import androidx.compose.ui.unit.sp

private val LAYOUT_MAX_DOCK = HomeLayout.MAX_DOCK

/** The first word of an app name, lowercase, at most nine characters (`messages`, `garmin`). */
fun dockName(label: String): String = label.trim().substringBefore(' ').lowercase().take(9)

/** Dock tab text: the app name (`messages`) or `~folder`; null when an app is gone. */
fun dockLabel(item: Item, ui: LauncherUi): String? = when (item) {
    is Item.App -> ui.apps[item.pkg]?.label?.let(::dockName)
    is Item.Folder -> "~${item.name}"
    is Item.Widget -> null
    is Item.Panel -> null
    is Item.Gap -> null
}

/** Display name of a layout item; null when an app is no longer installed. */
fun itemLabel(item: Item, ui: LauncherUi): String? = when (item) {
    is Item.App -> ui.apps[item.pkg]?.label?.lowercase()
    is Item.Folder -> "~${item.name}"
    is Item.Widget -> null
    is Item.Panel -> null
    is Item.Gap -> null
}

/**
 * Icon style with something pinned: the pinned apps and the quick tiles form one bottom bar (pinned first, no repeats),
 * so a newly pinned app sits in line with the tiles instead of in a row of its own. Null when the two stay separate.
 */
fun bottomBar(ui: LauncherUi): List<Item>? {
    if (ui.homeStyle != HomeStyle.ICONS || !ui.panels.quick || ui.layout.dock.isEmpty()) return null
    val pinned = ui.layout.dock
    return pinned + ui.quick.map { Item.App(it) }.filter { it !in pinned }
}

/** Row of text tabs for the pinned items. Tap opens, long-press asks for the item menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Dock(
    ui: LauncherUi,
    onTap: (Item) -> Unit,
    onLongPress: (Item) -> Unit,
    modifier: Modifier = Modifier,
    onBounds: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit = {},
    onMoveDock: (from: Int, to: Int) -> Unit = { _, _ -> },
) {
    val reorder = remember { DockReorder() }
    val pinnedCount = ui.layout.dock.size
    val merged = bottomBar(ui)
    if (merged != null) {
        val perRow = maxOf(LAYOUT_MAX_DOCK, ui.density.columns)
        Column(modifier.fillMaxWidth().padding(vertical = 2.dp).onGloballyPositioned { onBounds(it) }) {
            merged.chunked(perRow).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { item ->
                        val label = dockLabel(item, ui) ?: return@forEach
                        val tag = if (item is Item.App && item !in ui.layout.dock) Modifier.testTag("quick-${item.pkg}") else Modifier
                        val at = ui.layout.dock.indexOf(item)
                        val tint = if (at >= 0 && reorder.from >= 0 && reorder.over == at && at != reorder.from) LocalTheme.current.accent.color().copy(alpha = 0.22f) else Color.Transparent
                        val drag = if (at >= 0) Modifier.dockReorder(reorder, at, pinnedCount, onMoveDock) { onLongPress(item) } else Modifier
                        val fade = if (at >= 0 && reorder.from == at) Modifier.graphicsLayer { alpha = 0.35f } else Modifier
                        IconCell(label, { onTap(item) }, if (at >= 0) null else ({ onLongPress(item) }), Modifier.weight(1f).then(tag).then(fade).background(tint, RoundedCornerShape(14.dp)), labelSize = minOf(10f, ui.density.labelSp).sp, vPad = 3.dp, gesture = drag) { ItemIcon(item, ui, ui.density.dockDp.dp) }
                    }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        return
    }
    if (ui.homeStyle == HomeStyle.ICONS) {
        Row(modifier.fillMaxWidth().padding(vertical = 2.dp).onGloballyPositioned { onBounds(it) }, horizontalArrangement = Arrangement.SpaceEvenly) {
            ui.layout.dock.forEachIndexed { at, item ->
                val label = dockLabel(item, ui) ?: return@forEachIndexed
                val tint = if (reorder.from >= 0 && reorder.over == at && at != reorder.from) LocalTheme.current.accent.color().copy(alpha = 0.22f) else Color.Transparent
                val fade = if (reorder.from == at) Modifier.graphicsLayer { alpha = 0.35f } else Modifier
                IconCell(label, { onTap(item) }, null, Modifier.padding(horizontal = 4.dp).then(fade).background(tint, RoundedCornerShape(14.dp)), labelSize = minOf(10f, ui.density.labelSp).sp, vPad = 3.dp, gesture = Modifier.dockReorder(reorder, at, pinnedCount, onMoveDock) { onLongPress(item) }) { ItemIcon(item, ui, ui.density.dockDp.dp) }
            }
        }
        return
    }
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        ui.layout.dock.forEach { item ->
            val label = dockLabel(item, ui) ?: return@forEach
            TermText(
                label,
                Modifier.combinedClickable(onClick = { onTap(item) }, onLongClick = { onLongPress(item) })
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                bold = true,
            )
        }
    }
}
