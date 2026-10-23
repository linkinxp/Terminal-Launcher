package dev.linkinxp.termlauncher.weather

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class HourPoint(val hour: Int, val temp: Double, val code: Int)

data class Forecast(
    val temp: Double,
    val humidity: Int,
    val code: Int,
    val windKmh: Double,
    val windDeg: Int,
    val isDay: Boolean,
    val high: Double,
    val low: Double,
    val hours: List<HourPoint>,
    val fetchedAtMs: Long,
)

data class GeoResult(val name: String, val lat: Double, val lon: Double)

private const val STRIP_POINTS = 6
private const val STRIP_STEP_HOURS = 3

/**
 * Parses an Open-Meteo forecast body. Anything missing or malformed in the current/daily blocks fails (never
 * throws); `hours` are the next [STRIP_POINTS] points every 3 hours after `current.time`, skipping null entries.
 */
fun parseForecast(json: String, fetchedAtMs: Long): Result<Forecast> = try {
    val root = Json.parseToJsonElement(json).jsonObject
    val cur = root.obj("current")
    val daily = root.obj("daily")
    Result.success(
        Forecast(
            temp = cur.double("temperature_2m"),
            humidity = cur.int("relative_humidity_2m"),
            code = cur.int("weather_code"),
            windKmh = cur.double("wind_speed_10m"),
            windDeg = cur.int("wind_direction_10m"),
            isDay = cur.int("is_day") == 1,
            high = daily.arr("temperature_2m_max").first().jsonPrimitive.doubleOrNull ?: error("high"),
            low = daily.arr("temperature_2m_min").first().jsonPrimitive.doubleOrNull ?: error("low"),
            hours = hourPoints(root, cur),
            fetchedAtMs = fetchedAtMs,
        ),
    )
} catch (e: Exception) {
    Result.failure(IllegalArgumentException("wx: bad forecast data"))
}

private fun hourPoints(root: JsonObject, cur: JsonObject): List<HourPoint> {
    val hourly = root["hourly"]?.jsonObject ?: return emptyList()
    val times = hourly["time"]?.jsonArray ?: return emptyList()
    val temps = hourly["temperature_2m"]?.jsonArray ?: return emptyList()
    val codes = hourly["weather_code"]?.jsonArray ?: return emptyList()
    val now = cur["time"]?.jsonPrimitive?.content ?: return emptyList()
    val index = times.indexOfFirst { it.jsonPrimitive.content == now.take(13) + ":00" }
    if (index < 0) return emptyList()
    return (1..STRIP_POINTS).mapNotNull { k ->
        val i = index + k * STRIP_STEP_HOURS
        if (i >= times.size || i >= temps.size || i >= codes.size) return@mapNotNull null
        val temp = temps[i].jsonPrimitive.doubleOrNull ?: return@mapNotNull null
        val code = codes[i].jsonPrimitive.intOrNull ?: return@mapNotNull null
        val hour = times[i].jsonPrimitive.content.substring(11, 13).toIntOrNull() ?: return@mapNotNull null
        HourPoint(hour, temp, code)
    }
}

private fun JsonObject.obj(key: String): JsonObject = this[key]?.jsonObject ?: error(key)
private fun JsonObject.arr(key: String): JsonArray = this[key]?.jsonArray ?: error(key)
private fun JsonObject.double(key: String): Double = this[key]?.jsonPrimitive?.doubleOrNull ?: error(key)
private fun JsonObject.int(key: String): Int = this[key]?.jsonPrimitive?.intOrNull ?: error(key)

/** First result of an Open-Meteo geocoding search, or a failure with the message `not found`. */
fun parseGeocode(json: String): Result<GeoResult> = try {
    val first = Json.parseToJsonElement(json).jsonObject["results"]?.jsonArray?.firstOrNull()?.jsonObject
    if (first == null) {
        Result.failure(IllegalArgumentException("not found"))
    } else {
        Result.success(GeoResult(first.string("name"), first.double("latitude"), first.double("longitude")))
    }
} catch (e: Exception) {
    Result.failure(IllegalArgumentException("wx: bad geocoding data"))
}

private fun JsonObject.string(key: String): String = this[key]?.jsonPrimitive?.content ?: error(key)
