package dev.linkinxp.termlauncher.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeUpTest {
    @Test fun nothingIsShownAtTheStart() {
        assertEquals(listOf(""), wakeText(0))
        assertEquals(listOf(""), wakeText(-100))
    }

    @Test fun textIsTypedOneCharacterAtATime() {
        assertEquals(listOf("Wak"), wakeText(3L * WAKE_CHAR_MS))
        assertEquals(listOf("Wake up, Neo..."), wakeText(WAKE_LINES[0].length * WAKE_CHAR_MS.toLong()))
    }

    @Test fun nextLineStartsAfterAPause() {
        val firstDone = WAKE_LINES[0].length * WAKE_CHAR_MS.toLong()
        assertEquals(listOf(WAKE_LINES[0]), wakeText(firstDone + WAKE_PAUSE_MS - 1))
        assertEquals(listOf(WAKE_LINES[0], "T"), wakeText(firstDone + WAKE_PAUSE_MS + WAKE_CHAR_MS))
    }

    @Test fun theWholeScriptShowsAtTheEndAndStaysThere() {
        assertEquals(WAKE_LINES, wakeText(wakeDurationMs()))
        assertEquals(WAKE_LINES, wakeText(wakeDurationMs() + 60_000))
    }

    @Test fun theVisibleTextOnlyEverGrows() {
        var lastChars = 0
        for (t in 0..wakeDurationMs() step 25) {
            val n = wakeText(t).sumOf { it.length }
            assertTrue(n >= lastChars)
            lastChars = n
        }
    }

    @Test fun itIsTheClassicScript() {
        assertEquals("Wake up, Neo...", WAKE_LINES.first())
        assertTrue(WAKE_LINES.any { it.contains("white rabbit") })
        assertTrue(wakeDurationMs() in 5_000..15_000)
    }
}
