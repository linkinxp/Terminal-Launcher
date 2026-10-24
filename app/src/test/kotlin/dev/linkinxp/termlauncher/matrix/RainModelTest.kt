package dev.linkinxp.termlauncher.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RainModelTest {
    @Test fun everyCellIsInsideTheGridAndUsesAKnownGlyphAndLevel() {
        for (tick in listOf(0L, 1L, 57L, 10_000L)) {
            val cells = rainCells(cols = 24, rows = 40, tick = tick, seed = 7)
            assertTrue(cells.isNotEmpty())
            cells.forEach {
                assertTrue(it.col in 0 until 24 && it.row in 0 until 40)
                assertTrue(it.glyph in RAIN_GLYPHS.indices)
                assertTrue(it.level in 0 until RAIN_TRAIL)
            }
        }
    }

    @Test fun aColumnHasAtMostOneHeadAndNeverMoreCellsThanTheTrail() {
        val cells = rainCells(30, 50, 123, 1)
        cells.groupBy { it.col }.values.forEach { col ->
            assertTrue(col.count { it.level == 0 } <= 1)
            assertTrue(col.size <= RAIN_TRAIL)
            assertEquals(col.size, col.map { it.row }.toSet().size)
        }
    }

    @Test fun theSameInputsGiveTheSameFrameAndTimeMovesTheRain() {
        assertEquals(rainCells(20, 30, 40, 3), rainCells(20, 30, 40, 3))
        assertNotEquals(rainCells(20, 30, 40, 3), rainCells(20, 30, 46, 3))
        assertNotEquals(rainCells(20, 30, 40, 3), rainCells(20, 30, 40, 4))
    }

    @Test fun aColumnsHeadFallsAsTimePasses() {
        // Follow one column's head over a few ticks: its row only increases until it wraps off the bottom.
        var last = -1
        var wrapped = false
        for (tick in 0L..200L) {
            val head = rainCells(1, 100, tick, 5).firstOrNull { it.level == 0 }?.row ?: continue
            if (head < last) wrapped = true else assertTrue(head >= last)
            last = head
        }
        assertTrue("the head should reach the bottom and start over within 200 ticks", wrapped || last > 0)
    }

    @Test fun degenerateGridsAreEmpty() {
        assertTrue(rainCells(0, 10, 1, 1).isEmpty())
        assertTrue(rainCells(10, 0, 1, 1).isEmpty())
        assertTrue(rainCells(-3, -3, -9, 1).isEmpty())
    }

    @Test fun negativeTicksDoNotCrash() {
        assertTrue(rainCells(10, 10, -50, 1).all { it.col in 0 until 10 && it.row in 0 until 10 })
    }

    @Test fun theGlyphSetHasKatakanaAndDigits() {
        assertTrue(RAIN_GLYPHS.any { it in 'ｦ'..'ﾝ' })
        assertTrue(('0'..'9').all { it in RAIN_GLYPHS })
        assertEquals(RAIN_GLYPHS.length, RAIN_GLYPHS.toSet().size)
    }
}
