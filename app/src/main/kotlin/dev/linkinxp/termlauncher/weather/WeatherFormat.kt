package dev.linkinxp.termlauncher.weather

import kotlin.math.roundToInt

private const val MAX_COLS = 40
private const val ICON_COLS = 12

/** 8-point compass name of a bearing in degrees (any integer, negative or above 360 wraps). */
fun compass(deg: Int): String {
    val names = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return names[Math.floorMod(((deg + 22.5) / 45).toInt().let { if (deg + 22.5 < 0) it - 1 else it }, 8)]
}

/**
 * The weather panel body: city, then art on the left with temperature/condition, wind, humidity and high/low
 * on the right, the 3-hour strip, and how old the data is. Every line is at most 40 columns.
 */
fun formatWeather(city: String, f: Forecast, fahrenheit: Boolean, nowMs: Long): List<String> {
    val unit = if (fahrenheit) "F" else "C"
    val info = listOf(
        "${f.temp.roundToInt()}°$unit ${conditionText(conditionOf(f.code))}",
        "wind ${compass(f.windDeg)} ${f.windKmh.roundToInt()} km/h",
        "humid ${f.humidity}%",
        "hi ${f.high.roundToInt()}° lo ${f.low.roundToInt()}°",
        "",
    )
    val icon = asciiIcon(conditionOf(f.code), f.isDay)
    val out = mutableListOf(city.lowercase().take(MAX_COLS))
    icon.indices.forEach { i -> out += (icon[i].padEnd(ICON_COLS) + " " + info[i]).trimEnd().take(MAX_COLS) }
    strip(f.hours)?.let { out += it }
    out += "updated " + age(nowMs - f.fetchedAtMs)
    return out
}

private fun strip(hours: List<HourPoint>): String? {
    if (hours.isEmpty()) return null
    var line = ""
    for (p in hours) {
        val part = String.format(java.util.Locale.ROOT, "%02d %d°", p.hour, p.temp.roundToInt())
        val next = if (line.isEmpty()) part else "$line  $part"
        if (next.length > MAX_COLS) break
        line = next
    }
    return line.ifEmpty { null }
}

private fun age(ms: Long): String = when {
    ms < 60_000 -> "just now"
    ms < 3_600_000 -> "${ms / 60_000}m ago"
    ms < 86_400_000 -> "${ms / 3_600_000}h ago"
    else -> "${ms / 86_400_000}d ago"
}
