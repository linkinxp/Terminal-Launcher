package dev.linkinxp.termlauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.Page
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DragDropTest {
    private fun app(n: String) = Item.App("pkg.$n")
    private val a: Item = app("a"); private val b: Item = app("b"); private val c: Item = app("c"); private val d: Item = app("d")
    private val cells: Map<Item, Rect> = mapOf(
        a to Rect(0f, 0f, 100f, 100f), b to Rect(100f, 0f, 200f, 100f),
        c to Rect(0f, 100f, 100f, 200f), d to Rect(100f, 100f, 200f, 200f),
    )

    @Test fun theCellUnderThePointerIsTheTarget() {
        assertEquals(b, nearestCell(Offset(150f, 50f), cells))
        assertEquals(c, nearestCell(Offset(20f, 180f), cells))
    }

    @Test fun justOutsideTheGridStillSnapsToTheNearestCellButFarAwayIsNothing() {
        assertEquals(d, nearestCell(Offset(230f, 130f), cells))
        assertNull(nearestCell(Offset(900f, 900f), cells))
        assertNull(nearestCell(Offset(10f, 10f), emptyMap()))
    }

    @Test fun theSameLookupWorksForSlotNumbers() {
        val slots = mapOf(0 to Rect(0f, 0f, 100f, 100f), 1 to Rect(100f, 0f, 200f, 100f), 5 to Rect(0f, 100f, 100f, 200f))
        assertEquals(1, nearestCell(Offset(150f, 50f), slots))
        assertEquals(5, nearestCell(Offset(10f, 190f), slots))
        assertNull(nearestCell(Offset(900f, 900f), slots))
    }
}
