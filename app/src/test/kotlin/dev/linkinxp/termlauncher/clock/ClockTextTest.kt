package dev.linkinxp.termlauncher.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockTextTest {
    @Test fun everyDigitIsThreeEqualWideLinesOfPrintableAscii() {
        for (d in (0..9) + BLANK) {
            val lines = figletLines(listOf(d))
            assertEquals("digit $d", 3, lines.size)
            assertEquals("digit $d", setOf(4), lines.map { it.length }.toSet())
            lines.forEach { l -> assertTrue("digit $d: '$l'", l.all { it.code in 32..126 }) }
        }
    }

    @Test fun fourDigitsAreSeparatedByASingleSpace() {
        val lines = figletLines(listOf(2, 1, 0, 5))
        assertEquals(3, lines.size)
        lines.forEach { assertEquals(4 * 5 - 1, it.length) }
    }

    @Test fun differentDigitsLookDifferent() {
        val art = (0..9).map { figletLines(listOf(it)) }
        assertEquals(10, art.toSet().size)
    }

    @Test fun aBlankDigitIsAllSpaces() {
        assertEquals(listOf("    ", "    ", "    "), figletLines(listOf(BLANK)))
        assertEquals(listOf("    ", "    ", "    "), figletLines(listOf(42)))
    }

    @Test fun binaryColumnsForTwentyOneTwentyNine() {
        val cols = binaryColumns(21, 29)
        assertEquals(listOf(3, 4, 3, 4), cols.map { it.size })
        assertEquals(listOf(false, true, false), cols[0])           // 2
        assertEquals(listOf(false, false, false, true), cols[1])    // 1
        assertEquals(listOf(false, true, false), cols[2])           // 2
        assertEquals(listOf(true, false, false, true), cols[3])     // 9
    }

    @Test fun binaryColumnsAtTheEdges() {
        assertTrue(binaryColumns(0, 0).flatten().none { it })
        val c = binaryColumns(23, 59)
        assertEquals(listOf(false, true, false), c[0])              // 2
        assertEquals(listOf(false, false, true, true), c[1])        // 3
        assertEquals(listOf(true, false, true), c[2])               // 5
        assertEquals(listOf(true, false, false, true), c[3])        // 9
    }

    @Test fun hexTime() {
        assertEquals("0x00:00", hexTime(0, 0))
        assertEquals("0x0F:1E", hexTime(15, 30))
        assertEquals("0x17:3B", hexTime(23, 59))
    }

    @Test fun epochText() {
        assertEquals("1761283749", epochText(1_761_283_749L))
        assertEquals("0000000000", epochText(0))
        assertEquals("0000000000", epochText(-5))
        assertEquals("2147483648", epochText(Int.MAX_VALUE + 1L))
        assertEquals("99999999999", epochText(99_999_999_999L))
    }

    @Test fun segmentMasks() {
        assertEquals(0b1111111, segmentMask(8))
        assertEquals(2, Integer.bitCount(segmentMask(1)))
        assertEquals(0, segmentMask(BLANK))
        assertEquals(0, segmentMask(10))
        assertEquals(0, segmentMask(-7))
        assertEquals(10, (0..9).map { segmentMask(it) }.toSet().size)
        assertNotEquals(segmentMask(6), segmentMask(5))
    }
}
