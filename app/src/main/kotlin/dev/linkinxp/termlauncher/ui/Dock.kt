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
import dev.linkinxp.termlauncher.layout.Item

/** Display name of a layout item; null when an app is no longer installed. */
fun itemLabel(item: Item, ui: LauncherUi): String? = when (item) {
    is Item.App -> ui.apps[item.pkg]?.label?.lowercase()
    is Item.Folder -> "~${item.name}"
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
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        ui.layout.dock.forEach { item ->
            val label = itemLabel(item, ui) ?: return@forEach
            TermText(
                label.take(6),
                Modifier.combinedClickable(onClick = { onTap(item) }, onLongClick = { onLongPress(item) })
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                bold = true,
            )
        }
    }
}
