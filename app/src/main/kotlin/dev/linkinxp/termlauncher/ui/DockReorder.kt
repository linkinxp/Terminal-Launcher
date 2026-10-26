package dev.linkinxp.termlauncher.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics

/** Which pinned app is being dragged and which pinned slot it is over; the cells' live positions are read at the moment of use. */
class DockReorder {
    var from by mutableIntStateOf(-1)
    var over by mutableIntStateOf(-1)
    val cells = HashMap<Int, LayoutCoordinates>()
}

/** The pinned cell the point is on (or nearest to, among [count] pinned cells), or -1 when none is on screen. */
internal fun pinnedCellAt(r: DockReorder, point: Offset, count: Int): Int =
    nearestCell(point, (0 until count).mapNotNull { i -> r.cells[i]?.takeIf { it.isAttached }?.let { i to it.boundsInRoot() } }.toMap()) ?: -1

/**
 * Hold a pinned app and drag it over another pinned app to put it there; a hold that never moves is the item menu.
 * Nothing moves on screen while dragging (the target is tinted), so the finger and the cell never drift apart.
 */
fun Modifier.dockReorder(r: DockReorder, index: Int, pinned: Int, onMove: (from: Int, to: Int) -> Unit, onMenu: () -> Unit): Modifier =
    onGloballyPositioned { r.cells[index] = it }
        .semantics { onLongClick(label = "item menu") { onMenu(); true } }
        .pointerInput(index, pinned) {
            var start = Offset.Zero
            var moved = false
            detectDragGesturesAfterLongPress(
                onDragStart = { at ->
                    start = r.cells[index]?.takeIf { it.isAttached }?.localToRoot(at) ?: at
                    moved = false; r.from = index; r.over = index
                },
                onDrag = { change, _ ->
                    change.consume()
                    val root = r.cells[index]?.takeIf { it.isAttached }?.localToRoot(change.position) ?: return@detectDragGesturesAfterLongPress
                    if ((root - start).getDistance() > viewConfiguration.touchSlop) moved = true
                    if (moved) r.over = pinnedCellAt(r, root, pinned)
                },
                onDragEnd = {
                    val to = r.over
                    if (!moved) onMenu() else if (to >= 0 && to != index) onMove(index, to)
                    r.from = -1; r.over = -1
                },
                onDragCancel = { r.from = -1; r.over = -1 },
            )
        }
