package dev.linkinxp.termlauncher.wall

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AsciiArtTest {
    private val sizes = listOf(80 to 60, 1 to 1, 7 to 3)

    private fun List<String>.fraction(pred: (Char) -> Boolean): Double {
        val all = joinToString("")
        return if (all.isEmpty()) 0.0 else all.count(pred).toDouble() / all.length
    }

    @Test fun everyStyleHasExactDimensionsAndPrintableAscii() {
        for (style in AsciiStyle.entries) for ((c, r) in sizes) {
            val art = generateAscii(style, c, r, 7)
            assertEquals("$style rows", r, art.size)
            art.forEach { line ->
                assertEquals("$style cols", c, line.length)
                assertTrue("$style printable", line.all { it.code in 0x20..0x7E })
            }
        }
    }

    @Test fun zeroAndNegativeSizesAreSafe() {
        for (style in AsciiStyle.entries) {
            assertTrue(generateAscii(style, 0, 10, 1).all { it.isEmpty() })
            assertTrue(generateAscii(style, 10, 0, 1).isEmpty())
            assertTrue(generateAscii(style, -5, -5, 1).isEmpty())
            assertTrue(generateAscii(style, -5, 3, 1).all { it.isEmpty() })
        }
    }

    @Test fun sameInputsGiveTheSameArt() {
        for (style in AsciiStyle.entries) {
            assertEquals(generateAscii(style, 80, 60, 3), generateAscii(style, 80, 60, 3))
        }
    }

    @Test fun differentSeedsGiveDifferentArt() {
        for (style in AsciiStyle.entries) {
            assertNotEquals("$style", generateAscii(style, 80, 60, 1), generateAscii(style, 80, 60, 2))
        }
    }

    @Test fun starsAreMostlyEmptySpace() {
        assertTrue(generateAscii(AsciiStyle.STARS, 80, 60, 5).fraction { it == ' ' } >= 0.8)
    }

    @Test fun glyphFieldIsMostlyFilled() {
        assertTrue(generateAscii(AsciiStyle.GLYPH, 80, 60, 5).fraction { it != ' ' } >= 0.6)
    }

    @Test fun skylineIsBuiltUpFromTheBottom() {
        val bottom = generateAscii(AsciiStyle.SKYLINE, 80, 60, 5).last()
        assertTrue(bottom.count { it != ' ' } * 2 >= bottom.length)
    }

    @Test fun hexdumpRowsLookLikeAMemoryDump() {
        generateAscii(AsciiStyle.HEXDUMP, 60, 10, 5).forEach { line ->
            assertTrue(line.contains(':'))
            assertTrue(line.any { it in "0123456789abcdef" })
        }
    }

    @Test fun aLargeGridStillRenders() {
        assertEquals(500, generateAscii(AsciiStyle.GLYPH, 500, 500, 1).size)
    }

    @Test fun driftRotatesRowsUpAndWraps() {
        val l = listOf("a", "b", "c")
        assertEquals(listOf("b", "c", "a"), driftLines(l, 1))
        assertEquals(listOf("c", "a", "b"), driftLines(l, -1))
        assertEquals(listOf("b", "c", "a"), driftLines(l, 4))
    }

    @Test fun driftSurvivesHugeStepsAndEmptyInput() {
        val l = listOf("a", "b", "c")
        assertEquals(3, driftLines(l, Int.MIN_VALUE).size)
        assertEquals(3, driftLines(l, Int.MAX_VALUE).size)
        assertTrue(driftLines(emptyList(), 5).isEmpty())
    }
}
