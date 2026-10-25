package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.sys.SysSnapshot

/** The icon being dragged on one page, how far it has moved, and where every slot is (read only when it drops). */
internal class DragState {
    var item by mutableStateOf<Item?>(null)
    var offset by mutableStateOf(Offset.Zero)
    val bounds = HashMap<Int, Rect>()
}

/** An empty grid slot: nothing to see, but a place an icon can be dropped (numbered by its position in the page list). */
@Composable
private fun SlotBox(slot: Int, ui: LauncherUi, drag: DragState, modifier: Modifier) {
    DisposableEffect(slot) { onDispose { drag.bounds.remove(slot) } }
    Box(modifier.height((ui.density.iconDp + 28f).dp).testTag("slot-$slot").onGloballyPositioned { drag.bounds[slot] = it.boundsInRoot() })
}

/**
 * One home page in the icon style: rows of icons with the panels and widgets between them at full width. Hold an icon to
 * pick it up and drop it on any slot, occupied or empty; a hold without moving is the item menu.
 */
@Composable
internal fun IconPageContent(
    page: Int,
    ui: LauncherUi,
    vm: LauncherViewModel,
    snapshot: SysSnapshot,
    onSearch: (String) -> Unit,
    onItemTap: (Item) -> Unit,
    onItemLongPress: (Item) -> Unit,
    onMoveItem: (page: Int, item: Item, slot: Int) -> Unit,
) {
    val items = ui.layout.pages[page].items
    val columns = ui.density.columns
    val drag = remember(page) { DragState() }
    val blocks = iconBlocks(items, columns)
    // Empty slots after the last icon (and, while dragging, one more row) can take a drop: only when the icons run to the end of the page.
    val tail = (blocks.lastOrNull() as? IconBlock.Cells)?.takeIf { it.start + it.items.size == items.size }
    blocks.forEach { block ->
        when (block) {
            is IconBlock.Full -> {
                val item = block.item
                if (item is Item.Widget) key(item.id) { WidgetPanel(item, vm, ui.density) }
                else if (item is Item.Panel) PanelItem(item.kind, ui, snapshot, onSearch)
            }
            is IconBlock.Cells -> {
                Row(Modifier.fillMaxWidth().zIndex(if (drag.item != null && drag.item in block.items) 1f else 0f)) {
                    block.items.forEachIndexed { k, item ->
                        val slot = block.start + k
                        val label = if (item is Item.Gap) null else itemLabel(item, ui)
                        if (label == null) { SlotBox(slot, ui, drag, Modifier.weight(1f)); return@forEachIndexed }
                        DisposableEffect(slot) { onDispose { drag.bounds.remove(slot) } }
                        val dragging = drag.item == item
                        val placeModifier = Modifier
                            .onGloballyPositioned { drag.bounds[slot] = it.boundsInRoot() }
                            .graphicsLayer {
                                if (dragging) { translationX = drag.offset.x; translationY = drag.offset.y; scaleX = 1.12f; scaleY = 1.12f; alpha = 0.9f }
                            }
                        // Inside the cell's click handling, so it sees (and consumes) the lift before a click can fire.
                        val dragGesture = Modifier
                            .semantics { onLongClick(label = "item menu") { onItemLongPress(item); true } }
                            .pointerInput(item, slot) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { drag.item = item; drag.offset = Offset.Zero },
                                    onDrag = { change, amount -> change.consume(); drag.offset += amount },
                                    onDragEnd = {
                                        val from = drag.bounds[slot]
                                        if (drag.offset.getDistance() < viewConfiguration.touchSlop) onItemLongPress(item)
                                        else if (from != null) nearestCell(from.center + drag.offset, drag.bounds)?.let { target -> if (target != slot) onMoveItem(page, item, target) }
                                        drag.item = null; drag.offset = Offset.Zero
                                    },
                                    onDragCancel = { drag.item = null; drag.offset = Offset.Zero },
                                )
                            }
                        IconCell(label, { onItemTap(item) }, null, Modifier.weight(1f).then(placeModifier), labelSize = ui.density.labelSp.sp, gesture = dragGesture) { ItemIcon(item, ui, ui.density.iconDp.dp) }
                    }
                    // The rest of the last row: empty slots an icon can be dropped on.
                    repeat(columns - block.items.size) { k -> SlotBox(block.start + block.items.size + k, ui, drag, Modifier.weight(1f)) }
                }
                if (block === tail && drag.item != null) {
                    Row(Modifier.fillMaxWidth()) { repeat(columns) { k -> SlotBox(block.start + columns + k, ui, drag, Modifier.weight(1f)) } }
                }
            }
        }
    }
}
