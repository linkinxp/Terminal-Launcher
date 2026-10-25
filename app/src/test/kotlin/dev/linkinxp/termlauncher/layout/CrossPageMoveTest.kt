package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class CrossPageMoveTest {
    private fun app(n: String): Item = Item.App("pkg.$n")
    private val a = app("a"); private val b = app("b"); private val c = app("c"); private val d = app("d"); private val e = app("e")
    private val gap: Item = Item.Gap

    private fun layout(dock: List<Item> = emptyList(), vararg pages: List<Item>) = HomeLayout(dock, pages.map { Page(it) })

    @Test fun anIconMovedToAnotherPageLeavesAHoleBehindAndTakesTheSlot() {
        val l = layout(emptyList(), listOf(a, b, c), listOf(d, gap, e)).moveItemToPage(0, b, 1, 1)
        assertEquals(listOf(a, gap, c), l.pages[0].items)
        assertEquals(listOf(d, b, e), l.pages[1].items)
    }

    @Test fun droppingOnAnOccupiedSlotInsertsAndPastTheEndPads() {
        assertEquals(listOf(d, b, e), layout(emptyList(), listOf(a, b), listOf(d, e)).moveItemToPage(0, b, 1, 1).pages[1].items)
        val padded = layout(emptyList(), listOf(a, b), listOf(d)).moveItemToPage(0, a, 1, 3)
        assertEquals(listOf(d, gap, gap, a), padded.pages[1].items)
        assertEquals(listOf(b), padded.pages[0].items.dropWhile { it == gap }) // the hole at the front stays, the rest keeps its place
    }

    @Test fun theSourcePageDropsItsTrailingHoles() {
        assertEquals(listOf(a), layout(emptyList(), listOf(a, b), listOf(d)).moveItemToPage(0, b, 1, 0).pages[0].items)
    }

    @Test fun badCrossPageMovesChangeNothing() {
        val l = layout(emptyList(), listOf(a), listOf(b))
        assertEquals(l, l.moveItemToPage(0, a, 0, 0))   // same page: that is placeItem's job
        assertEquals(l, l.moveItemToPage(0, c, 1, 0))   // not on the page
        assertEquals(l, l.moveItemToPage(0, a, 5, 0))   // no such page
        assertEquals(l, l.moveItemToPage(0, gap, 1, 0))
        assertEquals(l, l.moveItemToPage(0, a, 1, 900)) // absurd slot
    }

    @Test fun anIconDroppedOnTheDockIsPinnedAndLeavesTheHomePage() {
        val l = layout(listOf(c), listOf(a, b)).moveToDock(0, a)
        assertEquals(listOf(c, a), l.dock)
        assertEquals(listOf(b).let { listOf(gap) + it }, l.pages[0].items)
    }

    @Test fun aFullDockOrADuplicateRefusesTheMove() {
        val full = layout((1..HomeLayout.MAX_DOCK).map { app("d$it") }, listOf(a))
        assertEquals(full, full.moveToDock(0, a))
        val dup = layout(listOf(a), listOf(a, b))
        assertEquals(dup, dup.moveToDock(0, a))
        val panel = layout(emptyList(), listOf(Item.Panel(PanelKind.SYS)))
        assertEquals(panel, panel.moveToDock(0, Item.Panel(PanelKind.SYS)))
    }

    @Test fun aFolderCanBePinnedToo() {
        val f: Item = Item.Folder("media", listOf("x"))
        assertEquals(listOf(f), layout(emptyList(), listOf(f, a)).moveToDock(0, f).dock)
    }
}
