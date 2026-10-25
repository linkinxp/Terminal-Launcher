package dev.linkinxp.termlauncher.clock

import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

/** `mon 26 oct 2026  wk 44  day 299`, with the weekday and month names in the phone's language. */
fun dateLine(date: LocalDate, locale: Locale): String =
    "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).lowercase(locale)} ${date.dayOfMonth} " +
        "${date.month.getDisplayName(TextStyle.SHORT, locale).lowercase(locale)} ${date.year}  " +
        "wk ${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}  day ${date.dayOfYear}"
