package dev.linkinxp.termlauncher.matrix

/** Half-width katakana (as in the film) mixed with digits. */
val RAIN_GLYPHS: String = ('ｱ'..'ﾝ').joinToString("") + "0123456789"

/** Cells in a falling streak: level 0 is the bright head, higher levels fade out. */
const val RAIN_TRAIL = 14

class RainCell(val col: Int, val row: Int, val glyph: Int, val level: Int) {
    override fun equals(other: Any?) = other is RainCell && col == other.col && row == other.row && glyph == other.glyph && level == other.level
    override fun hashCode() = ((col * 31 + row) * 31 + glyph) * 31 + level
}

private fun mix(a: Int, b: Int, c: Int): Int {
    var h = a * -0x61c88647 + b * 0x27d4eb2d + c * 0x165667b1
    h = h xor (h ushr 15); h *= -0x7a143595; h = h xor (h ushr 13)
    return h and Int.MAX_VALUE
}

/**
 * The lit cells of one frame of falling code on a [cols] x [rows] grid. Every column is an endless streak with its
 * own speed and gap; the same [tick] and [seed] always give the same frame, and nothing for an empty grid.
 */
fun rainCells(cols: Int, rows: Int, tick: Long, seed: Int): List<RainCell> {
    if (cols <= 0 || rows <= 0) return emptyList()
    val out = ArrayList<RainCell>(cols * 6)
    for (c in 0 until cols) {
        val rate = 1 + mix(c, seed, 1) % 3 // rows per two ticks
        val cycle = rows + RAIN_TRAIL + mix(c, seed, 2) % (rows / 2 + 1)
        val offset = mix(c, seed, 3) % cycle
        val head = Math.floorMod(tick * rate / 2 + offset, cycle.toLong()).toInt()
        val flip = tick / (3 + c % 4)
        for (level in 0 until RAIN_TRAIL) {
            val row = head - level
            if (row !in 0 until rows) continue
            out += RainCell(c, row, mix(c, row, flip.toInt()) % RAIN_GLYPHS.length, level)
        }
    }
    return out
}
