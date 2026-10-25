package dev.linkinxp.termlauncher.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import dev.linkinxp.termlauncher.ui.isOnTop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Dropping an icon right on top of another one makes a folder of the two (or adds to a folder). */
class MergeTest {
    private val a = Item.App("a"); private val b = Item.App("b"); private val c = Item.App("c")
    private val base = HomeLayout.default().addToPage(0, a).addToPage(0, b).addToPage(0, c)

    @Test fun twoAppsBecomeAFolderWhereTheTargetWas() {
        val l = base.mergeOnto(0, a, 0, 1) // a dropped on b
        assertEquals(listOf<Item>(Item.Gap, Item.Folder("folder", listOf("b", "a")), c), l.pages[0].items)
    }

    @Test fun anAppDroppedOnAFolderJoinsIt() {
        val withFolder = base.mkdir("work").getOrThrow() // folder is the 4th item
        val l = withFolder.mergeOnto(0, a, 0, 3)
        assertEquals(Item.Folder("work", listOf("a")), l.pages[0].items[3])
        assertEquals(Item.Gap, l.pages[0].items[0])
    }

    @Test fun mergingAcrossPagesTakesTheAppFromItsOldPage() {
        val two = base.addPage().addToPage(1, Item.App("z"))
        val l = two.mergeOnto(0, a, 1, 0)
        assertEquals(listOf<Item>(Item.Folder("folder", listOf("z", "a"))), l.pages[1].items)
        assertEquals(listOf<Item>(Item.Gap, b, c), l.pages[0].items)
    }

    @Test fun newFoldersGetFreeNames() {
        val l = base.mkdir("folder").getOrThrow().mkdir("folder2").getOrThrow()
        assertEquals("folder3", l.newFolderName())
        assertEquals("folder", base.newFolderName())
    }

    @Test fun nothingHappensForSelfEmptySlotsOrFolders() {
        assertEquals(base, base.mergeOnto(0, a, 0, 0)) // onto itself
        assertEquals(base, base.mergeOnto(0, a, 0, 9)) // empty slot
        assertEquals(base, base.mergeOnto(0, a, 7, 0)) // no such page
        val f = base.mkdir("g").getOrThrow()
        assertEquals(f, f.mergeOnto(0, a, 0, 99))
    }

    @Test fun onlyTheMiddleOfACellCountsAsOnTop() {
        val cell = Rect(0f, 0f, 100f, 100f)
        assertTrue(isOnTop(Offset(50f, 50f), cell))
        assertTrue(isOnTop(Offset(70f, 30f), cell))
        assertFalse(isOnTop(Offset(5f, 50f), cell))   // left edge: insert instead
        assertFalse(isOnTop(Offset(50f, 95f), cell))
        assertFalse(isOnTop(Offset(150f, 50f), cell))
    }
}
