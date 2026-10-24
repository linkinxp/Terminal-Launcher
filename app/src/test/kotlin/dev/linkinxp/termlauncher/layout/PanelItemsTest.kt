package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelItemsTest {
    private val s = Item.Panel(PanelKind.SEARCH)
    private val sys = Item.Panel(PanelKind.SYS)
    private val cal = Item.Panel(PanelKind.CAL)
    private val wx = Item.Panel(PanelKind.WX)
    private fun layout(vararg pages: List<Item>) = HomeLayout(emptyList(), pages.map { Page(it) })

    @Test fun aFreshLayoutHasTheFourPanelsInTheOldOrder() {
        assertEquals(listOf<Item>(s, sys, cal, wx), HomeLayout.decode(null).pages[0].items)
    }

    @Test fun panelsSurviveEncodeAndDecode() {
        val l = layout(listOf(wx, Item.App("a")), listOf(s))
        assertEquals(l, HomeLayout.decode(l.encode()))
    }

    @Test fun aLayoutStoredBeforePanelsWereItemsGetsThemOnTopOfPageOne() {
        val old = """{"v":1,"layout":{"dock":[],"pages":[{"items":[{"type":"app","pkg":"a"}]},{"items":[]}]}}"""
        val l = HomeLayout.decode(old)
        assertEquals(listOf<Item>(s, sys, cal, wx, Item.App("a")), l.pages[0].items)
        assertEquals(emptyList<Item>(), l.pages[1].items)
    }

    @Test fun aCurrentLayoutWithoutPanelsStaysThatWay() {
        val l = layout(listOf(Item.App("a")))
        assertEquals(l, HomeLayout.decode(l.encode()))
    }

    @Test fun addingPutsAPanelOnTopOfThePageAndOnlyOnePerKind() {
        val l = layout(listOf(Item.App("a")), listOf(cal)).addPanel(1, PanelKind.WX).addPanel(0, PanelKind.CAL)
        assertEquals(listOf<Item>(cal, Item.App("a")), l.pages[0].items)
        assertEquals(listOf<Item>(wx), l.pages[1].items)
        assertEquals(setOf(PanelKind.CAL, PanelKind.WX), l.placedPanels())
    }

    @Test fun addingToAMissingPageChangesNothing() {
        val l = layout(listOf(Item.App("a")))
        assertEquals(l, l.addPanel(3, PanelKind.WX))
    }

    @Test fun movingSwapsWithTheNeighbourAndStopsAtTheEnds() {
        val l = layout(listOf(s, Item.App("a"), wx))
        assertEquals(listOf<Item>(Item.App("a"), s, wx), l.movePanel(PanelKind.SEARCH, +1).pages[0].items)
        assertEquals(listOf<Item>(s, wx, Item.App("a")), l.movePanel(PanelKind.WX, -1).pages[0].items)
        assertEquals(l, l.movePanel(PanelKind.SEARCH, -1))
        assertEquals(l, l.movePanel(PanelKind.WX, +1))
        assertEquals(l, l.movePanel(PanelKind.SYS, +1)) // not placed
    }

    @Test fun movingToAnotherPageGoesOnTop() {
        val l = layout(listOf(s, wx), listOf(Item.App("a"))).movePanelToPage(PanelKind.WX, 1)
        assertEquals(listOf<Item>(s), l.pages[0].items)
        assertEquals(listOf<Item>(wx, Item.App("a")), l.pages[1].items)
        assertEquals(l, l.movePanelToPage(PanelKind.WX, 9))
    }

    @Test fun removingTakesThePanelOffEveryPage() {
        val l = layout(listOf(s, wx), listOf(Item.App("a"))).removePanel(PanelKind.WX)
        assertEquals(listOf<Item>(s), l.pages[0].items)
        assertEquals(setOf(PanelKind.SEARCH), l.placedPanels())
    }

    @Test fun panelsStayWhenAppsAreCleanedUpAndWidgetsAreStripped() {
        val l = layout(listOf(s, Item.App("gone"), Item.Widget(1, "p/.W", WidgetSize.SMALL)))
        assertEquals(listOf<Item>(s), l.prune(emptySet()).withoutWidgets().pages[0].items)
        assertTrue(l.removePage(0) == l) // single page can't go
    }
}
