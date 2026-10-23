package dev.linkinxp.termlauncher.ui

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ClockDigitsTest {
    private val saved = Locale.getDefault()
    @Before fun nonLatinDigitsLocale() = Locale.setDefault(Locale.forLanguageTag("ar-EG"))
    @After fun restore() = Locale.setDefault(saved)

    @Test fun digitsAreAlwaysZeroToNineRegardlessOfLocale() {
        assertEquals(listOf(0, 2, 0, 5), clockDigits(2, 5))
        assertEquals(listOf(2, 3, 5, 9), clockDigits(23, 59))
        assertEquals(listOf(0, 0, 0, 0), clockDigits(0, 0))
    }
}
