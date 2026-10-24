package dev.linkinxp.termlauncher.wall

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallSettingsTest {
    private val d = WallSettings()
    private fun apply(sub: String?, arg: String? = null, current: WallSettings = d) =
        applyWall(current, sub, arg, Random(1))

    @Test fun defaultsMatchTheSpec() {
        assertEquals(WallSettings(WallMode.SOLID, AsciiStyle.GLYPH, 1, 55, false), d)
    }

    @Test fun styleByNameIsCaseInsensitive() {
        assertEquals(AsciiStyle.CIRCUIT, apply("style", "circuit").getOrThrow().style)
        assertEquals(AsciiStyle.STARS, apply("STYLE", "Stars").getOrThrow().style)
    }

    @Test fun badOrMissingStyleListsTheSix() {
        val msg = apply("style", "nope").exceptionOrNull()?.message ?: ""
        AsciiStyle.entries.forEach { assertTrue("missing $it in $msg", msg.contains(it.name.lowercase())) }
        assertTrue(apply("style", null).isFailure)
    }

    @Test fun nextCyclesAndWraps() {
        assertEquals(AsciiStyle.CIRCUIT, apply("next").getOrThrow().style)
        assertEquals(AsciiStyle.GLYPH, apply("next", current = d.copy(style = AsciiStyle.SKYLINE)).getOrThrow().style)
    }

    @Test fun seedAcceptsAnyIntAndRejectsTheRest() {
        assertEquals(42, apply("seed", "42").getOrThrow().seed)
        assertEquals(-7, apply("seed", "-7").getOrThrow().seed)
        for (bad in listOf("99999999999", "x", "")) {
            assertEquals("usage: wall seed <number>", apply("seed", bad).exceptionOrNull()?.message)
        }
        assertEquals("usage: wall seed <number>", apply("seed", null).exceptionOrNull()?.message)
    }

    @Test fun modeByName() {
        assertEquals(WallMode.ASCII, apply("mode", "ascii").getOrThrow().mode)
        assertEquals(WallMode.SYSTEM, apply("mode", "SYSTEM").getOrThrow().mode)
        assertEquals(WallMode.SOLID, apply("mode", "solid", d.copy(mode = WallMode.ASCII)).getOrThrow().mode)
        val msg = apply("mode", "bad").exceptionOrNull()?.message ?: ""
        assertTrue(msg.contains("solid system ascii"))
        assertTrue(apply("mode", null).isFailure)
    }

    @Test fun dimIsLimitedToZeroToOneHundred() {
        assertEquals(0, apply("dim", "0").getOrThrow().dim)
        assertEquals(100, apply("dim", "100").getOrThrow().dim)
        for (bad in listOf("-1", "101", "x", "")) {
            assertEquals("usage: wall dim <0-100>", apply("dim", bad).exceptionOrNull()?.message)
        }
        assertEquals("usage: wall dim <0-100>", apply("dim", null).exceptionOrNull()?.message)
    }

    @Test fun animateOnOff() {
        assertTrue(apply("animate", "on").getOrThrow().animate)
        assertTrue(!apply("animate", "off", d.copy(animate = true)).getOrThrow().animate)
        assertTrue(apply("animate", "maybe").isFailure)
        assertTrue(apply("animate", null).isFailure)
    }

    @Test fun randomIsDeterministicForASeededRandomAndAlwaysChangesSomething() {
        assertEquals(apply("random").getOrThrow(), apply("random").getOrThrow())
        val next = apply("random").getOrThrow()
        assertTrue(next.style != d.style || next.seed != d.seed)
        // Even if the draw repeats the current values, something must still change.
        val cur = next
        val again = applyWall(cur, "random", null, Random(1)).getOrThrow()
        assertTrue(again.style != cur.style || again.seed != cur.seed)
        assertEquals(cur.mode, again.mode)
    }

    @Test fun unknownSubcommandFails() {
        assertEquals(
            "wall: bogus: unknown (random next style seed mode dim animate)",
            apply("bogus").exceptionOrNull()?.message,
        )
    }

    @Test fun bareWallReturnsTheCurrentSettings() {
        assertEquals(d, apply(null).getOrThrow())
    }

    @Test fun describeShowsEveryField() {
        val text = d.copy(mode = WallMode.ASCII, seed = 9, dim = 40, animate = true).describe()
        val lines = text.lines()
        listOf("mode", "style", "seed", "dim", "anim").forEach { key -> assertTrue(lines.any { it.startsWith(key) }) }
        assertTrue(text.contains("ascii") && text.contains("9") && text.contains("40") && text.contains("on"))
        assertNotEquals(d.describe(), text)
    }

    @Test fun rainIsAWallMode() {
        assertEquals(WallMode.RAIN, apply("mode", "rain").getOrThrow().mode)
        assertTrue(apply("mode", "bad").exceptionOrNull()?.message?.contains("rain") == true)
    }
}
