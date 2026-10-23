package dev.linkinxp.termlauncher.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandHistoryTest {
    @Test fun blankLinesAreIgnored() {
        val h = CommandHistory()
        h.add(""); h.add("   ")
        assertTrue(h.all().isEmpty())
    }

    @Test fun consecutiveDuplicatesAreDroppedButSeparatedOnesKept() {
        val h = CommandHistory()
        listOf("a", "a", "b", "a").forEach(h::add)
        assertEquals(listOf("a", "b", "a"), h.all())
    }

    @Test fun lastIsNewestFirst() {
        val h = CommandHistory()
        listOf("a", "b", "c").forEach(h::add)
        assertEquals(listOf("c", "b"), h.last(2))
        assertEquals(listOf("c", "b", "a"), h.last(10))
    }

    @Test fun capsAtFiftyDroppingTheOldest() {
        val h = CommandHistory()
        (1..60).forEach { h.add("cmd$it") }
        assertEquals(50, h.all().size)
        assertEquals("cmd11", h.all().first())
        assertEquals("cmd60", h.all().last())
    }

    @Test fun encodeDecodeRoundTrips() {
        val h = CommandHistory()
        listOf("theme matrix", "ls -a").forEach(h::add)
        assertEquals(h.all(), CommandHistory.decode(h.encode()).all())
    }

    @Test fun decodeOfMissingOrBlankIsEmpty() {
        assertTrue(CommandHistory.decode(null).all().isEmpty())
        assertTrue(CommandHistory.decode("  ").all().isEmpty())
    }

    @Test fun decodeOfAnOversizedBlobKeepsTheNewestFifty() {
        val raw = (1..80).joinToString("\n") { "c$it" }
        val h = CommandHistory.decode(raw)
        assertEquals(50, h.all().size)
        assertEquals("c80", h.all().last())
    }
}
