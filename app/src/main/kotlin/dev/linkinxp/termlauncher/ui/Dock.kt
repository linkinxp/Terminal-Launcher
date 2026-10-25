package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.layout.HomeStyle
import dev.linkinxp.termlauncher.layout.Item
import androidx.compose.ui.unit.sp

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

/** Row of text tabs for the pinned items. Tap opens, long-press asks for the item menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Dock(
    ui: LauncherUi,
    onTap: (Item) -> Unit,
    onLongPress: (Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (ui.homeStyle == HomeStyle.ICONS) {
        Row(modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ui.layout.dock.forEach { item ->
                val label = dockLabel(item, ui) ?: return@forEach
                IconCell(label, { onTap(item) }, { onLongPress(item) }, Modifier.padding(horizontal = 4.dp), labelSize = minOf(10f, ui.density.labelSp).sp, vPad = 3.dp) { ItemIcon(item, ui, ui.density.dockDp.dp) }
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
