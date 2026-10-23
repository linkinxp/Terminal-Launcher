package dev.linkinxp.termlauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortNameTest {
    @Test fun stripsVowelsLikeTheReferenceDock() {
        assertEquals("phn", shortName("Phone"))
        assertEquals("msgs", shortName("Messages"))
        assertEquals("chrm", shortName("Chrome"))
        assertEquals("cmr", shortName("Camera"))
    }

    @Test fun usesOnlyTheFirstWordAndAtMostFiveLetters() {
        assertEquals("grmn", shortName("Garmin Connect"))
        assertEquals("sgnl", shortName("Signal"))
        assertEquals(5, shortName("Photographer").length)
    }

    @Test fun shortNamesAndOddLabelsAreKeptOrSafe() {
        assertEquals("hi", shortName("Hi"))
        assertEquals("x", shortName("X"))
        assertEquals("", shortName(""))
        assertEquals("日本語", shortName("日本語"))
        assertEquals("1pswr", shortName("1Password"))
    }
}
