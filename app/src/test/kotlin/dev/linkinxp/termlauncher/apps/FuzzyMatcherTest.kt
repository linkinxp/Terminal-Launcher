package dev.linkinxp.termlauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {
    @Test fun prefixMatchFindsChrome() {
        assertEquals(listOf(1), fuzzyRank("chr", listOf("Camera", "Chrome", "Files")))
    }

    @Test fun prefixBeatsWordStartBeatsSubsequence() {
        val labels = listOf("Smart Tube", "Material Tool", "Mt Everest")
        // "Mt Everest" prefix, "Material Tool" word-start (M..T), "Smart Tube" subsequence
        assertEquals(listOf(2, 1, 0), fuzzyRank("mt", labels))
    }

    @Test fun matchingIsCaseInsensitive() {
        assertEquals(listOf(0), fuzzyRank("CHROME", listOf("chrome")))
    }

    @Test fun blankQueryReturnsAllInOrder() {
        assertEquals(listOf(0, 1, 2), fuzzyRank("  ", listOf("a", "b", "c")))
        assertEquals(listOf(0, 1, 2), fuzzyRank("", listOf("a", "b", "c")))
    }

    @Test fun trailingSpacesAreIgnored() {
        assertEquals(listOf(1), fuzzyRank("chr  ", listOf("Camera", "Chrome")))
    }

    @Test fun duplicateLabelsAreBothReturnedInOrder() {
        assertEquals(listOf(0, 1), fuzzyRank("a", listOf("A", "A")))
    }

    @Test fun nonLatinLabelsMatchByPrefixWithoutCrashing() {
        assertEquals(listOf(0), fuzzyRank("日本", listOf("日本語", "Files")))
        assertTrue(fuzzyRank("日本", listOf("Files")).isEmpty())
    }

    @Test fun noMatchReturnsEmpty() {
        assertTrue(fuzzyRank("zzz", listOf("Camera", "Chrome")).isEmpty())
    }

    @Test fun emptyLabelOnlyMatchesBlankQuery() {
        assertEquals(listOf(0), fuzzyRank("", listOf("")))
        assertTrue(fuzzyRank("a", listOf("")).isEmpty())
    }
}
