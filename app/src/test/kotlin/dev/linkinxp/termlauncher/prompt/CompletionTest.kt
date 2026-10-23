package dev.linkinxp.termlauncher.prompt

import org.junit.Assert.assertEquals
import org.junit.Test

class CompletionTest {
    private val apps = listOf("chrome", "chrm", "camera")

    @Test fun completesToTheLongestCommonPrefix() {
        assertEquals("chr", complete("ch", apps))
    }

    @Test fun uniqueMatchCompletesFully() {
        assertEquals("camera", complete("cam", apps))
    }

    @Test fun matchingIsCaseInsensitiveAndUsesTheCandidateCase() {
        assertEquals("chrome", complete("CH", listOf("chrome")))
    }

    @Test fun noLongerPrefixLeavesTheInputAlone() {
        assertEquals("chr", complete("chr", listOf("chrome", "chrm")))
    }

    @Test fun blankInputIsReturnedAsGiven() {
        assertEquals("", complete("", apps))
        assertEquals("  ", complete("  ", apps))
    }

    @Test fun noMatchLeavesTheInputAlone() {
        assertEquals("zzz", complete("zzz", apps))
    }

    @Test fun multiWordCandidates() {
        assertEquals("garmin connect", complete("garmin c", listOf("garmin connect")))
    }

    @Test fun exactMatchStaysAsIs() {
        assertEquals("chrome", complete("chrome", listOf("chrome")))
    }

    @Test fun neverShortensWhatTheUserTyped() {
        assertEquals("chromium", complete("chromium", listOf("chrome")))
    }
}
