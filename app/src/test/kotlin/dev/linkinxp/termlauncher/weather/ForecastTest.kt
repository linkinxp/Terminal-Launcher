package dev.linkinxp.termlauncher.weather

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastTest {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    /** An Open-Meteo style body: hourly entry i has temp i.0 and code i % 4. */
    private fun sample(
        current: String = """{"time":"2026-10-23T21:00","interval":900,"temperature_2m":10.0,"relative_humidity_2m":93,"weather_code":1,"wind_speed_10m":3.2,"wind_direction_10m":135,"is_day":0}""",
        hourlyCount: Int = 48,
        hourlyTemps: (Int) -> String = { "$it.0" },
        daily: String = """{"time":["2026-10-23","2026-10-24"],"temperature_2m_max":[13.2,12.1],"temperature_2m_min":[6.0,7.0]}""",
    ): String {
        val start = LocalDateTime.of(2026, 10, 23, 0, 0)
        val times = (0 until hourlyCount).joinToString(",") { "\"${start.plusHours(it.toLong()).format(fmt)}\"" }
        val temps = (0 until hourlyCount).joinToString(",") { hourlyTemps(it) }
        val codes = (0 until hourlyCount).joinToString(",") { "${it % 4}" }
        return """{"latitude":45.8,"longitude":15.97,"timezone":"Europe/Zagreb","current":$current,""" +
            """"hourly":{"time":[$times],"temperature_2m":[$temps],"weather_code":[$codes]},"daily":$daily}"""
    }

    @Test fun parsesEveryCurrentField() {
        val f = parseForecast(sample(), 123L).getOrThrow()
        assertEquals(10.0, f.temp, 0.0)
        assertEquals(93, f.humidity)
        assertEquals(1, f.code)
        assertEquals(3.2, f.windKmh, 0.0)
        assertEquals(135, f.windDeg)
        assertFalse(f.isDay)
        assertEquals(13.2, f.high, 0.0)
        assertEquals(6.0, f.low, 0.0)
        assertEquals(123L, f.fetchedAtMs)
    }

    @Test fun hoursAreSixPointsThreeHoursApartAfterNow() {
        val f = parseForecast(sample(), 0L).getOrThrow()
        assertEquals(listOf(0, 3, 6, 9, 12, 15), f.hours.map { it.hour })
        assertEquals(listOf(24.0, 27.0, 30.0, 33.0, 36.0, 39.0), f.hours.map { it.temp })
        assertEquals(24 % 4, f.hours.first().code)
    }

    @Test fun currentTimeWithMinutesStillFindsTheHour() {
        val f = parseForecast(sample(current = """{"time":"2026-10-23T21:15","temperature_2m":10.0,"relative_humidity_2m":93,"weather_code":1,"wind_speed_10m":3.2,"wind_direction_10m":135,"is_day":1}"""), 0L).getOrThrow()
        assertTrue(f.isDay)
        assertEquals(6, f.hours.size)
    }

    @Test fun fewerThanEighteenFutureHoursGivesFewerPoints() {
        val f = parseForecast(sample(hourlyCount = 30), 0L).getOrThrow()
        assertEquals(listOf(0, 3), f.hours.map { it.hour })
    }

    @Test fun nullHourlyValuesAreSkippedNotFatal() {
        val f = parseForecast(sample(hourlyTemps = { if (it == 27) "null" else "$it.0" }), 0L).getOrThrow()
        assertEquals(listOf(0, 6, 9, 12, 15), f.hours.map { it.hour })
    }

    @Test fun badBodiesFailWithoutThrowing() {
        val noCurrent = """{"hourly":{"time":[],"temperature_2m":[],"weather_code":[]},"daily":{"temperature_2m_max":[1],"temperature_2m_min":[0]}}"""
        val nullTemp = sample(current = """{"time":"2026-10-23T21:00","temperature_2m":null,"relative_humidity_2m":93,"weather_code":1,"wind_speed_10m":3.2,"wind_direction_10m":135,"is_day":0}""")
        val noDaily = sample(daily = "{}")
        for (body in listOf(noCurrent, nullTemp, noDaily, "not json", "<html><body>502 Bad Gateway</body></html>", "", "[]", "null")) {
            assertTrue("should fail: ${body.take(30)}", parseForecast(body, 0L).isFailure)
        }
    }

    @Test fun geocodeReadsTheFirstResult() {
        val json = """{"results":[{"id":3186886,"name":"Zagreb","latitude":45.81444,"longitude":15.97798,"country_code":"HR"}]}"""
        assertEquals(GeoResult("Zagreb", 45.81444, 15.97798), parseGeocode(json).getOrThrow())
    }

    @Test fun geocodeWithoutResultsIsNotFound() {
        assertEquals("not found", parseGeocode("""{"results":[]}""").exceptionOrNull()?.message)
        assertEquals("not found", parseGeocode("{}").exceptionOrNull()?.message)
        assertTrue(parseGeocode("garbage").isFailure)
    }
}
