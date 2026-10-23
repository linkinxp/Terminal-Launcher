package dev.linkinxp.termlauncher.weather

enum class Condition { CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, SHOWERS, THUNDER }

/** WMO weather code -> condition; anything unknown reads as cloudy. */
fun conditionOf(code: Int): Condition = when (code) {
    0 -> Condition.CLEAR
    1, 2 -> Condition.PARTLY
    3 -> Condition.CLOUDY
    45, 48 -> Condition.FOG
    in 51..57 -> Condition.DRIZZLE
    in 61..67 -> Condition.RAIN
    in 71..77, 85, 86 -> Condition.SNOW
    in 80..82 -> Condition.SHOWERS
    in 95..99 -> Condition.THUNDER
    else -> Condition.CLOUDY
}

fun conditionText(c: Condition): String = when (c) {
    Condition.CLEAR -> "clear"
    Condition.PARTLY -> "partly cloudy"
    Condition.CLOUDY -> "cloudy"
    Condition.FOG -> "fog"
    Condition.DRIZZLE -> "drizzle"
    Condition.RAIN -> "rain"
    Condition.SNOW -> "snow"
    Condition.SHOWERS -> "showers"
    Condition.THUNDER -> "thunderstorm"
}

private val CLOUD = listOf("   .--.    ", " .-(    ). ", "(___.__)__)")

private val SUN = listOf("  \\ | /   ", "-- (   ) --", "  / | \\   ", "          ", "          ")
private val MOON = listOf("    _..    ", "  .'  *  . ", " :   (    :", "  `.   _.' ", "    ``     ")

/** Five lines (at most 12 columns, printable ASCII) of art for [c]; CLEAR and PARTLY differ at night. */
fun asciiIcon(c: Condition, isDay: Boolean): List<String> = when (c) {
    Condition.CLEAR -> if (isDay) SUN else MOON
    Condition.PARTLY -> if (isDay) {
        listOf("  \\ | /    ", " -- O .--.  ", "   .-(    ).", "  (___.__)_)", "            ")
    } else {
        listOf("   _..      ", "  ( * .--.  ", "   .-(    ).", "  (___.__)_)", "            ")
    }
    Condition.CLOUDY -> listOf("            ") + CLOUD + listOf("            ")
    Condition.FOG -> listOf(" _ - _ - _  ", "  _ - _ - _ ", " _ - _ - _  ", "  _ - _ - _ ", " _ - _ - _  ")
    Condition.DRIZZLE -> CLOUD + listOf("  .  .  .   ", "   .  .  .  ")
    Condition.RAIN -> CLOUD + listOf("  ' ' ' '   ", "  ' ' ' '   ")
    Condition.SHOWERS -> CLOUD + listOf(" ' , ' , '  ", "  ,  ' ,  ' ")
    Condition.SNOW -> CLOUD + listOf("  *  *  *   ", "   *  *  *  ")
    Condition.THUNDER -> CLOUD + listOf("  ,'|  ,'|  ", "   '    '   ")
}
