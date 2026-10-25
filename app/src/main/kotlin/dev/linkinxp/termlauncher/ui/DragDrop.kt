package dev.linkinxp.termlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** How far past a cell's edge a drop still counts for it, as a fraction of the cell's size. */
private const val SNAP = 0.75f

/** The slot a dragged icon is dropped on: the one under [point], else the nearest one within reach, else null. */
fun <K> nearestCell(point: Offset, cells: Map<K, Rect>): K? {
    cells.entries.firstOrNull { it.value.contains(point) }?.let { return it.key }
    val best = cells.entries.minByOrNull { (it.value.center - point).getDistance() } ?: return null
    val reach = maxOf(best.value.width, best.value.height) * (0.5f + SNAP)
    return best.key.takeIf { (best.value.center - point).getDistance() <= reach * 1.4f }
}
