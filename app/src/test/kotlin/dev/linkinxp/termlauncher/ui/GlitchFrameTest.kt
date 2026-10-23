package dev.linkinxp.termlauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlitchFrameTest {
    private val real = listOf(2, 1, 0, 8)

    @Test fun finishedProgressShowsTheRealDigits() {
        assertEquals(real, glitchFrame(real, 1f, 3))
        assertEquals(real, glitchFrame(real, 2f, 3))
    }

    @Test fun sameSeedGivesTheSameFrame() {
        assertEquals(glitchFrame(real, 0f, 42), glitchFrame(real, 0f, 42))
    }

    @Test fun framesAreFourDigitsZeroToNine() {
        listOf(0f, 0.3f, 0.99f).forEach { p ->
            val f = glitchFrame(real, p, 9)
            assertEquals(4, f.size)
            assertTrue(f.all { it in 0..9 })
        }
    }

    @Test fun digitsLockLeftToRight() {
        val f = glitchFrame(real, 0.45f, 1)
        assertEquals(real[0], f[0])
        assertEquals(real[1], f[1])
        assertTrue((0..50).any { seed -> glitchFrame(real, 0.45f, seed)[3] != real[3] })
    }

    @Test fun negativeAndNaNProgressBehaveLikeZero() {
        assertEquals(glitchFrame(real, 0f, 5), glitchFrame(real, -3f, 5))
        assertEquals(glitchFrame(real, 0f, 5), glitchFrame(real, Float.NaN, 5))
    }
}
