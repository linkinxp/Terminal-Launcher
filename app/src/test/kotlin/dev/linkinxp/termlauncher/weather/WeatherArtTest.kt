package dev.linkinxp.termlauncher.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherArtTest {
    @Test fun wmoCodesMapToConditions() {
        val expected = mapOf(
            0 to Condition.CLEAR, 1 to Condition.PARTLY, 2 to Condition.PARTLY, 3 to Condition.CLOUDY,
            45 to Condition.FOG, 48 to Condition.FOG,
            51 to Condition.DRIZZLE, 55 to Condition.DRIZZLE, 57 to Condition.DRIZZLE,
            61 to Condition.RAIN, 65 to Condition.RAIN, 67 to Condition.RAIN,
            71 to Condition.SNOW, 75 to Condition.SNOW, 77 to Condition.SNOW, 85 to Condition.SNOW, 86 to Condition.SNOW,
            80 to Condition.SHOWERS, 82 to Condition.SHOWERS,
            95 to Condition.THUNDER, 96 to Condition.THUNDER, 99 to Condition.THUNDER,
        )
        expected.forEach { (code, cond) -> assertEquals("code $code", cond, conditionOf(code)) }
    }

    @Test fun unknownCodesAreCloudy() {
        assertEquals(Condition.CLOUDY, conditionOf(-1))
        assertEquals(Condition.CLOUDY, conditionOf(1000))
    }

    @Test fun everyIconIsFiveShortPrintableLines() {
        for (c in Condition.entries) for (day in listOf(true, false)) {
            val icon = asciiIcon(c, day)
            assertEquals("$c day=$day lines", 5, icon.size)
            icon.forEach { line ->
                assertTrue("$c width ${line.length}", line.length <= 12)
                assertTrue("$c printable", line.all { it.code in 0x20..0x7E })
            }
        }
    }

    @Test fun clearAndPartlyLookDifferentAtNight() {
        assertNotEquals(asciiIcon(Condition.CLEAR, true), asciiIcon(Condition.CLEAR, false))
        assertNotEquals(asciiIcon(Condition.PARTLY, true), asciiIcon(Condition.PARTLY, false))
    }

    @Test fun conditionTextIsNeverBlank() {
        Condition.entries.forEach { assertTrue(conditionText(it).isNotBlank()) }
    }
}
