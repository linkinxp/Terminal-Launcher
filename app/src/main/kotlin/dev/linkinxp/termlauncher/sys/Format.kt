package dev.linkinxp.termlauncher.sys

import kotlin.math.roundToInt

/** `[######----]`: [fraction] clamped to 0..1 (NaN counts as 0) over [width] cells. */
fun asciiBar(fraction: Float, width: Int = 10): String {
    val f = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
    val filled = (f * width).roundToInt()
    return "[" + "#".repeat(filled) + "-".repeat(width - filled) + "]"
}

/** `3d 4h`, `4h 12m` or `12m`; negative durations read `0m`. */
fun formatUptime(ms: Long): String {
    val minutes = maxOf(ms, 0L) / 60_000
    val days = minutes / (24 * 60)
    val hours = minutes % (24 * 60) / 60
    val mins = minutes % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins}m"
    }
}
