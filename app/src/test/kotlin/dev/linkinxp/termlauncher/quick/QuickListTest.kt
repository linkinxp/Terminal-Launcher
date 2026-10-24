package dev.linkinxp.termlauncher.quick

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickListTest {
    @Test fun addAppendsWithoutDuplicates() {
        assertEquals(listOf("a", "b"), quickAdd(listOf("a"), "b"))
        assertEquals(listOf("a"), quickAdd(listOf("a"), "a"))
    }

    @Test fun addStopsAtEight() {
        val full = (1..8).map { "p$it" }
        assertEquals(full, quickAdd(full, "extra"))
        assertEquals(8, MAX_QUICK)
    }

    @Test fun removeDropsOnlyThatApp() {
        assertEquals(listOf("a", "c"), quickRemove(listOf("a", "b", "c"), "b"))
        assertEquals(listOf("a"), quickRemove(listOf("a"), "zzz"))
    }

    @Test fun moveUpSwapsWithThePreviousOne() {
        assertEquals(listOf("b", "a", "c"), quickMoveUp(listOf("a", "b", "c"), "b"))
        assertEquals(listOf("a", "b"), quickMoveUp(listOf("a", "b"), "a")) // already first
        assertEquals(listOf("a", "b"), quickMoveUp(listOf("a", "b"), "zzz"))
    }

    @Test fun encodeDecodeRoundTrips() {
        val l = listOf("com.a", "com.b")
        assertEquals(l, decodeQuick(encodeQuick(l)))
        assertEquals(emptyList<String>(), decodeQuick(encodeQuick(emptyList())))
    }

    @Test fun decodeOfNothingStoredIsNullSoDefaultsApply() {
        assertNull(decodeQuick(null))
    }

    @Test fun decodeCleansBlankDuplicateAndOversizedData() {
        assertEquals(listOf("a", "b"), decodeQuick("a\n\n  \nb\na"))
        assertEquals(8, decodeQuick((1..20).joinToString("\n") { "p$it" })!!.size)
    }

    @Test fun tileLabelIsTheFirstWordLowercaseCutToSevenCharacters() {
        assertEquals("phone", tileLabel("Phone"))
        assertEquals("garmin", tileLabel("Garmin Connect"))
        assertEquals("photogr", tileLabel("Photographer"))
        assertEquals("?", tileLabel("  "))
        assertEquals("?", tileLabel(""))
    }

    @Test fun tilesWrapIntoRowsOfFour() {
        assertEquals(listOf(listOf(1, 2, 3, 4), listOf(5, 6)), tileRows(listOf(1, 2, 3, 4, 5, 6)))
        assertEquals(emptyList<List<Int>>(), tileRows(emptyList<Int>()))
        assertEquals(listOf(listOf(1, 2), listOf(3)), tileRows(listOf(1, 2, 3), perRow = 2))
    }

    @Test fun actionsParse() {
        assertEquals(QuickAction.Show, parseQuickAction(null, null).getOrThrow())
        assertEquals(QuickAction.Add("google maps"), parseQuickAction("add", "google maps").getOrThrow())
        assertEquals(QuickAction.Remove("chrome"), parseQuickAction("RM", "chrome").getOrThrow())
        assertEquals(QuickAction.Clear, parseQuickAction("clear", null).getOrThrow())
        assertEquals(QuickAction.Reset, parseQuickAction("reset", null).getOrThrow())
    }

    @Test fun badActionsGiveUsageMessages() {
        assertEquals("usage: quick add <app>", parseQuickAction("add", null).exceptionOrNull()?.message)
        assertEquals("usage: quick add <app>", parseQuickAction("add", "  ").exceptionOrNull()?.message)
        assertEquals("usage: quick rm <app>", parseQuickAction("rm", null).exceptionOrNull()?.message)
        assertEquals("quick: bogus: unknown (add rm clear reset)", parseQuickAction("bogus", null).exceptionOrNull()?.message)
        assertTrue(parseQuickAction("clear", "extra").isSuccess)
    }
}
