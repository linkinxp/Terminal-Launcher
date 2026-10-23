package dev.linkinxp.termlauncher.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FortunesTest {
    @Test fun listIsLargeAndTidy() {
        assertTrue(fortunes.size >= 15)
        assertTrue(fortunes.all { it.isNotBlank() && it.length <= 80 })
    }

    @Test fun sameSeedGivesTheSameFortune() {
        assertEquals(fortune(7), fortune(7))
    }

    @Test fun negativeSeedsStayInRange() {
        assertTrue(fortune(-5) in fortunes)
        assertTrue(fortune(Int.MIN_VALUE) in fortunes)
    }

    @Test fun differentSeedsReachDifferentEntries() {
        assertTrue((0..30).map(::fortune).toSet().size > 1)
    }
}
