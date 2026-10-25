package dev.linkinxp.termlauncher.ui

import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.PanelKind
import dev.linkinxp.termlauncher.layout.WidgetSize
import org.junit.Assert.assertEquals
import org.junit.Test

class IconBlocksTest {
    private fun app(n: Int) = Item.App("a$n")

    @Test fun emptyAndSmallLists() {
        assertEquals(emptyList<IconBlock>(), iconBlocks(emptyList()))
        assertEquals(listOf(IconBlock.Cells(listOf(app(1)))), iconBlocks(listOf(app(1))))
    }

    @Test fun rowsOfFourWithAShorterLastRow() {
        val blocks = iconBlocks((1..9).map(::app))
        assertEquals(listOf(4, 4, 1), blocks.map { (it as IconBlock.Cells).items.size })
    }

    @Test fun panelsAndWidgetsBreakTheRunAndKeepTheFullWidth() {
        val w = Item.Widget(1, "p/.W", WidgetSize.SMALL)
        val p = Item.Panel(PanelKind.SYS)
        val blocks = iconBlocks(listOf(app(1), app(2), w, app(3), p, app(4)))
        assertEquals(
            listOf(IconBlock.Cells(listOf(app(1), app(2))), IconBlock.Full(w), IconBlock.Cells(listOf(app(3))), IconBlock.Full(p), IconBlock.Cells(listOf(app(4)))),
            blocks,
        )
    }

    @Test fun foldersAreCellsToo() {
        val f = Item.Folder("media", listOf("x", "y"))
        assertEquals(listOf(IconBlock.Cells(listOf(f, app(1)))), iconBlocks(listOf(f, app(1))))
    }

    @Test fun aDifferentColumnCountChunksDifferently() {
        assertEquals(listOf(3, 2), iconBlocks((1..5).map(::app), columns = 3).map { (it as IconBlock.Cells).items.size })
    }
}
