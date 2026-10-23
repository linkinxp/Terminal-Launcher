package dev.linkinxp.termlauncher.calendar

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgendaTest {
    private val utc = ZoneOffset.UTC
    // Friday 2026-10-23 09:00 UTC
    private val now = Instant.parse("2026-10-23T09:00:00Z").toEpochMilli()

    private fun at(s: String) = Instant.parse(s).toEpochMilli()
    private fun timed(title: String, from: String, to: String) = AgendaEvent(title, at(from), at(to), false)
    private fun allDay(title: String, from: String, to: String) = AgendaEvent(title, at(from), at(to), true)
    private fun fmt(vararg e: AgendaEvent, zone: ZoneId = utc, maxLines: Int = 6, width: Int = 34) =
        formatAgenda(e.toList(), now, zone, maxLines, width)

    @Test fun sortsByStartWithDayLabels() {
        val lines = fmt(
            timed("Review", "2026-10-27T14:30:00Z", "2026-10-27T15:00:00Z"),
            timed("Gym", "2026-10-24T08:00:00Z", "2026-10-24T09:00:00Z"),
            timed("Cloud Infra Standup", "2026-10-23T10:05:00Z", "2026-10-23T11:00:00Z"),
        )
        assertEquals(
            listOf("today 10:05  Cloud Infra Standup", "tmrw 08:00  Gym", "tue 14:30  Review"),
            lines,
        )
    }

    @Test fun endedEventsAreDropped() {
        val lines = fmt(timed("Early", "2026-10-23T07:00:00Z", "2026-10-23T08:00:00Z"), timed("Later", "2026-10-23T10:00:00Z", "2026-10-23T11:00:00Z"))
        assertEquals(listOf("today 10:00  Later"), lines)
    }

    @Test fun ongoingEventsStartWithNow() {
        assertEquals(listOf("now  Standup"), fmt(timed("Standup", "2026-10-23T08:30:00Z", "2026-10-23T09:30:00Z")))
    }

    @Test fun anEventSpanningMidnightThatHasStartedIsNow() {
        assertEquals(listOf("now  Night shift"), fmt(timed("Night shift", "2026-10-22T23:00:00Z", "2026-10-23T12:00:00Z")))
    }

    @Test fun allDayEventsUseUtcDatesAgainstTheLocalDay() {
        val lines = fmt(
            allDay("Holiday", "2026-10-24T00:00:00Z", "2026-10-25T00:00:00Z"),
            allDay("Release", "2026-10-23T00:00:00Z", "2026-10-24T00:00:00Z"),
            allDay("Past", "2026-10-21T00:00:00Z", "2026-10-22T00:00:00Z"),
        )
        assertEquals(listOf("today all day  Release", "tmrw all day  Holiday"), lines)
    }

    @Test fun longTitlesAreCutWithAnEllipsisToExactlyWidth() {
        val line = fmt(timed("This is a really long meeting title", "2026-10-23T10:05:00Z", "2026-10-23T11:00:00Z")).single()
        assertEquals(34, line.length)
        assertTrue(line.endsWith("…"))
        assertTrue(line.startsWith("today 10:05  This is a really"))
    }

    @Test fun blankTitleBecomesNoTitle() {
        assertEquals(listOf("today 10:05  (no title)"), fmt(timed("  ", "2026-10-23T10:05:00Z", "2026-10-23T11:00:00Z")))
    }

    @Test fun noEventsSaysSo() {
        assertEquals(listOf("no events"), fmt())
        assertEquals(listOf("no events"), fmt(timed("Gone", "2026-10-23T01:00:00Z", "2026-10-23T02:00:00Z")))
    }

    @Test fun maxLinesKeepsTheFirstOnes() {
        val lines = fmt(
            timed("A", "2026-10-23T10:00:00Z", "2026-10-23T11:00:00Z"),
            timed("B", "2026-10-23T12:00:00Z", "2026-10-23T13:00:00Z"),
            timed("C", "2026-10-23T14:00:00Z", "2026-10-23T15:00:00Z"),
            maxLines = 2,
        )
        assertEquals(listOf("today 10:00  A", "today 12:00  B"), lines)
    }

    @Test fun localTimeFollowsTheZoneAcrossADstChange() {
        // Zagreb leaves summer time on 2026-10-25 03:00 local: 09:00 UTC that day is 10:00 CET.
        val lines = fmt(timed("X", "2026-10-25T09:00:00Z", "2026-10-25T10:00:00Z"), zone = ZoneId.of("Europe/Zagreb"))
        assertEquals(listOf("sun 10:00  X"), lines)
    }

    @Test fun emojiAndRightToLeftTitlesNeverExceedTheWidth() {
        val emoji = "🎉".repeat(18)
        val rtl = "שלום עולם ".repeat(6)
        for (title in listOf(emoji, rtl)) {
            val line = fmt(timed(title, "2026-10-23T10:05:00Z", "2026-10-23T11:00:00Z")).single()
            assertTrue("length ${line.length}", line.length <= 34)
            assertTrue("no dangling surrogate", !Character.isHighSurrogate(line.last()))
        }
    }
}
