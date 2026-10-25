package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.layout.Item

const val ICON_COLUMNS = 4

/** What the icon page lays out: a row of app/folder cells, or a panel/widget that keeps the full width. */
sealed interface IconBlock {
    data class Cells(val items: List<Item>) : IconBlock
    data class Full(val item: Item) : IconBlock
}

/** Apps and folders in rows of [columns]; panels and widgets break the run and stand alone. */
fun iconBlocks(items: List<Item>, columns: Int = ICON_COLUMNS): List<IconBlock> {
    val out = ArrayList<IconBlock>()
    val run = ArrayList<Item>()
    fun flush() { run.chunked(columns).forEach { out += IconBlock.Cells(it) }; run.clear() }
    items.forEach { item ->
        if (item is Item.Widget || item is Item.Panel) { flush(); out += IconBlock.Full(item) } else run += item
    }
    flush()
    return out
}

/** One grid cell: the icon above a one-line label. Tap launches, long-press asks for the item menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconCell(
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelSize: TextUnit = 11.sp,
    vPad: Dp = 6.dp,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = vPad),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        TermText(label, Modifier.padding(top = 2.dp), size = labelSize, maxLines = 1)
    }
}

/** A folder as a tile: its first four apps as small icons in a 2x2. */
@Composable
fun FolderTile(folder: Item.Folder, ui: LauncherUi, size: Dp) {
    val theme = LocalTheme.current
    val mini = size * 0.4f
    Column(
        Modifier.size(size).background(theme.dim.color().copy(alpha = 0.18f), RoundedCornerShape(size * 0.28f)).padding(size * 0.08f),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        folder.apps.take(4).chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly) {
                pair.forEach { pkg -> IconTile(pkg, ui.apps[pkg]?.label ?: pkg, mini) }
            }
        }
    }
}

/** The icon for a layout item: an app's themed tile, or a folder tile. */
@Composable
fun ItemIcon(item: Item, ui: LauncherUi, size: Dp) {
    when (item) {
        is Item.App -> IconTile(item.pkg, ui.apps[item.pkg]?.label ?: item.pkg, size)
        is Item.Folder -> FolderTile(item, ui, size)
        else -> {}
    }
}

