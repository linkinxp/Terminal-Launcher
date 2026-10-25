package dev.linkinxp.termlauncher.layout

import dev.linkinxp.termlauncher.clock.dateLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class UxTextTest {
    private val full = (1..5).fold(HomeLayout.default()) { l, i -> l.pin("p$i") }

    @Test fun fullDockSaysSoAndHowToFixIt() =
        assertEquals("dock is full (5): unpin one first", full.pinProblem("p6", "Maps"))

    @Test fun alreadyPinnedSaysThat() =
        assertEquals("maps is already in the dock", full.pinProblem("p1", "Maps"))

    @Test fun dateLineFollowsTheLanguage() {
        val d = LocalDate.of(2026, 10, 26)
        assertEquals("mon 26 oct 2026  wk 44  day 299", dateLine(d, Locale.ENGLISH))
        val fr = dateLine(d, Locale.FRENCH)
        assertTrue(fr, fr.startsWith("lun") && fr != dateLine(d, Locale.ENGLISH))
    }
}
