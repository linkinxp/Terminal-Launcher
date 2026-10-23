package dev.linkinxp.termlauncher.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WxSettingsTest {
    @Test fun defaultsHaveNoCityAndCelsius() {
        val d = WxSettings()
        assertEquals(null, d.city)
        assertFalse(d.fahrenheit)
    }

    @Test fun bareWxRefreshes() {
        assertEquals(WxAction.Refresh, parseWxAction(null, null).getOrThrow())
    }

    @Test fun unitsAcceptCAndFInAnyCase() {
        assertEquals(WxAction.Units(true), parseWxAction("units", "f").getOrThrow())
        assertEquals(WxAction.Units(false), parseWxAction("UNITS", "C").getOrThrow())
    }

    @Test fun badUnitsGiveUsage() {
        assertEquals("usage: wx units c|f", parseWxAction("units", "x").exceptionOrNull()?.message)
        assertEquals("usage: wx units c|f", parseWxAction("units", null).exceptionOrNull()?.message)
    }

    @Test fun cityKeepsTheWholeName() {
        assertEquals(WxAction.City("São Paulo"), parseWxAction("city", "São Paulo").getOrThrow())
        assertEquals(WxAction.City("a&b=c"), parseWxAction("city", " a&b=c ").getOrThrow())
    }

    @Test fun missingCityGivesUsage() {
        assertEquals("usage: wx city <name>", parseWxAction("city", null).exceptionOrNull()?.message)
        assertEquals("usage: wx city <name>", parseWxAction("city", "  ").exceptionOrNull()?.message)
    }

    @Test fun unknownSubcommandFails() {
        assertEquals("wx: bogus: unknown (city units)", parseWxAction("bogus", null).exceptionOrNull()?.message)
    }
}
