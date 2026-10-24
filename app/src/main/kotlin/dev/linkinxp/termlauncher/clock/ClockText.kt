package dev.linkinxp.termlauncher.clock

import java.util.Locale

// Three-line digits, 4 columns wide (a 3-column glyph and one trailing space).
private val FIGLET = mapOf(
    0 to listOf(" _  ", "| | ", "|_| "),
    1 to listOf("    ", "  | ", "  | "),
    2 to listOf(" _  ", " _| ", "|_  "),
    3 to listOf(" _  ", " _| ", " _| "),
    4 to listOf("    ", "|_| ", "  | "),
    5 to listOf(" _  ", "|_  ", " _| "),
    6 to listOf(" _  ", "|_  ", "|_| "),
    7 to listOf(" _  ", "  | ", "  | "),
    8 to listOf(" _  ", "|_| ", "|_| "),
    9 to listOf(" _  ", "|_| ", " _| "),
)
private val FIGLET_BLANK = List(3) { "    " }

/** Big ASCII digits: exactly three lines; digits are separated by one space; [BLANK] or anything else is empty. */
fun figletLines(digits: List<Int>): List<String> =
    (0 until 3).map { row -> digits.joinToString(" ") { (FIGLET[it] ?: FIGLET_BLANK)[row] } }

/** Four columns (hour tens, hour units, minute tens, minute units) of 3, 4, 3, 4 bits, most significant first. */
fun binaryColumns(hour: Int, minute: Int): List<List<Boolean>> = listOf(
    bits(hour / 10, 3), bits(hour % 10, 4), bits(minute / 10, 3), bits(minute % 10, 4),
)

private fun bits(value: Int, count: Int): List<Boolean> = (count - 1 downTo 0).map { (value shr it) and 1 == 1 }

fun hexTime(hour: Int, minute: Int): String = String.format(Locale.ROOT, "0x%02X:%02X", hour, minute)

/** Unix seconds, at least ten digits wide; negative values show as zero. */
fun epochText(epochSeconds: Long): String = maxOf(epochSeconds, 0L).toString().padStart(10, '0')

// Segments: a=1 b=2 c=4 d=8 e=16 f=32 g=64.
private val SEGMENTS = intArrayOf(63, 6, 91, 79, 102, 109, 125, 7, 127, 111)

/** Which of the seven segments of a display digit are lit ([BLANK] and out-of-range digits: none). */
fun segmentMask(digit: Int): Int = SEGMENTS.getOrElse(digit) { 0 }
