package dev.linkinxp.termlauncher.weather

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TIMEOUT_MS = 10_000
private const val MAX_BODY_BYTES = 1_000_000

/** Open-Meteo over HTTPS: only the city's coordinates (and its name for the one-time search) leave the phone. */
class WeatherRepository {

    /** The raw forecast JSON, only for an HTTP 200; any other outcome is a failure with a short message. */
    suspend fun fetch(lat: Double, lon: Double, fahrenheit: Boolean): Result<String> {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${String.format(Locale.ROOT, "%.4f", lat)}&longitude=${String.format(Locale.ROOT, "%.4f", lon)}" +
            "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m,is_day" +
            "&hourly=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min" +
            "&timezone=auto&forecast_days=2" + if (fahrenheit) "&temperature_unit=fahrenheit" else ""
        return get(url)
    }

    suspend fun geocode(name: String): Result<GeoResult> {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(name, "UTF-8")}" +
            "&count=1&language=en&format=json"
        return get(url).fold(onSuccess = { parseGeocode(it) }, onFailure = { Result.failure(it) })
    }

    private suspend fun get(url: String): Result<String> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
            }
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) return@withContext Result.failure(java.io.IOException("wx: HTTP $code"))
            val bytes = conn.inputStream.use { it.readNBytes(MAX_BODY_BYTES) }
            Result.success(String(bytes, Charsets.UTF_8))
        } catch (e: Exception) {
            Result.failure(java.io.IOException("wx: offline"))
        } finally {
            conn?.disconnect()
        }
    }
}
