package dev.linkinxp.termlauncher.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRegistryTest {
    @Test fun allHasTheNineSpecThemesInOrder() {
        assertEquals(
            listOf(
                "classy-black", "matrix", "amber", "solarized-dark", "dracula",
                "neon-city", "synthwave", "blade-runner", "glitch-red",
            ),
            ThemeRegistry.all.map { it.name },
        )
    }

    @Test fun byNameIsCaseInsensitive() {
        assertEquals("matrix", ThemeRegistry.byName("MATRIX")?.name)
    }

    @Test fun byNameUnknownIsNull() {
        assertNull(ThemeRegistry.byName("nope"))
    }

    @Test fun defaultIsClassyBlackOnPureBlack() {
        assertEquals("classy-black", ThemeRegistry.default.name)
        assertEquals(0xFF000000L, ThemeRegistry.default.background)
    }

    @Test fun namesAreUnique() {
        assertEquals(ThemeRegistry.all.size, ThemeRegistry.all.map { it.name }.toSet().size)
    }

    @Test fun everyBorderStyleIsUsed() {
        assertEquals(BorderStyle.entries.toSet(), ThemeRegistry.all.map { it.border }.toSet())
        assertTrue(ThemeRegistry.all.all { it.background ushr 24 == 0xFFL })
    }

    @Test fun newThemesUseSpecColors() {
        val neon = ThemeRegistry.byName("neon-city")!!
        assertEquals(0xFF0B0014L, neon.background)
        assertEquals(0xFF00F0FFL, neon.foreground)
        assertEquals(0xFFFF2BD6L, neon.accent)
        assertEquals(BorderStyle.BRACKET, neon.border)
        assertEquals(0xFFFF3B3BL, ThemeRegistry.byName("glitch-red")!!.foreground)
        assertEquals(BorderStyle.DOUBLE, ThemeRegistry.byName("blade-runner")!!.border)
        assertEquals(BorderStyle.ROUNDED, ThemeRegistry.byName("synthwave")!!.border)
    }

    @Test fun onlyTheNewThemesAreCyber() {
        assertEquals(
            listOf("neon-city", "synthwave", "blade-runner", "glitch-red"),
            ThemeRegistry.all.filter { it.cyber }.map { it.name },
        )
    }

    @Test fun describeThemesMarksOnlyTheCurrentOne() {
        val lines = describeThemes("matrix").lines()
        assertEquals(9, lines.size)
        assertEquals(listOf("* matrix"), lines.filter { it.startsWith("*") })
        assertTrue(lines.filterNot { it.startsWith("*") }.all { it.startsWith("  ") })
        assertEquals(ThemeRegistry.all.map { it.name }, lines.map { it.drop(2) })
    }
}
