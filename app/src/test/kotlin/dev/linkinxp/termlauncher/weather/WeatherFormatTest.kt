package dev.linkinxp.termlauncher.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherFormatTest {
    private val forecast = Forecast(
        temp = 10.0, humidity = 93, code = 1, windKmh = 3.2, windDeg = 135, isDay = false, high = 13.2, low = 6.0,
        hours = listOf(HourPoint(0, 8.6, 1), HourPoint(3, 7.0, 3), HourPoint(6, 6.4, 3), HourPoint(9, 8.0, 0), HourPoint(12, 12.0, 0), HourPoint(15, 13.0, 0)),
        fetchedAtMs = 1_000_000L,
    )
    private fun text(f: Forecast = forecast, city: String = "Zagreb", fahrenheit: Boolean = false, ageMs: Long = 0) =
        formatWeather(city, f, fahrenheit, f.fetchedAtMs + ageMs)

    @Test fun compassPoints() {
        val expected = mapOf(0 to "N", 45 to "NE", 90 to "E", 135 to "SE", 180 to "S", 225 to "SW", 270 to "W", 315 to "NW", 360 to "N", -45 to "NW", 22 to "N", 23 to "NE")
        expected.forEach { (deg, name) -> assertEquals("deg $deg", name, compass(deg)) }
    }

    @Test fun firstLineIsTheLowercaseCity() {
        assertEquals("zagreb", text().first())
    }

    @Test fun showsTemperatureConditionWindHumidityAndRange() {
        val all = text().joinToString("\n")
        assertTrue(all.contains("10°C partly cloudy"))
        assertTrue(all.contains("wind SE 3 km/h"))
        assertTrue(all.contains("humid 93%"))
        assertTrue(all.contains("hi 13° lo 6°"))
    }

    @Test fun fahrenheitUsesTheFSuffix() {
        assertTrue(text(fahrenheit = true).joinToString("\n").contains("10°F"))
    }

    @Test fun hourStripShowsRoundedTempsAndStaysWithinFortyColumns() {
        val strip = text().first { it.startsWith("00 ") }
        assertTrue(strip.startsWith("00 9°  03 7°"))
        assertTrue(strip.length <= 40)
    }

    @Test fun updatedLineTracksTheAge() {
        assertEquals("updated just now", text(ageMs = 0).last())
        assertEquals("updated just now", text(ageMs = 59_000).last())
        assertEquals("updated 1m ago", text(ageMs = 60_000).last())
        assertEquals("updated 12m ago", text(ageMs = 12 * 60_000L).last())
        assertEquals("updated 3h ago", text(ageMs = 3 * 3_600_000L).last())
        assertEquals("updated 2d ago", text(ageMs = 2 * 86_400_000L).last())
    }

    @Test fun everyLineFitsFortyColumnsEvenForALongCity() {
        assertTrue(text(city = "C".repeat(60)).all { it.length <= 40 })
        assertTrue(text().all { it.length <= 40 })
    }
}
