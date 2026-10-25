package dev.linkinxp.termlauncher.fx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FxMathTest {
    @Test fun theScanBarSweepsTopToBottomAndStartsOver() {
        assertEquals(0f, scanBarY(0, 1000f, 7_000), 0.01f)
        assertEquals(500f, scanBarY(3_500, 1000f, 7_000), 0.01f)
        assertEquals(0f, scanBarY(7_000, 1000f, 7_000), 0.01f)
        assertTrue(scanBarY(123_456, 1000f, 7_000) in 0f..1000f)
        assertEquals(0f, scanBarY(-5, 1000f, 7_000), 0.01f)
    }

    @Test fun glitchBarsAreWithinTheScreenAndDeterministic() {
        val bars = glitchBars(seed = 3, count = 7, height = 2000f)
        assertEquals(7, bars.size)
        bars.forEach { assertTrue(it.y >= 0f && it.y + it.height <= 2000f && it.height > 0f && it.colorIndex in 0..2) }
        assertEquals(bars, glitchBars(3, 7, 2000f))
        assertNotEquals(bars, glitchBars(4, 7, 2000f))
        assertTrue(glitchBars(1, 0, 2000f).isEmpty())
        assertTrue(glitchBars(1, 5, 0f).isEmpty())
    }

    @Test fun glitchesComeEveryHalfMinuteToAMinute() {
        repeat(50) { assertTrue(nextGlitchDelayMs(it) in 30_000L..60_000L) }
        assertNotEquals(nextGlitchDelayMs(1), nextGlitchDelayMs(2))
    }

    @Test fun flickerIsAShortBurstOfFlashes() {
        val f = flickerFlashes(5)
        assertTrue(f.size in 2..4)
        assertTrue(f.all { it.durationMs in 30..90 && it.alpha in 0.04f..0.12f })
    }
}
