package dev.linkinxp.termlauncher.fx

import kotlin.random.Random

/** Top of the soft scan band at [timeMs]: it sweeps from the top to the bottom of [heightPx] every [periodMs] and starts over. */
fun scanBarY(timeMs: Long, heightPx: Float, periodMs: Long = 7_000): Float =
    (maxOf(timeMs, 0L) % periodMs).toFloat() / periodMs * heightPx

/** One horizontal glitch bar: where it sits, how tall it is, how far it is shifted and which of the three colors it takes. */
data class GlitchBar(val y: Float, val height: Float, val dx: Float, val colorIndex: Int)

/** [count] random bars inside a screen of [height] (none for an empty screen); the same seed always gives the same bars. */
fun glitchBars(seed: Int, count: Int, height: Float): List<GlitchBar> {
    if (count <= 0 || height <= 0f) return emptyList()
    val r = Random(seed)
    return List(count) {
        val h = height * (0.006f + r.nextFloat() * 0.02f)
        GlitchBar(y = r.nextFloat() * (height - h), height = h, dx = (r.nextFloat() - 0.5f) * 90f, colorIndex = r.nextInt(3))
    }
}

/** How long to wait before the next screen-wide glitch: between 30 and 60 seconds. */
fun nextGlitchDelayMs(seed: Int): Long = 30_000L + Random(seed).nextLong(30_001)

data class Flash(val durationMs: Int, val alpha: Float)

/** A short burst of two to four dark flashes (the CRT flicker). */
fun flickerFlashes(seed: Int): List<Flash> {
    val r = Random(seed)
    return List(2 + r.nextInt(3)) { Flash(40 + r.nextInt(31), 0.06f + r.nextFloat() * 0.04f) }
}
