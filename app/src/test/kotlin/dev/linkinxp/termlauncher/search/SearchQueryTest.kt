package dev.linkinxp.termlauncher.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryTest {
    @Test fun cleanQueryTrimsAndCollapsesWhitespace() {
        assertEquals("hello world", cleanQuery("  hello   world \n"))
        assertEquals("a b c", cleanQuery("a\nb\tc"))
    }

    @Test fun blankQueriesAreNull() {
        assertNull(cleanQuery(""))
        assertNull(cleanQuery("   "))
        assertNull(cleanQuery("\n\t"))
    }

    @Test fun veryLongQueriesAreCutToFiveHundredCharacters() {
        assertEquals(500, cleanQuery("x".repeat(10_000))!!.length)
    }

    @Test fun urlEncodesReservedCharacters() {
        assertEquals("https://www.google.com/search?q=cats+%26+dogs", searchUri("cats & dogs"))
        assertEquals("https://www.google.com/search?q=a%3Db%23c%25d%2Be%3Ff", searchUri("a=b#c%d+e?f"))
    }

    @Test fun urlEncodesNonAsciiAsUtf8() {
        assertEquals("https://www.google.com/search?q=S%C3%A3o+Paulo", searchUri("São Paulo"))
        assertEquals("https://www.google.com/search?q=%F0%9F%8E%89", searchUri("🎉"))
    }

    @Test fun blankQueryHasNoUri() {
        assertNull(searchUri("  "))
        assertNull(searchUri(""))
    }
}
