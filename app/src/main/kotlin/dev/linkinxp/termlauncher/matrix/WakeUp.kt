package dev.linkinxp.termlauncher.matrix

import dev.linkinxp.termlauncher.wall.WallMode

val WAKE_LINES = listOf("Wake up, Neo...", "The Matrix has you...", "Follow the white rabbit.", "Knock, knock, Neo.")
const val WAKE_CHAR_MS = 70
const val WAKE_PAUSE_MS = 1_000L
private const val WAKE_HOLD_MS = 1_500L

/** Total time of the sequence, including the pause on the finished screen. */
fun wakeDurationMs(): Long = WAKE_LINES.sumOf { it.length * WAKE_CHAR_MS.toLong() } + (WAKE_LINES.size - 1) * WAKE_PAUSE_MS + WAKE_HOLD_MS

/** The lines visible [elapsedMs] into the sequence: each line is typed out, then a pause, then the next. */
fun wakeText(elapsedMs: Long): List<String> {
    val shown = ArrayList<String>()
    var start = 0L
    for (line in WAKE_LINES) {
        if (elapsedMs < start && shown.isNotEmpty()) break
        val chars = ((elapsedMs - start) / WAKE_CHAR_MS).coerceIn(0, line.length.toLong()).toInt()
        shown += line.take(chars)
        start += line.length * WAKE_CHAR_MS + WAKE_PAUSE_MS
    }
    return shown
}

class PillPlan(val theme: String?, val wallMode: WallMode?, val message: String)

/** `pill red|blue`: what to switch to and what to say; anything else only prints the choice. */
fun pillPlan(arg: String?): PillPlan = when (arg?.trim()?.lowercase()) {
    "red" -> PillPlan("matrix", WallMode.RAIN, "you take the red pill. welcome to the real world.")
    "blue" -> PillPlan("classy-black", WallMode.SOLID, "you take the blue pill. the story ends.")
    else -> PillPlan(null, null, "usage: pill red|blue")
}

const val SPOON = "there is no spoon."
const val RABBIT = "(\\_/)\n(o.o)  knock, knock, neo."
