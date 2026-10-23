package dev.linkinxp.termlauncher.sys

import dev.linkinxp.termlauncher.ui.pageLabel
import dev.linkinxp.termlauncher.ui.statusTail
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    private val empty = "[----------]"

    @Test fun asciiBarFillsProportionally() {
        assertEquals("[######----]", asciiBar(0.62f))
        assertEquals(empty, asciiBar(0f))
        assertEquals("[##########]", asciiBar(1f))
        assertEquals("[##--]", asciiBar(0.5f, 4))
    }

    @Test fun asciiBarClampsAndSurvivesNaN() {
        assertEquals(empty, asciiBar(-1f))
        assertEquals(empty, asciiBar(Float.NaN))
        assertEquals("[##########]", asciiBar(2f))
    }

    @Test fun formatUptimePicksTheLargestTwoUnits() {
        assertEquals("0m", formatUptime(0))
        assertEquals("0m", formatUptime(59_999))
        assertEquals("12m", formatUptime(12 * 60_000L))
        assertEquals("4h 12m", formatUptime((4 * 60 + 12) * 60_000L))
        assertEquals("3d 4h", formatUptime((3 * 24 + 4) * 3_600_000L))
        assertEquals("0m", formatUptime(-5))
    }

    @Test fun pageLabelsNameTheFirstFivePages() {
        assertEquals("1:home", pageLabel(0))
        assertEquals("2:two", pageLabel(1))
        assertEquals("5:five", pageLabel(4))
        assertEquals("8", pageLabel(7))
    }

    @Test fun statusTailShowsBatteryAndTime() {
        assertEquals("bat 66% │ 21:08", statusTail(66, false, 21, 8))
        assertEquals("bat 66%+ │ 09:05", statusTail(66, true, 9, 5))
        assertEquals("bat ? │ 00:00", statusTail(null, false, 0, 0))
    }
}
