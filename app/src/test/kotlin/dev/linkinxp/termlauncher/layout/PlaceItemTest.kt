package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceItemTest {
    private fun app(n: String): Item = Item.App("pkg.$n")
    private val a = app("a"); private val b = app("b"); private val c = app("c"); private val d = app("d")
    private val gap: Item = Item.Gap
    private fun layout(vararg items: Item) = HomeLayout(emptyList(), listOf(Page(items.toList())))
    private fun HomeLayout.items() = pages[0].items

    @Test fun droppingOnAnotherIconInsertsThereAndTheOthersShift() {
        assertEquals(listOf(b, c, a, d), layout(a, b, c, d).placeItem(0, a, 2).items())
        assertEquals(listOf(d, a, b, c), layout(a, b, c, d).placeItem(0, d, 0).items())
    }

    @Test fun droppingOnAnEmptySlotTakesThatSlotAndLeavesAGapBehind() {
        assertEquals(listOf(gap, b, a, d), layout(a, b, gap, d).placeItem(0, a, 2).items())
    }

    @Test fun droppingPastTheEndPadsWithGapsAndLeavesAGapBehind() {
        assertEquals(listOf(gap, b, c, d, gap, gap, a), layout(a, b, c, d).placeItem(0, a, 6).items())
    }

    @Test fun movingBackToTheFrontTrimsTheGapsThatAreNoLongerNeeded() {
        assertEquals(listOf(a, b, c, d), layout(gap, b, c, d, gap, gap, a).placeItem(0, a, 0).items())
    }

    @Test fun movingTheLastIconIntoAHoleDoesNotLeaveTrailingGaps() {
        assertEquals(listOf(a, d, c), layout(a, gap, c, d).placeItem(0, d, 1).items().let { it.dropLastWhile { x -> x == gap } })
    }

    @Test fun badPlacementsChangeNothing() {
        val l = layout(a, b)
        assertEquals(l, l.placeItem(0, a, 0))        // onto its own slot
        assertEquals(l, l.placeItem(0, c, 1))        // not on the page
        assertEquals(l, l.placeItem(3, a, 1))        // no such page
        assertEquals(l, l.placeItem(0, a, -2))
        assertEquals(l, l.placeItem(0, a, 500))      // absurdly far: refused
    }

    @Test fun gapsSurviveEncodingAndPruningAndAreNotApps() {
        val l = layout(a, gap, b)
        assertEquals(l, HomeLayout.decode(l.encode()))
        assertEquals(l.items(), l.prune(setOf("pkg.a", "pkg.b")).items())
        assertEquals(setOf("pkg.a", "pkg.b"), l.pages[0].items.filterIsInstance<Item.App>().map { it.pkg }.toSet())
    }

    @Test fun anItemListWithOnlyGapsStillCountsAsAPage() {
        assertEquals(1, layout(gap, gap).pages.size)
    }
}
