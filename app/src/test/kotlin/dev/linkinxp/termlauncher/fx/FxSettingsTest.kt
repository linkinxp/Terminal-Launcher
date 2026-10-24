package dev.linkinxp.termlauncher.fx

import dev.linkinxp.termlauncher.theme.ThemeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FxSettingsTest {
    @Test fun defaultsMatchTheSpec() {
        val d = FxSettings()
        assertNull(d.scanlines)
        assertFalse(d.flicker)
        assertTrue(d.glitch)
        assertTrue(d.cursor)
        assertTrue(d.boot)
    }

    @Test fun withChangesOnlyTheNamedEffect() {
        val s = FxSettings().with("glitch", false).getOrThrow()
        assertEquals(FxSettings(glitch = false), s)
    }

    @Test fun withIsCaseInsensitive() {
        assertEquals(true, FxSettings().with("SCANLINES", true).getOrThrow().scanlines)
    }

    @Test fun withUnknownNameFailsWithTheListOfEffects() {
        val r = FxSettings().with("nope", true)
        assertEquals("fx: nope: no such effect (scanlines flicker glitch cursor boot)", r.exceptionOrNull()?.message)
    }

    @Test fun scanlinesFollowTheThemeUntilSetExplicitly() {
        val cyber = ThemeRegistry.byName("neon-city")!!
        val plain = ThemeRegistry.byName("matrix")!!
        assertTrue(FxSettings().scanlinesOn(cyber))
        assertFalse(FxSettings().scanlinesOn(plain))
        assertFalse(FxSettings(scanlines = false).scanlinesOn(cyber))
        assertTrue(FxSettings(scanlines = true).scanlinesOn(plain))
    }

    @Test fun describeMentionsEveryEffect() {
        val text = FxSettings().describe(ThemeRegistry.default)
        listOf("scanlines", "flicker", "glitch", "cursor", "boot").forEach { assertTrue(text.contains(it)) }
    }

    @Test fun withScanlinesSetsAndClearsTheExplicitValue() {
        assertEquals(true, FxSettings().withScanlines(true).scanlines)
        assertEquals(false, FxSettings().withScanlines(false).scanlines)
        assertEquals(null, FxSettings(scanlines = true).withScanlines(null).scanlines)
    }
}
