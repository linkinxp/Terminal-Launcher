package dev.linkinxp.termlauncher.ui

import dev.linkinxp.termlauncher.layout.HomeLayout
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
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
class DragState {
    var item by mutableStateOf<Item?>(null)
    /** Where the finger is on screen (root coordinates) and where it first picked the icon up. */
    var finger by mutableStateOf(Offset.Zero)
    var start = Offset.Zero
    /** -1, 0 or 1: the finger is at the left edge, nowhere special, or the right edge (the page turns after a moment). */
    var edge by mutableStateOf(0)
    var dockBounds: LayoutCoordinates? = null
    /** An app picked up in the drawer (the finger is then followed by the drawer drag watcher, not by a home cell). */
    var fromDrawer by mutableStateOf(false)
    var moved = false
    /** The slot (page * 100000 + slot) an icon would join as a folder if let go now, or -1. */
    var hover by mutableStateOf(-1L)
    /** Set by the home pager: drops [Item] picked up in the drawer where the finger let go. */
    var drawerDrop: (Offset, Item) -> Unit = { _, _ -> }
    /** Live layout coordinates of every slot: read at the moment of use, so a scrolling pager can never leave them stale. */
    val bounds = HashMap<Long, LayoutCoordinates>()
    fun key(page: Int, slot: Int) = page * SLOT_RANGE + slot
    /** The slots of one page, by slot number. */
    fun slotsOf(page: Int): Map<Int, Rect> = bounds.filter { it.key / SLOT_RANGE == page.toLong() && it.value.isAttached }
        .map { (it.key % SLOT_RANGE).toInt() to it.value.boundsInRoot() }.toMap()
    /** A screen position for [local] in the cell at [page]/[slot], or null when that cell is not on screen any more. */
    fun toRoot(page: Int, slot: Int, local: Offset): Offset? = bounds[key(page, slot)]?.takeIf { it.isAttached }?.localToRoot(local)
    private companion object { const val SLOT_RANGE = 100_000L }
}

/** An empty grid slot: nothing to see, but a place an icon can be dropped (numbered by its position in the page list). */
@Composable
private fun SlotBox(page: Int, slot: Int, ui: LauncherUi, drag: DragState, modifier: Modifier) {
    DisposableEffect(page, slot) { onDispose { drag.bounds.remove(drag.key(page, slot)) } }
    Box(modifier.height((ui.density.iconDp + 28f).dp).testTag("slot-$slot").onGloballyPositioned { drag.bounds[drag.key(page, slot)] = it })
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
    drag: DragState,
    currentPage: () -> Int,
    screenWidth: () -> Float,
    onMoveToPage: (fromPage: Int, item: Item, toPage: Int, slot: Int) -> Unit,
    onDropOnDock: (page: Int, item: Item) -> Unit,
    onMerge: (fromPage: Int, item: Item.App, toPage: Int, slot: Int) -> Unit = { _, _, _, _ -> },
) {
    val items = ui.layout.pages[page].items
    val columns = ui.density.columns
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
                        if (label == null) { SlotBox(page, slot, ui, drag, Modifier.weight(1f)); return@forEachIndexed }
                        DisposableEffect(page, slot) { onDispose { drag.bounds.remove(drag.key(page, slot)) } }
                        val dragging = drag.item == item
                        val joining = drag.hover == drag.key(page, slot)
                        val placeModifier = Modifier
                            .onGloballyPositioned { drag.bounds[drag.key(page, slot)] = it }
                            .background(if (joining) LocalTheme.current.accent.color().copy(alpha = 0.22f) else Color.Transparent, RoundedCornerShape(14.dp))
                            .graphicsLayer { alpha = if (dragging) 0.3f else 1f } // the floating copy follows the finger, this one marks where it came from
                        // Inside the cell's click handling, so it sees (and consumes) the lift before a click can fire.
                        val dragGesture = Modifier
                            .semantics { onLongClick(label = "item menu") { onItemLongPress(item); true } }
                            .pointerInput(item, slot) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { at ->
                                        // Positions are read in this cell's own (never moved) frame and turned into screen positions.
                                        val root = drag.toRoot(page, slot, at) ?: at
                                        drag.item = item; drag.start = root; drag.finger = root; drag.edge = 0
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        drag.toRoot(page, slot, change.position)?.let { drag.finger = it }
                                        drag.edge = edgeDirection(drag.finger.x, screenWidth(), 48.dp.toPx())
                                        drag.hover = mergeTarget(drag, ui.layout, item, currentPage())?.let { drag.key(currentPage(), it) } ?: -1L
                                    },
                                    onDragEnd = {
                                        val finger = drag.finger
                                        if ((finger - drag.start).getDistance() < viewConfiguration.touchSlop) onItemLongPress(item)
                                        else {
                                            val target = currentPage()
                                            val joinSlot = mergeTarget(drag, ui.layout, item, target)
                                            when {
                                                drag.dockBounds?.takeIf { it.isAttached }?.boundsInRoot()?.contains(finger) == true -> onDropOnDock(page, item)
                                                joinSlot != null -> onMerge(page, item as Item.App, target, joinSlot)
                                                else -> nearestCell(finger, drag.slotsOf(target))?.let { s ->
                                                    if (target == page) { if (s != slot) onMoveItem(page, item, s) } else onMoveToPage(page, item, target, s)
                                                }
                                            }
                                        }
                                        drag.item = null; drag.edge = 0; drag.hover = -1L
                                    },
                                    onDragCancel = { drag.item = null; drag.edge = 0; drag.hover = -1L },
                                )
                            }
                        IconCell(label, { onItemTap(item) }, null, Modifier.weight(1f).then(placeModifier), labelSize = ui.density.labelSp.sp, gesture = dragGesture) { ItemIcon(item, ui, ui.density.iconDp.dp) }
                    }
                    // The rest of the last row: empty slots an icon can be dropped on.
                    repeat(columns - block.items.size) { k -> SlotBox(page, block.start + block.items.size + k, ui, drag, Modifier.weight(1f)) }
                }
                if (block === tail && drag.item != null) {
                    // Takes no layout space (a bottom-anchored page would jump up a row the moment you pick an icon up): the
                    // slots hang below the last row, invisible, only there to be dropped on.
                    Row(Modifier.fillMaxWidth().layout { m, c -> val p = m.measure(c); layout(p.width, 0) { p.place(0, 0) } }) {
                        repeat(columns) { k -> SlotBox(page, block.start + columns + k, ui, drag, Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** The slot on [page] the dragged app would join as a folder: it must be right on top of another app or a folder. */
internal fun mergeTarget(drag: DragState, layout: HomeLayout, dragged: Item, page: Int): Int? {
    if (dragged !is Item.App) return null
    val slots = drag.slotsOf(page)
    val slot = nearestCell(drag.finger, slots) ?: return null
    val rect = slots[slot] ?: return null
    val target = layout.pages.getOrNull(page)?.items?.getOrNull(slot)
    return slot.takeIf { isOnTop(drag.finger, rect) && target != dragged && (target is Item.App || target is Item.Folder) }
}
