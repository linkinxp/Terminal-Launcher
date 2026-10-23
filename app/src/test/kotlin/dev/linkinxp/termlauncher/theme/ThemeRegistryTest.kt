package dev.linkinxp.termlauncher.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRegistryTest {
    @Test fun allHasTheFiveSpecThemesInOrder() {
        assertEquals(
            listOf("classy-black", "matrix", "amber", "solarized-dark", "dracula"),
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
}
