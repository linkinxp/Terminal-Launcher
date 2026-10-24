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
            ThemeRegistry.all.map { it.name }.take(9),
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
        assertEquals(15, lines.size)
        assertEquals(listOf("* matrix"), lines.filter { it.startsWith("*") })
        assertTrue(lines.filterNot { it.startsWith("*") }.all { it.startsWith("  ") })
        assertEquals(ThemeRegistry.all.map { it.name }, lines.map { it.drop(2) })
    }

    @Test fun thePopularPalettesFollowTheNineOriginalsWithTheirExactColors() {
        val names = ThemeRegistry.all.map { it.name }
        assertEquals(15, names.size)
        assertEquals(listOf("gruvbox", "nord", "tokyo-night", "catppuccin", "monokai", "one-dark"), names.drop(9))
        fun check(n: String, bg: Long, fg: Long, accent: Long, dim: Long, border: BorderStyle) {
            val t = ThemeRegistry.byName(n)!!
            assertEquals(listOf(bg, fg, accent, dim, border), listOf(t.background, t.foreground, t.accent, t.dim, t.border))
            assertEquals(false, t.cyber); assertEquals(false, t.glow)
        }
        check("gruvbox", 0xFF282828, 0xFFEBDBB2, 0xFFFE8019, 0xFF928374, BorderStyle.SINGLE)
        check("nord", 0xFF2E3440, 0xFFD8DEE9, 0xFF88C0D0, 0xFF4C566A, BorderStyle.ROUNDED)
        check("tokyo-night", 0xFF1A1B26, 0xFFC0CAF5, 0xFF7AA2F7, 0xFF565F89, BorderStyle.ROUNDED)
        check("catppuccin", 0xFF1E1E2E, 0xFFCDD6F4, 0xFFCBA6F7, 0xFF6C7086, BorderStyle.ROUNDED)
        check("monokai", 0xFF272822, 0xFFF8F8F2, 0xFFA6E22E, 0xFF75715E, BorderStyle.DOUBLE)
        check("one-dark", 0xFF282C34, 0xFFABB2BF, 0xFF61AFEF, 0xFF5C6370, BorderStyle.SINGLE)
        assertTrue(names.all { it == it.lowercase() })
        assertEquals(15, describeThemes("nord").lines().size)
    }
}
