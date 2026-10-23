package dev.linkinxp.termlauncher.ui

import kotlin.random.Random

/**
 * Digits to show while the clock "glitches": digit `i` shows its real value once
 * `progress >= (i + 1) / 5` (so they lock in left to right), the rest are seeded-random 0-9.
 * Negative or NaN progress counts as 0; `progress >= 1` shows the real digits.
 */
fun glitchFrame(digits: List<Int>, progress: Float, seed: Int): List<Int> {
    val p = if (progress.isNaN()) 0f else progress
    return digits.mapIndexed { i, d -> if (p >= (i + 1) / 5f) d else Random(seed + i).nextInt(10) }
}
