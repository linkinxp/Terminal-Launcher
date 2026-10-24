package dev.linkinxp.termlauncher.clock

import java.util.Locale

/** How hours are shown: follow the phone's own setting, or force 24 / 12 hour. */
enum class ClockFormat {
    AUTO, H24, H12;

    fun is24(systemIs24: Boolean): Boolean = when (this) { AUTO -> systemIs24; H24 -> true; H12 -> false }

    companion object {
        fun fromName(name: String?): ClockFormat = entries.firstOrNull { it.name == name } ?: AUTO
    }
}

/** An empty digit slot: the leading hour digit of 1-9 o'clock in 12-hour mode. */
const val BLANK = -1

private const val USAGE = "usage: clock auto|24|12"

fun parseClockFormat(arg: String?): Result<ClockFormat> = when (arg?.trim()?.lowercase()) {
    "auto" -> Result.success(ClockFormat.AUTO)
    "24", "24h" -> Result.success(ClockFormat.H24)
    "12", "12h" -> Result.success(ClockFormat.H12)
    else -> Result.failure(IllegalArgumentException(USAGE))
}

/** The four clock digits as 0-9 (or [BLANK]), computed arithmetically so a non-Latin-digit locale cannot break them. */
fun clockDigits(hour: Int, minute: Int, h24: Boolean = true): List<Int> {
    val h = if (h24) hour else twelveHour(hour)
    return listOf(if (!h24 && h < 10) BLANK else h / 10, h % 10, minute / 10, minute % 10)
}

private fun twelveHour(hour: Int): Int = (hour % 12).let { if (it == 0) 12 else it }

fun amPm(hour: Int): String = if (hour in 0..11) "am" else "pm"

/** `21:08` in 24-hour mode, `9:08pm` in 12-hour mode (no leading zero). */
fun formatClockTime(hour: Int, minute: Int, h24: Boolean): String =
    if (h24) String.format(Locale.ROOT, "%02d:%02d", hour, minute)
    else String.format(Locale.ROOT, "%d:%02d%s", twelveHour(hour), minute, amPm(hour))

/** What `clock [value]` does: the format to save (null = unchanged) and the line to print. */
data class ClockReply(val set: ClockFormat?, val text: String)

fun clockFormatReply(sub: String?, arg: String?, current: ClockFormat): ClockReply {
    if (sub == null) return ClockReply(null, "format  ${current.label()}")
    if (arg != null) return ClockReply(null, USAGE) // one word only
    return parseClockFormat(sub).fold({ ClockReply(it, "clock: ${it.label()}") }, { ClockReply(null, it.message ?: USAGE) })
}

private fun ClockFormat.label() = name.lowercase().removePrefix("h")
