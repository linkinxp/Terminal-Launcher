package dev.linkinxp.termlauncher.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class DockOrderTest {
    private val l = HomeLayout.default().pin("a").pin("b").pin("c")
    private fun names(l: HomeLayout) = l.dock.map { (it as Item.App).pkg }

    @Test fun movesForwardAndBack() {
        assertEquals(listOf("b", "c", "a"), names(l.moveInDock(0, 2)))
        assertEquals(listOf("c", "a", "b"), names(l.moveInDock(2, 0)))
    }

    @Test fun badIndexesOrNoMoveChangeNothing() {
        assertEquals(l, l.moveInDock(1, 1))
        assertEquals(l, l.moveInDock(-1, 2))
        assertEquals(l, l.moveInDock(0, 3))
    }
}
