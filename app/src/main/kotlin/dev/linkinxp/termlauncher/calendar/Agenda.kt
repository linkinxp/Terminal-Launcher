package dev.linkinxp.termlauncher.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

data class AgendaEvent(val title: String, val startMs: Long, val endMs: Long, val allDay: Boolean)

private const val NO_EVENTS = "no events"
private const val NO_TITLE = "(no title)"

/**
 * One line per upcoming event, e.g. `today 10:05  Standup`, `now  Standup` (ongoing), `tmrw all day  Holiday`,
 * `tue 14:30  Review`. Ended events are dropped, the rest sorted by start and cut to [maxLines] lines of at
 * most [width] characters (long titles end in `…`). All-day events follow the CalendarContract convention:
 * begin/end are UTC midnights (end exclusive) compared as dates with the local today.
 */
fun formatAgenda(events: List<AgendaEvent>, nowMs: Long, zone: ZoneId, maxLines: Int = 6, width: Int = 34): List<String> {
    val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    val startOfToday = today.atStartOfDay(zone).toInstant().toEpochMilli()

    class Entry(val sortKey: Long, val prefix: String, val title: String)

    val entries = events.mapNotNull { e ->
        val title = e.title.trim().ifEmpty { NO_TITLE }
        if (e.allDay) {
            val first = Instant.ofEpochMilli(e.startMs).atZone(ZoneOffset.UTC).toLocalDate()
            val endExclusive = Instant.ofEpochMilli(e.endMs).atZone(ZoneOffset.UTC).toLocalDate()
            if (endExclusive <= today) return@mapNotNull null
            val shown = if (first < today) today else first
            Entry(maxOf(first.atStartOfDay(zone).toInstant().toEpochMilli(), startOfToday), "${dayLabel(shown, today)} all day", title)
        } else {
            if (e.endMs <= nowMs) return@mapNotNull null
            if (e.startMs <= nowMs) {
                Entry(e.startMs, "now", title)
            } else {
                val start = Instant.ofEpochMilli(e.startMs).atZone(zone)
                val time = String.format(Locale.ROOT, "%02d:%02d", start.hour, start.minute)
                Entry(e.startMs, "${dayLabel(start.toLocalDate(), today)} $time", title)
            }
        }
    }.sortedBy { it.sortKey }
    if (entries.isEmpty()) return listOf(NO_EVENTS)
    return entries.take(maxLines).map { fit(it.prefix, it.title, width) }
}

private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "today"
    today.plusDays(1) -> "tmrw"
    else -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).lowercase()
}

/** `prefix  title` cut to [width] UTF-16 units, ending in `…` when the title was shortened (never mid-surrogate). */
private fun fit(prefix: String, title: String, width: Int): String {
    val line = "$prefix  $title"
    if (line.length <= width) return line
    val room = width - prefix.length - 2
    if (room < 2) return line.take(width).dropDanglingSurrogate()
    return "$prefix  " + title.take(room - 1).dropDanglingSurrogate() + "…"
}

private fun String.dropDanglingSurrogate(): String =
    if (isNotEmpty() && Character.isHighSurrogate(last())) dropLast(1) else this

/**
 * The `[from, to]` range to query the calendar for: from the start of the previous local day (all-day events end
 * at a UTC midnight, so in western zones today's one ends "in the past" by the evening and a window starting
 * at [nowMs] would miss it; [formatAgenda] drops what has really ended) to [days] days after [nowMs].
 */
fun calendarQueryWindow(nowMs: Long, zone: ZoneId, days: Long): Pair<Long, Long> {
    val yesterday = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate().minusDays(1)
    return yesterday.atStartOfDay(zone).toInstant().toEpochMilli() to nowMs + days * 24 * 3_600_000L
}
