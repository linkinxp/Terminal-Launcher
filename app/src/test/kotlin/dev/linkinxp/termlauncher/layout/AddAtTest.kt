package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Test

/** Dropping a drawer app on a home slot places it there (or moves it when it is already placed). */
class AddAtTest {
    private val base = HomeLayout.default().addToPage(0, Item.App("a")).addToPage(0, Item.App("b")).addToPage(0, Item.App("c"))
    private fun items(l: HomeLayout, page: Int = 0) = l.pages[page].items

    @Test fun anOccupiedSlotInsertsAndTheRestShift() =
        assertEquals(listOf<Item>(Item.App("a"), Item.App("n"), Item.App("b"), Item.App("c")), items(base.addAt(0, "n", 1)))

    @Test fun aHoleIsTakenWithoutShiftingAnything() {
        val withHole = base.placeItem(0, Item.App("b"), 5) // b moves past the end: a, hole, c, hole, hole, b
        val l = withHole.addAt(0, "n", 1)
        assertEquals(Item.App("n"), items(l)[1])
        assertEquals(Item.App("c"), items(l)[2])
    }

    @Test fun pastTheEndPadsWithHoles() =
        assertEquals(listOf<Item>(Item.App("a"), Item.App("b"), Item.App("c"), Item.Gap, Item.App("n")), items(base.addAt(0, "n", 4)))

    @Test fun anAppAlreadyOnAPageMovesInsteadOfDuplicating() {
        val l = base.addAt(0, "c", 0)
        assertEquals(listOf<Item>(Item.App("c"), Item.App("a"), Item.App("b")), items(l))
    }

    @Test fun anAppFromTheDockLeavesTheDock() {
        val l = base.pin("n").addAt(0, "n", 0)
        assertEquals(emptyList<Item>(), l.dock)
        assertEquals(Item.App("n"), items(l)[0])
    }

    @Test fun badPageOrSlotChangesNothing() {
        assertEquals(base, base.addAt(3, "n", 0))
        assertEquals(base, base.addAt(0, "n", -1))
        assertEquals(base, base.addAt(0, "n", 999))
    }
}
