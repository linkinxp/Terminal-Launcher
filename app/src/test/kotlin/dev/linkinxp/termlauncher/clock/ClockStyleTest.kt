package dev.linkinxp.termlauncher.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockStyleTest {
    private val usage = "usage: clock style pixel|segment|figlet|binary|hex|epoch|plain"

    @Test fun everyNameParsesInAnyCase() {
        ClockStyle.entries.forEach { s ->
            assertEquals(s, parseClockStyle(s.name.lowercase()).getOrThrow())
            assertEquals(s, parseClockStyle(" ${s.name} ").getOrThrow())
        }
        assertEquals(ClockStyle.FIGLET, parseClockStyle("FigLet").getOrThrow())
    }

    @Test fun badValuesGiveUsage() {
        listOf("nope", "", "  ", null, "pixel segment").forEach {
            assertEquals(usage, parseClockStyle(it).exceptionOrNull()?.message)
        }
    }

    @Test fun storedNamesDecodeAndUnknownOnesFallBackToPixel() {
        assertEquals(ClockStyle.EPOCH, ClockStyle.fromName("EPOCH"))
        assertEquals(ClockStyle.PIXEL, ClockStyle.fromName("nonsense"))
        assertEquals(ClockStyle.PIXEL, ClockStyle.fromName(null))
    }

    @Test fun theOrderIsTheCycleOrder() {
        assertEquals(listOf("PIXEL", "SEGMENT", "FIGLET", "BINARY", "HEX", "EPOCH", "PLAIN"), ClockStyle.entries.map { it.name })
    }

    @Test fun styleReplyShowsSetsAndRejects() {
        assertEquals(ClockStyleReply(null, "style   pixel"), clockStyleReply(null, ClockStyle.PIXEL))
        assertEquals(ClockStyleReply(ClockStyle.HEX, "clock style: hex"), clockStyleReply("hex", ClockStyle.PIXEL))
        assertEquals(ClockStyleReply(null, usage), clockStyleReply("nope", ClockStyle.PIXEL))
    }

    @Test fun styleSubcommandWithoutANameShowsTheUsage() {
        assertEquals(ClockStyleReply(null, usage), clockStyleCommand(null, ClockStyle.HEX))
        assertEquals(ClockStyleReply(ClockStyle.HEX, "clock style: hex"), clockStyleCommand("hex", ClockStyle.PIXEL))
    }
}
