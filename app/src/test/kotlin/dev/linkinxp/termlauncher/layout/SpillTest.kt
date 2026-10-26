package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Test

/** A page that is full hands its last icon to the next page, so home never has to scroll. */
class SpillTest {
    private fun app(n: String): Item = Item.App(n)
    private fun pkgs(l: HomeLayout, page: Int) = l.pages[page].items.map { (it as? Item.App)?.pkg ?: it.toString() }

    @Test fun theLastIconGoesToTheFrontOfTheNextPage() {
        val l = HomeLayout.default().addToPage(0, app("a")).addToPage(0, app("b")).addPage().addToPage(1, app("z")).spillPage(0)
        assertEquals(listOf("a"), pkgs(l, 0))
        assertEquals(listOf("b", "z"), pkgs(l, 1))
    }

    @Test fun aNewPageIsAddedWhenThereIsNoNextOne() {
        val l = HomeLayout.default().addToPage(0, app("a")).addToPage(0, app("b")).spillPage(0)
        assertEquals(2, l.pages.size)
        assertEquals(listOf("a"), pkgs(l, 0))
        assertEquals(listOf("b"), pkgs(l, 1))
    }

    @Test fun pagesWithPanelsAreSkipped() {
        val glance = HomeLayout.fresh() // apps page, then the panels page
        val l = glance.addToPage(0, app("a")).addToPage(0, app("b")).spillPage(0)
        assertEquals(3, l.pages.size)
        assertEquals(listOf("b"), pkgs(l, 2))
        assertEquals(glance.pages[1], l.pages[1])
    }

    @Test fun holesLeftBehindAreTrimmed() {
        val l = HomeLayout.default().addToPage(0, app("a")).addToPage(0, app("b")).placeItem(0, app("b"), 4).spillPage(0)
        assertEquals(listOf("a"), pkgs(l, 0)) // the holes b leaves behind are trimmed
        assertEquals(listOf("b"), pkgs(l, 1))
    }

    @Test fun noRoomAnywhereOrNothingToMoveChangesNothing() {
        val full = (1..HomeLayout.MAX_PAGES - 1).fold(HomeLayout.default()) { l, _ -> l.addPage() }
            .let { l -> (0 until l.pages.size).fold(l) { acc, i -> acc.addPanel(i, PanelKind.entries[i % PanelKind.entries.size]) } }
        assertEquals(full, full.spillPage(0))
        val onlyPanels = HomeLayout.default().addPanel(0, PanelKind.SYS)
        assertEquals(onlyPanels, onlyPanels.spillPage(0))
        assertEquals(onlyPanels, onlyPanels.spillPage(5))
    }
}
