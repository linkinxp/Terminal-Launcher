package dev.linkinxp.termlauncher.clock

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockFormatTest {
    @Test fun autoFollowsThePhoneAndTheOthersDoNot() {
        assertEquals(true, ClockFormat.AUTO.is24(systemIs24 = true))
        assertEquals(false, ClockFormat.AUTO.is24(systemIs24 = false))
        assertEquals(true, ClockFormat.H24.is24(systemIs24 = false))
        assertEquals(false, ClockFormat.H12.is24(systemIs24 = true))
    }

    @Test fun parsingAcceptsTheUsualSpellingsCaseInsensitively() {
        assertEquals(ClockFormat.AUTO, parseClockFormat("auto").getOrThrow())
        assertEquals(ClockFormat.H24, parseClockFormat("24").getOrThrow())
        assertEquals(ClockFormat.H24, parseClockFormat("24H").getOrThrow())
        assertEquals(ClockFormat.H12, parseClockFormat("12").getOrThrow())
        assertEquals(ClockFormat.H12, parseClockFormat(" 12h ").getOrThrow())
    }

    @Test fun badOrMissingValuesGiveUsage() {
        assertEquals("usage: clock auto|24|12", parseClockFormat("13").exceptionOrNull()?.message)
        assertEquals("usage: clock auto|24|12", parseClockFormat("").exceptionOrNull()?.message)
        assertEquals("usage: clock auto|24|12", parseClockFormat(null).exceptionOrNull()?.message)
    }

    @Test fun clockDigitsIn24HourMode() {
        assertEquals(listOf(2, 1, 0, 5), clockDigits(21, 5, h24 = true))
        assertEquals(listOf(0, 0, 0, 0), clockDigits(0, 0, h24 = true))
    }

    @Test fun clockDigitsIn12HourModeBlankTheLeadingZeroAndWrapMidnightAndNoon() {
        assertEquals(listOf(BLANK, 9, 2, 9), clockDigits(21, 29, h24 = false))
        assertEquals(listOf(1, 0, 4, 5), clockDigits(10, 45, h24 = false))
        assertEquals(listOf(1, 2, 0, 5), clockDigits(0, 5, h24 = false))   // 12:05 am
        assertEquals(listOf(1, 2, 3, 0), clockDigits(12, 30, h24 = false)) // 12:30 pm
        assertEquals(listOf(BLANK, 1, 5, 9), clockDigits(13, 59, h24 = false))
    }

    @Test fun amPmMarker() {
        assertEquals("am", amPm(0)); assertEquals("am", amPm(11))
        assertEquals("pm", amPm(12)); assertEquals("pm", amPm(23))
    }

    @Test fun formatClockTime() {
        assertEquals("21:08", formatClockTime(21, 8, h24 = true))
        assertEquals("09:05", formatClockTime(9, 5, h24 = true))
        assertEquals("9:08pm", formatClockTime(21, 8, h24 = false))
        assertEquals("12:05am", formatClockTime(0, 5, h24 = false))
        assertEquals("12:00pm", formatClockTime(12, 0, h24 = false))
        assertEquals("9:05am", formatClockTime(9, 5, h24 = false))
    }

    @Test fun storedNamesDecodeAndUnknownOnesFallBackToAuto() {
        assertEquals(ClockFormat.H12, ClockFormat.fromName("H12"))
        assertEquals(ClockFormat.AUTO, ClockFormat.fromName("nonsense"))
        assertEquals(ClockFormat.AUTO, ClockFormat.fromName(null))
    }

    @Test fun theClockCommandShowsSetsAndRejects() {
        assertEquals(ClockReply(null, "format  auto"), clockFormatReply(null, null, ClockFormat.AUTO))
        assertEquals(ClockReply(null, "format  12"), clockFormatReply(null, null, ClockFormat.H12))
        assertEquals(ClockReply(ClockFormat.H12, "clock: 12"), clockFormatReply("12h", null, ClockFormat.AUTO))
        assertEquals(ClockReply(ClockFormat.H24, "clock: 24"), clockFormatReply("24", null, ClockFormat.AUTO))
        assertEquals(ClockReply(null, "usage: clock auto|24|12"), clockFormatReply("13", null, ClockFormat.H12))
        assertEquals(ClockReply(null, "usage: clock auto|24|12"), clockFormatReply("12", "pm", ClockFormat.H12))
    }
}
