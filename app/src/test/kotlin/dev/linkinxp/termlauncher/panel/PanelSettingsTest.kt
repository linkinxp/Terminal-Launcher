package dev.linkinxp.termlauncher.panel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelSettingsTest {
    private val d = PanelSettings()

    @Test fun bothPanelsStartOff() {
        assertEquals(PanelSettings(cal = false, wx = false), d)
    }

    @Test fun bareCommandReturnsTheCurrentSettings() {
        assertEquals(d, applyPanel(d, null, null).getOrThrow())
    }

    @Test fun setsEachPanelCaseInsensitively() {
        assertEquals(PanelSettings(cal = true), applyPanel(d, "cal", true).getOrThrow())
        assertEquals(PanelSettings(wx = false), applyPanel(PanelSettings(wx = true), "WX", false).getOrThrow())
    }

    @Test fun unknownPanelListsTheValidOnes() {
        assertEquals("panel: x: no such panel (cal wx)", applyPanel(d, "x", true).exceptionOrNull()?.message)
    }

    @Test fun missingValueGivesUsage() {
        assertEquals("usage: panel <cal|wx> on|off", applyPanel(d, "cal", null).exceptionOrNull()?.message)
    }

    @Test fun describeHasALinePerPanel() {
        val lines = PanelSettings(cal = true).describe().lines()
        assertTrue(lines.any { it.startsWith("cal") && it.contains("on") })
        assertTrue(lines.any { it.startsWith("wx") && it.contains("off") })
    }
}
