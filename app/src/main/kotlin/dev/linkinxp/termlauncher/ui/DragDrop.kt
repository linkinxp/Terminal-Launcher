package dev.linkinxp.termlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** Which edge of the screen a finger is at while dragging: -1 left, 1 right, 0 neither ([edgePx] wide strips). */
fun edgeDirection(x: Float, width: Float, edgePx: Float): Int = when {
    x < edgePx -> -1
    x > width - edgePx -> 1
    else -> 0
}

private const val SWIPE_DP = 44f

/** How far a vertical swipe must travel (in pixels at this screen [density]) before it opens the drawer or the shade. */
fun swipeThresholdPx(density: Float): Float = SWIPE_DP * density

/** The middle 60% of a cell: a drop there lands on the icon (make a folder); nearer its edge it goes between icons. */
fun isOnTop(point: Offset, cell: Rect): Boolean =
    kotlin.math.abs(point.x - cell.center.x) <= cell.width * 0.3f && kotlin.math.abs(point.y - cell.center.y) <= cell.height * 0.3f

/** How far past a cell's edge a drop still counts for it, as a fraction of the cell's size. */
private const val SNAP = 0.75f

/** The slot a dragged icon is dropped on: the one under [point], else the nearest one within reach, else null. */
fun <K> nearestCell(point: Offset, cells: Map<K, Rect>): K? {
    cells.entries.firstOrNull { it.value.contains(point) }?.let { return it.key }
    val best = cells.entries.minByOrNull { (it.value.center - point).getDistance() } ?: return null
    val reach = maxOf(best.value.width, best.value.height) * (0.5f + SNAP)
    return best.key.takeIf { (best.value.center - point).getDistance() <= reach * 1.4f }
}
