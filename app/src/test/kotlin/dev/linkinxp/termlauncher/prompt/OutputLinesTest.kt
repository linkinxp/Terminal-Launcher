package dev.linkinxp.termlauncher.prompt

import org.junit.Assert.assertEquals
import org.junit.Test

class OutputLinesTest {
    @Test fun shortTextIsUnchanged() {
        assertEquals("a\nb\nc", capLines("a\nb\nc"))
    }

    @Test fun keepsOnlyTheLastTwelveLines() {
        val text = (1..20).joinToString("\n") { "l$it" }
        val out = capLines(text).lines()
        assertEquals(12, out.size)
        assertEquals("l9", out.first())
        assertEquals("l20", out.last())
    }

    @Test fun blankTextIsEmpty() {
        assertEquals("", capLines(""))
        assertEquals("", capLines("  \n "))
    }

    @Test fun trailingNewlineDoesNotAddABlankLine() {
        assertEquals("a\nb", capLines("a\nb\n"))
    }

    @Test fun honorsACustomLimit() {
        assertEquals("c\nd", capLines("a\nb\nc\nd", max = 2))
    }
}
